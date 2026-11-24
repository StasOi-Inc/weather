package ru.rufp.weather.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.rufp.weather.R
import ru.rufp.weather.presentation.MainViewModel
import ru.rufp.weather.update.UpdateInstaller
import ru.rufp.weather.update.UpdateInfo

@Composable
fun UpdateDialog(
    update: UpdateInfo,
    viewModel: MainViewModel,
    installer: UpdateInstaller,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var progress by remember { mutableStateOf<Int?>(null) }
    var downloaded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    fun startDownload() {
        if (progress != null) return
        viewModel.downloadUpdate(update) { p ->
            if (p < 0) {
                error = true
                progress = null
            } else {
                if (p >= 100) downloaded = true
                progress = if (p >= 100) null else p
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (progress == null) onDismiss() },
        confirmButton = {
            if (downloaded) {
                TextButton(onClick = {
                    if (installer.canInstall()) {
                        installer.installIntent()?.let { intent ->
                            context.startActivity(intent)
                        }
                        onDismiss()
                    } else {
                        context.startActivity(installer.unknownSourcesSettingsIntent())
                    }
                }) { Text(stringResource(R.string.update_install)) }
            } else {
                TextButton(
                    onClick = { startDownload() },
                    enabled = progress == null && !error
                ) { Text(stringResource(R.string.update_download)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later)) }
        },
        title = { Text(stringResource(R.string.update_available, update.versionName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.update_size, "%.1f".format(update.sizeBytes / 1_000_000.0)),
                    style = MaterialTheme.typography.bodyMedium
                )
                progress?.let { p ->
                    LinearProgressIndicator(
                        progress = { p / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                    )
                    Text(
                        stringResource(R.string.update_downloading_percent, p),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                if (downloaded) {
                    if (installer.canInstall()) {
                        Text(stringResource(R.string.update_ready))
                    } else {
                        Text(
                            stringResource(R.string.update_install_permission_hint),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (error) {
                    Text(
                        stringResource(R.string.update_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    )
}
