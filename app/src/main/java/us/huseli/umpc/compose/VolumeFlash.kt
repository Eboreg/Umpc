package us.huseli.umpc.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.VolumeDown
import androidx.compose.material.icons.sharp.VolumeMute
import androidx.compose.material.icons.sharp.VolumeOff
import androidx.compose.material.icons.sharp.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun VolumeFlash(modifier: Modifier = Modifier, volume: Int, isVisible: Boolean, onHide: () -> Unit) {
    LaunchedEffect(isVisible, volume) {
        if (isVisible) {
            delay(3_000)
            onHide()
        }
    }

    if (isVisible) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 2.dp,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                    Icon(
                        if (volume == 0) Icons.Sharp.VolumeOff
                        else if (volume < 25) Icons.Sharp.VolumeMute
                        else if (volume < 75) Icons.Sharp.VolumeDown
                        else Icons.Sharp.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.width(40.dp),
                    )
                    LinearProgressIndicator(progress = volume.toFloat() / 100)
                    Text(
                        text = "${volume}%",
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(40.dp),
                    )
                }
            }
        }
    }
}
