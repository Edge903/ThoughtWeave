package io.wit117.thoughtweave

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.wit117.thoughtweave.core.ui.theme.ThoughtWeaveTheme
import io.wit117.thoughtweave.navigation.ThoughtWeaveApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThoughtWeaveTheme {
                ThoughtWeaveApp()
            }
        }
    }
}
