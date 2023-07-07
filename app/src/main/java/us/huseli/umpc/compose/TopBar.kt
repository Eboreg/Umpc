package us.huseli.umpc.compose

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.BugReport
import androidx.compose.material.icons.sharp.LibraryMusic
import androidx.compose.material.icons.sharp.PlaylistPlay
import androidx.compose.material.icons.sharp.QueueMusic
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material.icons.sharp.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import us.huseli.umpc.BuildConfig
import us.huseli.umpc.ContentScreen
import us.huseli.umpc.R

class MainMenuItem(
    val contentScreen: ContentScreen,
    val icon: ImageVector,
    val description: String,
)

@Composable
fun getMainMenuItems(): List<MainMenuItem> {
    val items = mutableListOf(
        MainMenuItem(ContentScreen.SETTINGS, Icons.Sharp.Settings, stringResource(R.string.settings)),
        MainMenuItem(ContentScreen.LIBRARY, Icons.Sharp.LibraryMusic, stringResource(R.string.library)),
        MainMenuItem(ContentScreen.QUEUE, Icons.Sharp.PlaylistPlay, stringResource(R.string.queue)),
        MainMenuItem(ContentScreen.PLAYLISTS, Icons.Sharp.QueueMusic, stringResource(R.string.playlists)),
        MainMenuItem(ContentScreen.SEARCH, Icons.Sharp.Search, stringResource(R.string.search)),
    )
    if (BuildConfig.DEBUG)
        items.add(0, MainMenuItem(ContentScreen.DEBUG, Icons.Sharp.BugReport, stringResource(R.string.debug)))
    return items
}

@Composable
fun HorizontalMainMenu(
    activeScreen: ContentScreen,
    onMenuItemClick: (ContentScreen) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        getMainMenuItems().forEach { item ->
            NavigationBarItem(
                selected = activeScreen == item.contentScreen,
                onClick = { onMenuItemClick(item.contentScreen) },
                icon = { Icon(item.icon, null) },
                label = { Text(item.description) },
            )
        }
    }
}

@Composable
fun VerticalMainMenu(
    activeScreen: ContentScreen,
    onMenuItemClick: (ContentScreen) -> Unit,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.widthIn(max = 250.dp)) {
                getMainMenuItems().forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, null) },
                        label = { Text(item.description) },
                        selected = activeScreen == item.contentScreen,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onMenuItemClick(item.contentScreen)
                        },
                    )
                }
            }
        },
        content = content,
    )
}
