package us.huseli.umpc

import android.app.NotificationChannel
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.view.KeyEvent
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import us.huseli.umpc.Constants.NOTIFICATION_CHANNEL_ID_NOW_PLAYING
import us.huseli.umpc.compose.App
import us.huseli.umpc.ui.theme.UmpcTheme
import us.huseli.umpc.viewmodels.MPDViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity(), LoggerInterface {
    private val viewModel by viewModels<MPDViewModel>()
    private var isStreaming = false

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID_NOW_PLAYING,
            "Now playing",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        val manager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        createNotificationChannel()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isStreaming.collect { isStreaming = it }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentSong.collect { song ->
                    val appWidgetManager = AppWidgetManager.getInstance(this@MainActivity)
                    val componentName = ComponentName(this@MainActivity, AppWidgetProvider::class.java)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                    val views = RemoteViews(packageName, R.layout.widget).apply {
                        setTextViewText(R.id.song, song?.title ?: "")
                        setTextViewText(R.id.artistAndAlbum, song?.let { "${it.artist} • ${it.album}" } ?: "")
                    }
                    appWidgetManager.updateAppWidget(componentName, views)
                    val updateIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    }
                    sendBroadcast(updateIntent)
                    // appWidgetManager.updateAppWidget(appWidgetIds, views)
                }
            }
        }

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build()
        )

        setContent {
            UmpcTheme {
                App()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isStreaming) return super.onKeyDown(keyCode, event)

        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                viewModel.onVolumeUpPressed()
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                viewModel.onVolumeDownPressed()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
