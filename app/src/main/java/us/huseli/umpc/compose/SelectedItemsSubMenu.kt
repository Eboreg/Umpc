package us.huseli.umpc.compose

import androidx.annotation.PluralsRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import us.huseli.umpc.R
import us.huseli.umpc.compose.utils.SmallOutlinedButton

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectedItemsSubMenu(
    modifier: Modifier = Modifier,
    selectedItemCount: Int,
    @PluralsRes pluralsResId: Int,
    padding: PaddingValues = PaddingValues(start = 10.dp, end = 10.dp, bottom = 5.dp),
    onEnqueueClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onDeselectAllClick: () -> Unit,
    onRemoveClick: (() -> Unit)? = null,
) {
    Surface(tonalElevation = 2.dp, modifier = modifier.fillMaxWidth().zIndex(1f)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = pluralStringResource(pluralsResId, selectedItemCount, selectedItemCount),
                style = MaterialTheme.typography.bodySmall,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SmallOutlinedButton(
                    modifier = Modifier.padding(bottom = 5.dp),
                    onClick = onEnqueueClick,
                    text = stringResource(R.string.enqueue),
                )
                SmallOutlinedButton(
                    modifier = Modifier.padding(bottom = 5.dp),
                    onClick = onAddToPlaylistClick,
                    text = stringResource(R.string.add_to_playlist),
                )
                SmallOutlinedButton(
                    modifier = Modifier.padding(bottom = 5.dp),
                    onClick = onDeselectAllClick,
                    text = stringResource(R.string.deselect_all),
                )
                onRemoveClick?.let {
                    SmallOutlinedButton(
                        modifier = Modifier.padding(bottom = 5.dp),
                        onClick = it,
                        text = stringResource(R.string.remove),
                    )
                }
            }
        }}
}
