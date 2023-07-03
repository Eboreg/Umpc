package us.huseli.umpc.compose

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.BugReport
import androidx.compose.material.icons.sharp.LibraryMusic
import androidx.compose.material.icons.sharp.QueueMusic
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material.icons.sharp.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.umpc.BuildConfig
import us.huseli.umpc.ContentScreen
import us.huseli.umpc.R

@Composable
fun RowScope.ContentScreenItem(
    activeScreen: ContentScreen,
    contentScreen: ContentScreen,
    icon: ImageVector,
    description: String,
    onClick: (ContentScreen) -> Unit,
) {
    NavigationBarItem(
        selected = activeScreen == contentScreen,
        onClick = { onClick(contentScreen) },
        icon = { Icon(icon, null) },
        label = { Text(description) },
    )
}

@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    activeScreen: ContentScreen,
    onContentScreenClick: (ContentScreen) -> Unit,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        if (BuildConfig.DEBUG) {
            ContentScreenItem(
                activeScreen = activeScreen,
                contentScreen = ContentScreen.DEBUG,
                icon = Icons.Sharp.BugReport,
                description = stringResource(R.string.debug),
                onClick = onContentScreenClick,
            )
        }
        ContentScreenItem(
            activeScreen = activeScreen,
            contentScreen = ContentScreen.SETTTINGS,
            icon = Icons.Sharp.Settings,
            description = stringResource(R.string.settings),
            onClick = onContentScreenClick,
        )
        ContentScreenItem(
            activeScreen = activeScreen,
            contentScreen = ContentScreen.QUEUE,
            icon = Icons.Sharp.QueueMusic,
            description = stringResource(R.string.queue),
            onClick = onContentScreenClick,
        )
        ContentScreenItem(
            activeScreen = activeScreen,
            contentScreen = ContentScreen.LIBRARY,
            icon = Icons.Sharp.LibraryMusic,
            description = stringResource(R.string.library),
            onClick = onContentScreenClick,
        )
        ContentScreenItem(
            activeScreen = activeScreen,
            contentScreen = ContentScreen.SEARCH,
            icon = Icons.Sharp.Search,
            description = stringResource(R.string.search),
            onClick = onContentScreenClick,
        )
    }
}
