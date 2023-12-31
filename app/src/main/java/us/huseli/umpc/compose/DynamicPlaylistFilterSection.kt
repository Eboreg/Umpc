package us.huseli.umpc.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import us.huseli.umpc.data.DynamicPlaylistFilter
import us.huseli.umpc.data.MPDVersion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicPlaylistFilterSection(
    modifier: Modifier = Modifier,
    filter: DynamicPlaylistFilter,
    protocolVersion: MPDVersion,
    onChange: (DynamicPlaylistFilter) -> Unit,
    onAdd: () -> Unit,
) {
    var isKeyDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var isComparatorDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedKey by rememberSaveable(filter) { mutableStateOf(filter.key) }
    var selectedComparator by rememberSaveable(filter) { mutableStateOf(filter.comparator) }
    var value by rememberSaveable(filter) { mutableStateOf(filter.value) }

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ExposedDropdownMenuBox(
            expanded = isKeyDropdownExpanded,
            onExpandedChange = { isKeyDropdownExpanded = it },
            modifier = Modifier.weight(1f),
        ) {
            TextField(
                modifier = Modifier.menuAnchor(),
                readOnly = true,
                value = selectedKey.display,
                onValueChange = {},
                label = { Text(stringResource(R.string.filter_by)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isKeyDropdownExpanded) }
            )
            ExposedDropdownMenu(
                expanded = isKeyDropdownExpanded,
                onDismissRequest = { isKeyDropdownExpanded = false },
            ) {
                DynamicPlaylistFilter.Key.values().forEach { key ->
                    DropdownMenuItem(
                        text = { Text(key.display) },
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
            modifier = Modifier.weight(1f),
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
                DynamicPlaylistFilter.comparatorValuesByVersion(protocolVersion).forEach { comparator ->
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
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            onValueChange = {
                value = it
                onChange(DynamicPlaylistFilter(selectedKey, it, selectedComparator))
            },
            singleLine = true,
            label = { Text(stringResource(R.string.value)) },
        )
        TextButton(
            onClick = onAdd,
            content = { Text(stringResource(R.string.add)) },
        )
    }
}
