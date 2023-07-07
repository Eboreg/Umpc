package us.huseli.umpc.compose

import android.os.Looper
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import us.huseli.umpc.PlaylistDestination
import us.huseli.umpc.PlaylistListDestination
import us.huseli.umpc.QueueDestination
import us.huseli.umpc.SearchDestination
import us.huseli.umpc.SettingsDestination
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDPlaylist
import us.huseli.umpc.getActivity
import us.huseli.umpc.isInLandscapeMode
import us.huseli.umpc.viewmodels.MPDViewModel
import us.huseli.umpc.viewmodels.SearchViewModel

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

@Composable
fun App(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    viewModel: MPDViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val error by viewModel.error.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()

    val queueListState = rememberLazyListState()
    val libraryListState = rememberLazyListState()
    val searchListState = rememberLazyListState()

    var activeScreen by rememberSaveable { mutableStateOf(ContentScreen.NONE) }
    var isCoverShown by rememberSaveable { mutableStateOf(false) }

    val onGotoAlbumClick: (MPDAlbum) -> Unit = {
        navController.navigate(AlbumDestination.route(it))
        isCoverShown = false
    }

    val onGotoArtistClick: (String) -> Unit = {
        navController.navigate(ArtistDestination.route(it))
        isCoverShown = false
    }

    fun navigate(route: String, navOptions: NavOptions? = null) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            context.getActivity()?.runOnUiThread {
                navController.navigate(route, navOptions)
            }
        } else {
            navController.navigate(route, navOptions)
        }
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

    ResponsiveScaffold(
        activeScreen = activeScreen,
        onMenuItemClick = {
            when (it) {
                ContentScreen.DEBUG -> navController.navigate(
                    if (BuildConfig.DEBUG) DebugDestination.route else QueueDestination.route
                )
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
                activeScreen = ContentScreen.SETTINGS
                SettingsScreen()
            }

            composable(route = SearchDestination.route) {
                activeScreen = ContentScreen.SEARCH
                SearchScreen(
                    viewModel = searchViewModel,
                    listState = searchListState,
                    onGotoAlbumClick = onGotoAlbumClick,
                    onGotoArtistClick = onGotoArtistClick,
                )
            }

            composable(
                route = AlbumDestination.routeTemplate,
                arguments = AlbumDestination.arguments,
            ) {
                AlbumScreen(onGotoArtistClick = onGotoArtistClick)
            }

            composable(
                route = ArtistDestination.routeTemplate,
                arguments = ArtistDestination.arguments
            ) {
                ArtistScreen(onGotoAlbumClick = onGotoAlbumClick)
            }

            composable(route = PlaylistListDestination.route) {
                activeScreen = ContentScreen.PLAYLISTS
                PlaylistListScreen(onGotoPlaylistClick = { navController.navigate(PlaylistDestination.route(it)) })
            }

            composable(
                route = PlaylistDestination.routeTemplate,
                arguments = PlaylistDestination.arguments,
            ) {
                PlaylistScreen(
                    onGotoAlbumClick = onGotoAlbumClick,
                    onGotoArtistClick = onGotoArtistClick,
                    onPlaylistDeleted = { navigate(PlaylistListDestination.route) },
                    onPlaylistRenamed = { newName ->
                        navigate(PlaylistDestination.route(MPDPlaylist(newName)))
                    }
                )
            }
        }

        if (isCoverShown) {
            CoverScreen(
                modifier = modifier.padding(innerPadding),
                onGotoAlbumClick = onGotoAlbumClick,
                onGotoArtistClick = onGotoArtistClick,
                onDismiss = { isCoverShown = false }
            )
        }
    }
}
