package us.huseli.umpc.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.huseli.umpc.isInLandscapeMode

@Composable
fun SubMenuScreen(
    modifier: Modifier = Modifier,
    menu: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (isInLandscapeMode()) {
        Row(modifier = modifier) {
            Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxHeight()) {
                Column(
                    modifier = Modifier.fillMaxHeight().padding(10.dp),
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
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
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
