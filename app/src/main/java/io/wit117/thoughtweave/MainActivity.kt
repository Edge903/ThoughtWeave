package io.wit117.thoughtweave

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.wit117.thoughtweave.core.ui.theme.ThoughtWeaveTheme
import io.wit117.thoughtweave.domain.repository.AiModelRepository
import io.wit117.thoughtweave.navigation.ThoughtWeaveApp
import javax.inject.Inject

@AndroidEntryPoint
/** An AppCompatActivity because per-app language switching on Android 12 and below needs one. */
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var aiModelRepository: AiModelRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The model's location must be re-checked at every launch; it is never cached.
        aiModelRepository.refresh()
        enableEdgeToEdge()
        setContent {
            ThoughtWeaveTheme {
                ThoughtWeaveApp()
            }
        }
    }
}
