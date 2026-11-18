# 🥛 JoyMilk

Aplikasi Android internal untuk mencatat order, mengelola produk, dan merekap penjualan susu kedelai. Staf dapat menyiapkan order dari Android, Telegram, atau klien AI internal melalui MCP.

## Arsitektur

Firebase Auth menangani login email/password. Android mengakses data bisnis lewat API Vercel. Backend menulis ke Supabase Postgres dan mengirim sinyal perubahan lewat Supabase Realtime. Order menyimpan nama dan harga produk saat transaksi. Tingkat kemanisan tidak memengaruhi harga.

## Setup staging atau produksi

1. Siapkan proyek Firebase dengan email/password dan proyek Supabase. Buka konfigurasi integrasi Firebase Auth pihak ketiga di Supabase, isi Firebase Project ID, lalu nonaktifkan **Allow public access to channels** di Realtime Settings. Pakai proyek terpisah untuk staging dan produksi. [Integrasi Firebase Auth](https://supabase.com/docs/guides/auth/third-party/firebase-auth) · [Realtime Settings](https://supabase.com/docs/guides/realtime/settings)
2. Jalankan migrasi SQL di `supabase/migrations/` secara berurutan dengan role migrasi. Atur kata sandi untuk role `joymilk_runtime` melalui sesi database admin, misalnya dengan `\password joymilk_runtime` di psql. Ambil URL koneksi backend dari Supabase Connect dan gunakan role runtime. Untuk Vercel, gunakan transaction pooler jika sesuai dengan jaringan proyek. Backend menonaktifkan prepared statements untuk mode ini. [Koneksi Postgres Supabase](https://supabase.com/docs/guides/database/connecting-to-postgres)
3. Di Vercel, pilih root directory `backend`, framework Next.js, dan Node.js 22. Isi `DATABASE_URL` dan `FIREBASE_SERVICE_ACCOUNT_JSON` memakai nilai lingkungan tersebut. Lihat [contoh environment](backend/.env.example). Simpan kredensial penulis database dan Firebase Admin di backend.
4. Masuk ke `backend` dan jalankan `npm ci`. Dengan kredensial Firebase Admin dan koneksi role admin database di lingkungan lokal, jalankan `npm run admin -- profile <firebase-uid> admin` untuk admin pertama. Ulangi untuk staf yang sudah punya akun Firebase Auth dengan peran `staff`. Perintah ini juga menyiapkan claim Realtime. Login kembali di Android bila sesi lama belum memiliki claim baru.
5. Masukkan kategori dan produk awal lewat Android setelah profil admin aktif. Supabase dimulai dari database kosong. Laporan Supabase mencakup transaksi sejak cutover. Order dan riwayat harga Firestore tetap di arsip. Lihat [prosedur cutover](docs/IMPLEMENTATION_PLAN.md).

## Menjalankan Android

- Siapkan Android Studio, JDK 17+, dan Android SDK 35. Letakkan konfigurasi Firebase untuk `com.joymilk.app` di `app/google-services.json`.
- Isi `JOYMILK_API_URL`, `SUPABASE_URL`, dan `SUPABASE_PUBLISHABLE_KEY` melalui environment atau Gradle properties. Pakai URL dasar HTTPS untuk API dan Supabase, tanpa path API tambahan.
- Jalankan dari Android Studio atau build dengan `gradle :app:assembleDebug` memakai Gradle 8.11.1. Skrip `gradlew.bat` membutuhkan JAR wrapper.

## Telegram dan MCP

- Di lingkungan Vercel, tambahkan `TELEGRAM_BOT_TOKEN`, `TELEGRAM_WEBHOOK_SECRET`, `OPENAI_API_KEY`, dan `OPENAI_MODEL`. Gunakan model yang mendukung Structured Outputs. Chat staf dikirim ke layanan interpretasi untuk menyiapkan draf. Jangan masukkan data pelanggan yang tidak diperlukan.
- Daftarkan webhook Telegram ke `https://<backend>/api/telegram/webhook` dengan nilai `secret_token` yang sama, `allowed_updates` berisi `message` dan `callback_query`, serta `max_connections=1` untuk menjaga urutan percakapan.
- Admin menautkan akun dengan `npm run admin -- link <firebase-uid> <telegram-user-id>` dan mencabut tautan dengan `npm run admin -- unlink <firebase-uid>`.
- Untuk klien AI, jalankan `npm run admin -- token <firebase-uid> confirm` untuk menerbitkan token yang dapat menyimpan order, atau hilangkan `confirm` untuk akses pencarian dan draf saja. Simpan token dari keluaran sekali tampil dan catat ID pencabutan. Cabut dengan `npm run admin -- revoke <firebase-uid> <revocation-id>`.
- Arahkan klien MCP yang mendukung header kustom ke `https://<backend>/api/mcp` dengan Streamable HTTP dan header `Authorization: Bearer <token>`. Klien meminta persetujuan pengguna sebelum memakai tool konfirmasi. [Telegram Bot API](https://core.telegram.org/bots/api#setwebhook) · [MCP Streamable HTTP](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports)

## Verifikasi lokal

Di `backend`, jalankan `npm run check` dan `npm run build`. Untuk pengujian transaksi, siapkan database PostgreSQL uji terpisah dan terapkan migrasi skema pertama. Set `JOYMILK_TEST_OWNER_URL` ke koneksi role owner database uji dan `DATABASE_URL` ke koneksi runtime database uji, lalu jalankan `npm test`. Tes menulis fixture ke database uji.

## Dokumentasi

| Dokumen | Isi |
|---|---|
| [PRD](docs/PRD.md) | Kebutuhan pengguna dan aturan produk |
| [TRD](docs/TRD.md) | Arsitektur, API, autentikasi, dan MCP |
| [Skema database](docs/DATABASE_SCHEMA.md) | Tabel Postgres, akses, dan transaksi |
| [Workflow](docs/WORKFLOW.md) | Alur Android, Telegram, dan klien AI |
| [Aturan UI/UX](docs/UI_UX_RULES.md) | Komponen, bahasa, dan status UI |
| [Mockup](docs/MOCKUP.md) | Mockup layar Android dan wireframe |
| [Prototipe interaktif](docs/mockup.html) | Pratinjau layar Android |
| [Rencana implementasi](docs/IMPLEMENTATION_PLAN.md) | Tahapan, pengujian, dan cutover |

Instruksi pengembangan tersedia di [AGENTS.md](AGENTS.md) dan [CLAUDE.md](CLAUDE.md). JoyMilk untuk penggunaan internal.
