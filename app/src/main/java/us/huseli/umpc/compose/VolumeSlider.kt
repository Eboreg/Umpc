package us.huseli.umpc.compose

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.VolumeMute
import androidx.compose.material.icons.sharp.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    var volume by rememberSaveable(value) { mutableStateOf(value) }

    BoxWithConstraints(contentAlignment = Alignment.Center, modifier = modifier) {
        val sliderWidth by remember(maxWidth) { mutableStateOf(maxWidth - 80.dp) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.width(40.dp),
                imageVector = Icons.Sharp.VolumeMute,
                contentDescription = stringResource(R.string.volume_down),
            )
            Slider(
                modifier = Modifier.width(sliderWidth),
                value = volume,
                valueRange = 0f..100f,
                onValueChange = { volume = it },
                onValueChangeFinished = { onValueChange(volume) },
            )
            Icon(
                modifier = Modifier.width(40.dp),
                imageVector = Icons.Sharp.VolumeUp,
                contentDescription = stringResource(R.string.volume_up),
            )
        }
    }
}
