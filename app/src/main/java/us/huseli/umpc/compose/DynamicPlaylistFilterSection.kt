package us.huseli.umpc.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import us.huseli.umpc.R
import us.huseli.umpc.data.DynamicPlaylistFilter
import us.huseli.umpc.data.DynamicPlaylistFilterComparator
import us.huseli.umpc.data.DynamicPlaylistFilterKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicPlaylistFilterSection(
    modifier: Modifier = Modifier,
    filter: DynamicPlaylistFilter,
    onChange: (DynamicPlaylistFilter) -> Unit,
) {
    var isKeyDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var isComparatorDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedKey by rememberSaveable { mutableStateOf(filter.key) }
    var selectedComparator by rememberSaveable { mutableStateOf(filter.comparator) }
    var value by rememberSaveable { mutableStateOf(filter.value) }

    Column(modifier = modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = isKeyDropdownExpanded,
            onExpandedChange = { isKeyDropdownExpanded = !isKeyDropdownExpanded },
        ) {
            TextField(
                modifier = Modifier.menuAnchor(),
                readOnly = true,
                value = selectedKey.displayName,
                onValueChange = {},
                label = { Text(stringResource(R.string.filter_by)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isKeyDropdownExpanded) }
            )
            ExposedDropdownMenu(
                expanded = isKeyDropdownExpanded,
                onDismissRequest = { isKeyDropdownExpanded = false },
            ) {
                DynamicPlaylistFilterKey.values().forEach { key ->
                    DropdownMenuItem(
                        text = { Text(key.displayName) },
                        onClick = {
                            if (key != selectedKey) {
                                selectedKey = key
                                onChange(DynamicPlaylistFilter(key, value, selectedComparator))
                            }
                            isKeyDropdownExpanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
        ExposedDropdownMenuBox(
            expanded = isComparatorDropdownExpanded,
            onExpandedChange = { isComparatorDropdownExpanded = !isComparatorDropdownExpanded },
        ) {
            TextField(
                modifier = Modifier.menuAnchor(),
                readOnly = true,
                value = selectedComparator.displayName,
                onValueChange = {},
                label = { Text(stringResource(R.string.comparator)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isComparatorDropdownExpanded) },
            )
            ExposedDropdownMenu(
                expanded = isComparatorDropdownExpanded,
                onDismissRequest = { isComparatorDropdownExpanded = false },
            ) {
                DynamicPlaylistFilterComparator.values().forEach { comparator ->
                    DropdownMenuItem(
                        text = { Text(comparator.displayName) },
                        onClick = {
                            if (comparator != selectedComparator) {
                                selectedComparator = comparator
                                onChange(DynamicPlaylistFilter(selectedKey, value, comparator))
                            }
                            isComparatorDropdownExpanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = {
                value = it
                onChange(DynamicPlaylistFilter(selectedKey, it, selectedComparator))
            },
            singleLine = true,
            label = { Text(stringResource(R.string.value)) },
        )
    }
}
