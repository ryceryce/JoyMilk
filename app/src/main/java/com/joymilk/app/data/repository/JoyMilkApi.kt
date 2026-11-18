package com.joymilk.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.joymilk.app.BuildConfig
import com.joymilk.app.util.NetworkMonitor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class ApiFailure(val status: Int, message: String) : IOException(message)

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class JoyMilkApi @Inject constructor(
    private val auth: FirebaseAuth,
    network: NetworkMonitor
) {
    private val http = OkHttpClient.Builder().callTimeout(25, TimeUnit.SECONDS).build()
    private val sockets = http.newBuilder().callTimeout(0, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS).build()
    private val changed = MutableSharedFlow<String>(extraBufferCapacity = 16)
    private val uid = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()
    private val refreshes = merge(changed, combine(uid, network.isOffline) { user, offline ->
        user.takeUnless { offline }
    }.distinctUntilChanged().flatMapLatest { onlineUid ->
        if (onlineUid == null) emptyFlow() else flow {
            emit("*")
            if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_PUBLISHABLE_KEY.isBlank()) return@flow
            emitAll(realtime().retryWhen { _, attempt ->
                delay((1L shl attempt.coerceAtMost(5).toInt()) * 1_000)
                true
            })
        }
    })

    private val sharedRefreshes = refreshes.shareIn(
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO),
        SharingStarted.WhileSubscribed(5_000)
    )

    fun changes(vararg tables: String) = sharedRefreshes.filter { it == "*" || it in tables }

    internal fun <T> observe(vararg tables: String, fetch: suspend () -> T): Flow<T> =
        changes(*tables).onStart { emit("*") }.conflate().map { fetch() }

    internal suspend fun objectAt(path: String): JSONObject = JSONObject(request(path))
    internal suspend fun arrayAt(path: String): JSONArray = JSONArray(request(path))

    internal suspend fun request(
        path: String, method: String = "GET", body: JSONObject? = null, idempotencyKey: String? = null
    ): String {
        if (BuildConfig.JOYMILK_API_URL.isBlank()) error("URL API JoyMilk belum dikonfigurasi.")
        val result = send(path, method, body, idempotencyKey, token())
        val (status, text) = if (result.first == 401 && auth.currentUser != null)
            send(path, method, body, idempotencyKey, token(force = true))
        else result
        if (status !in 200..299) {
            val message = runCatching { JSONObject(text).getString("message") }.getOrDefault("Permintaan gagal.")
            throw ApiFailure(status, message)
        }
        return text
    }

    internal fun invalidate(table: String) { changed.tryEmit(table) }

    private suspend fun token(force: Boolean = false): String =
        auth.currentUser?.getIdToken(force)?.await()?.token
            ?: throw ApiFailure(401, "Silakan masuk kembali.")

    private suspend fun send(
        path: String, method: String, body: JSONObject?, idempotencyKey: String?, token: String
    ): Pair<Int, String> {
        val url = BuildConfig.JOYMILK_API_URL.trimEnd('/') + "/api/v1/" + path
        val request = Request.Builder().url(url)
            .header("Authorization", "Bearer $token")
            .method(method, body?.toString()?.toRequestBody("application/json".toMediaType()))
            .apply { idempotencyKey?.let { header("Idempotency-Key", it) } }
            .build()
        return suspendCancellableCoroutine { continuation ->
            val call = http.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { continuation.resumeWithException(e) }
                override fun onResponse(call: Call, response: Response) {
                    val result = runCatching { response.use { it.code to it.body?.string().orEmpty() } }
                    continuation.resumeWith(result)
                }
            })
        }
    }

    private fun realtime(): Flow<String> = callbackFlow {
        val jwt = token()
        val url = (BuildConfig.SUPABASE_URL.trimEnd('/') + "/realtime/v1/websocket")
            .toHttpUrl().newBuilder()
            .addQueryParameter("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            .addQueryParameter("vsn", "1.0.0").build()
        val topic = "realtime:joymilk:changes"
        fun message(topic: String, event: String, ref: String, payload: JSONObject = JSONObject()) =
            JSONObject().put("topic", topic).put("event", event).put("ref", ref).put("payload", payload).toString()
        val socket = sockets.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val config = JSONObject().put("private", true)
                    .put("broadcast", JSONObject().put("ack", false).put("self", false))
                    .put("postgres_changes", JSONArray())
                webSocket.send(message(topic, "phx_join", "1", JSONObject().put("config", config).put("access_token", jwt)))
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                val data = runCatching { JSONObject(text) }.getOrNull() ?: return
                val payload = data.optJSONObject("payload")
                when (data.optString("event")) {
                    "phx_reply" -> if (data.optString("ref") == "1") {
                        if (payload?.optString("status") == "ok") trySend("*")
                        else close(IOException("Realtime ditolak."))
                    }
                    "broadcast" -> if (data.optString("topic") == topic) {
                        payload?.optString("event")?.let { trySend(it) }
                    }
                    "phx_error", "phx_close" -> close(IOException("Realtime terputus."))
                }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                response?.close()
                close(t)
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                close(IOException("Realtime terputus."))
            }
        })
        val heartbeat = launch {
            var ref = 2
            while (true) {
                delay(25_000)
                socket.send(message("phoenix", "heartbeat", (ref++).toString()))
                if (ref % 8 == 0) {
                    val accessToken = token()
                    socket.send(message(topic, "access_token", (ref++).toString(),
                        JSONObject().put("access_token", accessToken)))
                }
            }
        }
        awaitClose { heartbeat.cancel(); socket.cancel() }
    }
}
