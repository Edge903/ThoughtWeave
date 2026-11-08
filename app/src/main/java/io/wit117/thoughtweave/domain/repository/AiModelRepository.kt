package io.wit117.thoughtweave.domain.repository

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import io.wit117.thoughtweave.domain.model.AiModelStatus
import kotlinx.coroutines.flow.StateFlow

interface AiModelRepository {
    val status: StateFlow<AiModelStatus>

    /** Re-checks delivery. Pack locations must not be cached, so this runs at every launch. */
    fun refresh()

    fun retry()

    /** Removes the installed model and obtains it again. */
    fun redownload()

    /** Shows Play's dialog for mobile data or app confirmation. @return false if none is shown. */
    fun showConfirmation(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean
}
