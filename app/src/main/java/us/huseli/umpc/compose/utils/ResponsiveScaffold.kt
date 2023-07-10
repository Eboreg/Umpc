package us.huseli.umpc.compose.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
        VerticalMainMenu(
            activeScreen = activeScreen,
            onMenuItemClick = onMenuItemClick,
        ) {
            Scaffold(
                snackbarHost = snackbarHost,
                bottomBar = bottomBar,
                content = content
            )
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