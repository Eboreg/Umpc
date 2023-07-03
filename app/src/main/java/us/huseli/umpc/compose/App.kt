package us.huseli.umpc.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import us.huseli.umpc.AlbumDestination
import us.huseli.umpc.ArtistDestination
import us.huseli.umpc.BuildConfig
import us.huseli.umpc.ContentScreen
import us.huseli.umpc.CoverDestination
import us.huseli.umpc.DebugDestination
import us.huseli.umpc.LibraryDestination
import us.huseli.umpc.QueueDestination
import us.huseli.umpc.SearchDestination
import us.huseli.umpc.SettingsDestination
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.viewmodels.MPDViewModel
import us.huseli.umpc.viewmodels.SettingsViewModel

@Composable
fun App(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    viewModel: MPDViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val error by viewModel.error.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val currentSongAlbumArt by viewModel.currentSongAlbumArt.collectAsStateWithLifecycle()
    val currentSongElapsed by viewModel.currentSongElapsed.collectAsStateWithLifecycle()
    val currentSongDuration by viewModel.currentSongDuration.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    val streamingUrl by settingsViewModel.streamingUrl.collectAsStateWithLifecycle()
    val queueListState = rememberLazyListState()
    val libraryListState = rememberLazyListState()
    val searchListState = rememberLazyListState()
    var activeScreen by rememberSaveable { mutableStateOf(ContentScreen.COVER) }

    val onGotoAlbumClick: (MPDSong) -> Unit = { navController.navigate(AlbumDestination.route(it)) }
    val onGotoArtistClick: (MPDSong) -> Unit = { navController.navigate(ArtistDestination.route(it)) }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (activeScreen != ContentScreen.COVER) {
                currentSong?.let { song ->
                    BottomBar(
                        albumArt = currentSongAlbumArt,
                        title = song.title,
                        artist = song.artist,
                        album = song.album,
                        playerState = playerState,
                        isStreaming = isStreaming,
                        currentSongDuration = currentSongDuration,
                        currentSongElapsed = currentSongElapsed,
                        showStreamingIcon = streamingUrl != null,
                        onPlayPauseClick = { viewModel.playOrPause() },
                        onSurfaceClick = { navController.navigate(CoverDestination.route) },
                        onStreamingChange = { viewModel.toggleStream(it) },
                    )
                }
            }
        },
        topBar = {
            TopBar(
                activeScreen = activeScreen,
                onContentScreenClick = {
                    when (it) {
                        ContentScreen.DEBUG -> navController.navigate(
                            if (BuildConfig.DEBUG) DebugDestination.route else QueueDestination.route
                        )
                        ContentScreen.QUEUE -> navController.navigate(QueueDestination.route)
                        ContentScreen.LIBRARY -> navController.navigate(LibraryDestination.route)
                        ContentScreen.COVER -> navController.navigate(CoverDestination.route)
                        ContentScreen.SETTTINGS -> navController.navigate(SettingsDestination.route)
                        ContentScreen.SEARCH -> navController.navigate(SearchDestination.route)
                        ContentScreen.NONE -> {}
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            modifier = modifier.padding(innerPadding),
            navController = navController,
            startDestination = QueueDestination.route,
        ) {
            composable(route = CoverDestination.route) {
                activeScreen = ContentScreen.COVER
                CoverScreen(
                    onGotoAlbumClick = onGotoAlbumClick,
                    onGotoArtistClick = onGotoArtistClick,
                )
            }

            composable(route = QueueDestination.route) {
                activeScreen = ContentScreen.QUEUE
                QueueScreen(
                    listState = queueListState,
                    onGotoAlbumClick = onGotoAlbumClick,
                    onGotoArtistClick = onGotoArtistClick,
                )
            }

            composable(route = LibraryDestination.route) {
                activeScreen = ContentScreen.LIBRARY
                LibraryScreen(
                    listState = libraryListState,
                    onGotoAlbumClick = onGotoAlbumClick,
                    onGotoArtistClick = onGotoArtistClick,
                )
            }

            composable(route = DebugDestination.route) {
                activeScreen = ContentScreen.DEBUG
                DebugScreen()
            }

            composable(route = SettingsDestination.route) {
                activeScreen = ContentScreen.SETTTINGS
                SettingsScreen()
            }

            composable(route = SearchDestination.route) {
                activeScreen = ContentScreen.SEARCH
                SearchScreen(
                    listState = searchListState,
                    onGotoAlbumClick = onGotoAlbumClick,
                    onGotoArtistClick = onGotoArtistClick,
                )
            }

            composable(
                route = AlbumDestination.routeTemplate,
                arguments = AlbumDestination.arguments,
            ) {
                activeScreen = ContentScreen.NONE
                AlbumScreen(onGotoArtistClick = onGotoArtistClick)
            }

            composable(
                route = ArtistDestination.routeTemplate,
                arguments = ArtistDestination.arguments
            ) {
                activeScreen = ContentScreen.NONE
                ArtistScreen(onGotoAlbumClick = onGotoAlbumClick)
            }
        }
    }
}
