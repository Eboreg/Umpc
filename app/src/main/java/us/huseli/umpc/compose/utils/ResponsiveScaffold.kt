package us.huseli.umpc.compose.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import us.huseli.umpc.ContentScreen
import us.huseli.umpc.compose.HorizontalMainMenu
import us.huseli.umpc.compose.VerticalMainMenu
import us.huseli.umpc.isInLandscapeMode

@Composable
fun ResponsiveScaffold(
    activeScreen: ContentScreen,
    onMenuItemClick: (ContentScreen) -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    if (isInLandscapeMode()) {
        Scaffold(
            snackbarHost = snackbarHost,
            bottomBar = bottomBar,
        ) { innerPadding ->
            Row {
                Column(modifier = Modifier.width(IntrinsicSize.Min)) {
                    VerticalMainMenu(
                        modifier = Modifier.padding(innerPadding),
                        activeScreen = activeScreen,
                        onMenuItemClick = onMenuItemClick,
                    )
                }
                Box { content(innerPadding) }
            }
        }
    } else {
        Scaffold(
            snackbarHost = snackbarHost,
            bottomBar = bottomBar,
            topBar = {
                HorizontalMainMenu(
                    activeScreen = activeScreen,
                    onMenuItemClick = onMenuItemClick,
                )
            },
            content = content
        )
    }
}
