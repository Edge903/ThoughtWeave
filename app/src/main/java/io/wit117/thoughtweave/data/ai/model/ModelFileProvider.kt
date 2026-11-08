package io.wit117.thoughtweave.data.ai.model

import java.io.File

/** What the engine needs from model delivery: a verified file and a way to report failure. */
interface ModelFileProvider {
    fun verifiedModelFile(): File?

    /** Re-hashes the model: a mismatch means re-download, a match means the device is unsupported. */
    suspend fun onEngineInitFailed()
}
