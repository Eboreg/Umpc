package us.huseli.umpc.compose

import android.content.Intent
import android.os.Looper
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import us.huseli.umpc.AlbumDestination
import us.huseli.umpc.ArtistDestination
import us.huseli.umpc.BuildConfig
import us.huseli.umpc.ContentScreen
import us.huseli.umpc.DebugDestination
import us.huseli.umpc.LibraryDestination
import us.huseli.umpc.MediaService
import us.huseli.umpc.PlaylistDetailsDestination
import us.huseli.umpc.PlaylistListDestination
import us.huseli.umpc.QueueDestination
import us.huseli.umpc.R
import us.huseli.umpc.SearchDestination
import us.huseli.umpc.SettingsDestination
import us.huseli.umpc.compose.screens.AlbumScreen
import us.huseli.umpc.compose.screens.ArtistScreen
import us.huseli.umpc.compose.screens.CoverScreen
import us.huseli.umpc.compose.screens.DebugScreen
import us.huseli.umpc.compose.screens.LibraryScreen
import us.huseli.umpc.compose.screens.PlaylistListScreen
import us.huseli.umpc.compose.screens.QueueScreen
import us.huseli.umpc.compose.screens.SearchScreen
import us.huseli.umpc.compose.screens.SettingsScreen
import us.huseli.umpc.compose.screens.StoredPlaylistScreen
import us.huseli.umpc.compose.utils.MessageFlash
import us.huseli.umpc.compose.utils.ResponsiveScaffold
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDPlaylist
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.getActivity
import us.huseli.umpc.viewmodels.LibraryViewModel
import us.huseli.umpc.viewmodels.MPDViewModel
import us.huseli.umpc.viewmodels.PlaylistListViewModel
import us.huseli.umpc.viewmodels.QueueViewModel
import us.huseli.umpc.viewmodels.SearchViewModel

@Composable
fun App(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    navController: NavHostController = rememberNavController(),
    viewModel: MPDViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    queueViewModel: QueueViewModel = hiltViewModel(),
    playlistViewModel: PlaylistListViewModel = hiltViewModel(),
) {
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val error by viewModel.error.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val showVolumeFlash by viewModel.showVolumeFlash.collectAsStateWithLifecycle()
    val volume by viewModel.volume.collectAsStateWithLifecycle()
    val loadingDynamicPlaylist by viewModel.loadingDynamicPlaylist.collectAsStateWithLifecycle()
    val playlists by viewModel.storedPlaylists.collectAsStateWithLifecycle()

    var activeScreen by rememberSaveable { mutableStateOf(ContentScreen.NONE) }
    var isCoverShown by rememberSaveable { mutableStateOf(false) }
    var songToAddToPlaylist by rememberSaveable { mutableStateOf<MPDSong?>(null) }

    val onGotoAlbumClick: (MPDAlbum) -> Unit = {
        navController.navigate(AlbumDestination.route(it))
        isCoverShown = false
    }

    val onGotoArtistClick: (String) -> Unit = {
        navController.navigate(ArtistDestination.route(it))
        isCoverShown = false
    }

    val onAddSongToPlaylistClick: (MPDSong) -> Unit = { songToAddToPlaylist = it }

    fun navigate(route: String, navOptions: NavOptions? = null) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            context.getActivity()?.runOnUiThread { navController.navigate(route, navOptions) }
        } else navController.navigate(route, navOptions)
    }

    /**
     * Q: What does the stuff below do?
     *
     * A: Glad you asked! `onBackPressedCallback` overrides the NavController's
     * backpress logic so a backpress only closes the cover screen and nothing
     * more. The 2nd LaunchedEffect makes sure `onBackPressedCallback` only
     * takes effect when the cover screen is actually open, otherwise
     * navigation works as usual.
     */
    val onBackPressedCallback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isCoverShown = false
            }
        }
    }

    LaunchedEffect(lifecycleOwner, onBackPressedDispatcher) {
        onBackPressedDispatcher?.let { dispatcher ->
            navController.setLifecycleOwner(lifecycleOwner)
            navController.setOnBackPressedDispatcher(dispatcher)
            dispatcher.addCallback(lifecycleOwner, onBackPressedCallback)
        }
    }

    LaunchedEffect(isCoverShown) {
        onBackPressedCallback.isEnabled = isCoverShown
    }

    LaunchedEffect(error) {
        error?.let {
            val result = snackbarHostState.showSnackbar(
                message = it.message,
                actionLabel = it.actionLabel,
                withDismissAction = true,
                duration = if (it.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) it.onActionPerformed?.invoke()
            viewModel.clearError()
        }
    }

    LaunchedEffect(message) {
        message?.let {
            val result = snackbarHostState.showSnackbar(
                message = it.message,
                actionLabel = it.actionLabel,
                withDismissAction = true,
                duration = if (it.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) it.onActionPerformed?.invoke()
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(Unit) {
        context.startForegroundService(Intent(context, MediaService::class.java))
    }

    songToAddToPlaylist?.let { song ->
        val successMessage = pluralStringResource(R.plurals.add_songs_playlist_success, 1, 1)

        AddToPlaylistDialog(
            title = "\"${song.title}\"",
            playlists = playlists,
            onConfirm = { playlistName ->
                viewModel.addSongToStoredPlaylist(song, playlistName) { response ->
                    if (response.isSuccess) viewModel.addMessage(successMessage)
                    else response.error?.let { viewModel.addMessage(it) }
                }
                songToAddToPlaylist = null
            },
            onCancel = { songToAddToPlaylist = null },
        )
    }

    ResponsiveScaffold(
        activeScreen = activeScreen,
        onMenuItemClick = {
            when (it) {
                ContentScreen.DEBUG ->
                    navController.navigate(if (BuildConfig.DEBUG) DebugDestination.route else QueueDestination.route)
                ContentScreen.QUEUE -> navController.navigate(QueueDestination.route)
                ContentScreen.LIBRARY -> navController.navigate(LibraryDestination.route)
                ContentScreen.SETTINGS -> navController.navigate(SettingsDestination.route)
                ContentScreen.SEARCH -> navController.navigate(SearchDestination.route)
                ContentScreen.NONE -> {}
                ContentScreen.PLAYLISTS -> navController.navigate(PlaylistListDestination.route)
            }
            isCoverShown = false
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!isCoverShown && currentSong != null) {
                BottomBar(onSurfaceClick = { isCoverShown = true })
            }
        },
    ) { innerPadding ->
        NavHost(
            modifier = modifier.padding(innerPadding),
            navController = navController,
            startDestination = QueueDestination.route,
        ) {
            composable(route = QueueDestination.route) {
                activeScreen = ContentScreen.QUEUE
                QueueScreen(
                    viewModel = queueViewModel,
                    onGotoAlbumClick = onGotoAlbumClick,
                    onGotoArtistClick = onGotoArtistClick,
                    onAddSongToPlaylistClick = onAddSongToPlaylistClick,
                )
            }

            composable(route = LibraryDestination.route) {
                activeScreen = ContentScreen.LIBRARY
                LibraryScreen(
                    viewModel = libraryViewModel,
                    onGotoAlbumClick = onGotoAlbumClick,
                    onGotoArtistClick = onGotoArtistClick,
                )
            }

            composable(route = DebugDestination.route) {
                activeScreen = ContentScreen.DEBUG
                DebugScreen()
            }

            composable(route = SettingsDestination.route) {
                activeScreen = ContentScreen.SETTINGS
                SettingsScreen()
            }

            composable(route = SearchDestination.route) {
                activeScreen = ContentScreen.SEARCH
                SearchScreen(
                    viewModel = searchViewModel,
                    onGotoAlbumClick = onGotoAlbumClick,
                    onGotoArtistClick = onGotoArtistClick,
                    onAddSongToPlaylistClick = onAddSongToPlaylistClick,
                )
            }

            composable(
                route = AlbumDestination.routeTemplate,
                arguments = AlbumDestination.arguments,
            ) {
                AlbumScreen(
                    onGotoArtistClick = onGotoArtistClick,
                    onAddSongToPlaylistClick = onAddSongToPlaylistClick,
                )
            }

            composable(
                route = ArtistDestination.routeTemplate,
                arguments = ArtistDestination.arguments
            ) {
                ArtistScreen(onGotoAlbumClick = onGotoAlbumClick)
            }

            composable(route = PlaylistListDestination.route) {
                activeScreen = ContentScreen.PLAYLISTS
                PlaylistListScreen(
                    viewModel = playlistViewModel,
                    onGotoStoredPlaylistClick = { navigate(PlaylistDetailsDestination.route(it)) }
                )
            }

            composable(
                route = PlaylistDetailsDestination.routeTemplate,
                arguments = PlaylistDetailsDestination.arguments,
            ) {
                StoredPlaylistScreen(
                    onGotoAlbumClick = onGotoAlbumClick,
                    onGotoArtistClick = onGotoArtistClick,
                    onPlaylistDeleted = { navigate(PlaylistListDestination.route) },
                    onPlaylistRenamed = { newName ->
                        navigate(PlaylistDetailsDestination.route(MPDPlaylist(newName)))
                    },
                    onAddSongToPlaylistClick = onAddSongToPlaylistClick,
                )
            }
        }

        AnimatedVisibility(
            visible = isCoverShown,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            CoverScreen(
                modifier = modifier.padding(innerPadding),
                onGotoAlbumClick = onGotoAlbumClick,
                onGotoArtistClick = onGotoArtistClick,
                onDismiss = { isCoverShown = false }
            )
        }

        VolumeFlash(
            modifier = Modifier.padding(innerPadding),
            volume = volume,
            isVisible = showVolumeFlash && !isCoverShown,
            onHide = { viewModel.resetShowVolumeFlash() }
        )
        if (showVolumeFlash && isCoverShown) viewModel.resetShowVolumeFlash()

        if (loadingDynamicPlaylist) {
            MessageFlash(
                modifier = Modifier.padding(innerPadding),
                content = {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp).size(24.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(stringResource(R.string.loading_dynamic_playlist))
                    }
                }
            )
        }
    }
}
