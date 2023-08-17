package us.huseli.umpc.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import us.huseli.umpc.repository.SnackbarMessage
import us.huseli.umpc.viewmodels.MPDViewModel

@Composable
fun DebugScreen(modifier: Modifier = Modifier, viewModel: MPDViewModel = hiltViewModel()) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Row {
            OutlinedButton(
                onClick = { viewModel.addMessage(SnackbarMessage(message = "Message", actionLabel = "Action")) },
                content = { Text("Show message") },
            )
            OutlinedButton(
                onClick = { viewModel.addError(SnackbarMessage(message = "Error", actionLabel = "Action")) },
                content = { Text("Show error") },
            )
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "background", MaterialTheme.colorScheme.background)
            ColorSample(Modifier.weight(0.5f), "error", MaterialTheme.colorScheme.error)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "errorContainer", MaterialTheme.colorScheme.errorContainer)
            ColorSample(Modifier.weight(0.5f), "inverseOnSurface", MaterialTheme.colorScheme.inverseOnSurface)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "inversePrimary", MaterialTheme.colorScheme.inversePrimary)
            ColorSample(Modifier.weight(0.5f), "inverseSurface", MaterialTheme.colorScheme.inverseSurface)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "onBackground", MaterialTheme.colorScheme.onBackground)
            ColorSample(Modifier.weight(0.5f), "onError", MaterialTheme.colorScheme.onError)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "onErrorContainer", MaterialTheme.colorScheme.onErrorContainer)
            ColorSample(Modifier.weight(0.5f), "onPrimary", MaterialTheme.colorScheme.onPrimary)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "onPrimaryContainer", MaterialTheme.colorScheme.onPrimaryContainer)
            ColorSample(Modifier.weight(0.5f), "onSecondary", MaterialTheme.colorScheme.onSecondary)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "onSecondaryContainer", MaterialTheme.colorScheme.onSecondaryContainer)
            ColorSample(Modifier.weight(0.5f), "onSurface", MaterialTheme.colorScheme.onSurface)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "onSurfaceVariant", MaterialTheme.colorScheme.onSurfaceVariant)
            ColorSample(Modifier.weight(0.5f), "onTertiary", MaterialTheme.colorScheme.onTertiary)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "onTertiaryContainer", MaterialTheme.colorScheme.onTertiaryContainer)
            ColorSample(Modifier.weight(0.5f), "outline", MaterialTheme.colorScheme.outline)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "outlineVariant", MaterialTheme.colorScheme.outlineVariant)
            ColorSample(Modifier.weight(0.5f), "primary", MaterialTheme.colorScheme.primary)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "primaryContainer", MaterialTheme.colorScheme.primaryContainer)
            ColorSample(Modifier.weight(0.5f), "scrim", MaterialTheme.colorScheme.scrim)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "secondary", MaterialTheme.colorScheme.secondary)
            ColorSample(Modifier.weight(0.5f), "secondaryContainer", MaterialTheme.colorScheme.secondaryContainer)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "surface", MaterialTheme.colorScheme.surface)
            ColorSample(Modifier.weight(0.5f), "surfaceTint", MaterialTheme.colorScheme.surfaceTint)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "surfaceVariant", MaterialTheme.colorScheme.surfaceVariant)
            ColorSample(Modifier.weight(0.5f), "tertiary", MaterialTheme.colorScheme.tertiary)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "tertiaryContainer", MaterialTheme.colorScheme.tertiaryContainer)
        }
    }
}
