package io.wit117.thoughtweave.feature.aimodel

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.wit117.thoughtweave.R
import io.wit117.thoughtweave.data.ai.delivery.ImportedModelDelivery
import javax.inject.Inject

@HiltViewModel
class ModelImportViewModel @Inject constructor(
    private val delivery: ImportedModelDelivery,
) : ViewModel() {
    fun import(uri: Uri) = delivery.import(uri)
}

/** Debug builds only: imports the model through the system file picker. */
@Composable
fun ModelImportAction(viewModel: ModelImportViewModel = hiltViewModel()) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::import)
    }
    TextButton(onClick = { picker.launch(arrayOf("*/*")) }) {
        Text(stringResource(R.string.debug_import_model))
    }
}
