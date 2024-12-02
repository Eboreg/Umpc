package us.huseli.umpc.compose.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.isInLandscapeMode

@Composable
inline fun SubMenuScreen(
    modifier: Modifier = Modifier,
    landscapeMenuPadding: PaddingValues = PaddingValues(10.dp),
    portraitMenuPadding: PaddingValues = PaddingValues(start = 10.dp, end = 10.dp, bottom = 10.dp),
    crossinline menu: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (isInLandscapeMode()) {
        Row(modifier = modifier) {
            Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxHeight()) {
                Column(
                    modifier = Modifier.fillMaxHeight().padding(landscapeMenuPadding),
                    verticalArrangement = Arrangement.SpaceAround,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    menu()
                }
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    } else {
        Column(modifier = modifier) {
            Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(portraitMenuPadding),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    menu()
                }
            }
            content()
        }
    }
}
