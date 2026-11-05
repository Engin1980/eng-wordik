package cz.wordik.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cz.wordik.app.ui.StartupErrorScreen
import cz.wordik.app.ui.WordikApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as WordikApplication).container
        setContent {
            container.fold(
                onSuccess = { WordikApp(it) },
                onFailure = { StartupErrorScreen(it) },
            )
        }
    }
}
