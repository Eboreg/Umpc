package us.huseli.umpc.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.VolumeDown
import androidx.compose.material.icons.sharp.VolumeMute
import androidx.compose.material.icons.sharp.VolumeOff
import androidx.compose.material.icons.sharp.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.umpc.R

@Composable
fun VolumeSlider(
    modifier: Modifier = Modifier,
    volume: Float,
    backgroundAlpha: Float = 0.5f,
    onVolumeChange: (Float) -> Unit,
) {
    var mutableVolume by rememberSaveable(volume) { mutableStateOf(volume) }

    Box(contentAlignment = Alignment.Center, modifier = modifier.height(IntrinsicSize.Min)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background.copy(alpha = backgroundAlpha),
            shape = MaterialTheme.shapes.extraLarge,
            content = {}
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(40.dp)) {
            IconButton(
                modifier = Modifier.width(IntrinsicSize.Min).height(24.dp),
                onClick = {
                    mutableVolume = 0f
                    onVolumeChange(mutableVolume)
                }
            ) {
                Icon(
                    if (mutableVolume == 0f) Icons.Sharp.VolumeOff
                    else if (mutableVolume < 25f) Icons.Sharp.VolumeMute
                    else if (mutableVolume < 75f) Icons.Sharp.VolumeDown
                    else Icons.Sharp.VolumeUp,
                    stringResource(R.string.mute_volume)
                )
            }
            Slider(
                modifier = Modifier.weight(1f),
                value = mutableVolume,
                valueRange = 0f..100f,
                onValueChange = { mutableVolume = it },
                onValueChangeFinished = { onVolumeChange(mutableVolume) },
            )
            Text(
                "${mutableVolume.toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(end = 5.dp)
            )
        }
    }
}
