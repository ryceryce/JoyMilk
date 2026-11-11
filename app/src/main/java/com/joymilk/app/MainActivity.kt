package com.joymilk.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.joymilk.app.data.repository.AuthRepository
import com.joymilk.app.ui.navigation.JoyMilkNavGraph
import com.joymilk.app.ui.theme.JoyMilkTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JoyMilkTheme {
                val navController = rememberNavController()
                JoyMilkNavGraph(
                    navController = navController,
                    authRepository = authRepository
                )
            }
        }
    }
}
