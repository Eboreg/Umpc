package us.huseli.umpc

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.RemoteViews
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.media.session.MediaButtonReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import us.huseli.umpc.mpd.MPDRepository
import javax.inject.Inject

@AndroidEntryPoint
class WidgetProvider : AppWidgetProvider(), LoggerInterface {
    @Inject
    lateinit var repo: MPDRepository
    @Inject
    lateinit var ioScope: CoroutineScope
    private var appWidgetIds: IntArray = intArrayOf()

    override fun onEnabled(context: Context) {
        log("onEnabled, context=$context")
        startListeners(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        this.appWidgetIds = appWidgetIds
        appWidgetIds.forEach { appWidgetId ->
            val bitmap = repo.engines.image.currentSongAlbumArt.value?.fullImage?.asAndroidBitmap()
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val updateIntent = Intent(context, javaClass).apply {
                setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            }
            val pendingUpdateIntent: PendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                updateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT,
            )
            ioScope.launch {
                repo.currentSong.collect { song ->

                }
            }
            val views = RemoteViews(context.packageName, R.layout.widget).apply {
                if (bitmap != null) setImageViewBitmap(R.id.albumArt, bitmap)
                else setImageViewResource(R.id.albumArt, R.mipmap.ic_launcher)

                repo.currentSong.value?.let { song ->
                    setTextViewText(R.id.song, song.title)
                    setTextViewText(R.id.artistAndAlbum, "${song.artist} • ${song.album.name}")
                }

                setOnClickPendingIntent(R.id.albumArt, pendingIntent)
                setOnClickPendingIntent(
                    R.id.previous,
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS,
                    ),
                )
                setOnClickPendingIntent(
                    R.id.playOrPause,
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE,
                    ),
                )
                setOnClickPendingIntent(
                    R.id.stop,
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_STOP,
                    ),
                )
                setOnClickPendingIntent(
                    R.id.next,
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT,
                    ),
                )
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        log("onRecieve, intent=$intent")
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, AppWidgetProvider::class.java)
        try {
            val views = RemoteViews(context.packageName, R.layout.widget).apply {
                setTextViewText(R.id.song, "apan")
                setTextViewText(R.id.artistAndAlbum, "ap")
                /*
                setTextViewText(R.id.song, repo.currentSong.value?.title ?: "")
                setTextViewText(
                    R.id.artistAndAlbum,
                    repo.currentSong.value?.let { "${it.artist} • ${it.album.name}" } ?: "")
                 */
            }
            // appWidgetManager.updateAppWidget(componentName, views)
            appWidgetManager.updateAppWidget(appWidgetIds, views)
        } catch (_: UninitializedPropertyAccessException) {}
    }

    private fun startListeners(context: Context) {
        log("startListeners, context=$context, repo=$repo")
        ioScope.launch {
            repo.currentSong.filterNotNull().distinctUntilChanged().collect { song ->
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds =
                    appWidgetManager.getAppWidgetIds(ComponentName(context, AppWidgetProvider::class.java))
                val views = RemoteViews(context.packageName, R.layout.widget).apply {
                    setTextViewText(R.id.song, song.title)
                    setTextViewText(R.id.artistAndAlbum, "${song.artist} • ${song.album}")
                }
                log("currentSong=$song")
                appWidgetManager.updateAppWidget(appWidgetIds, views)
            }
        }
        ioScope.launch {
            repo.engines.image.currentSongAlbumArt.collect { albumArt ->
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds =
                    appWidgetManager.getAppWidgetIds(ComponentName(context, AppWidgetProvider::class.java))
                val bitmap = albumArt?.fullImage?.asAndroidBitmap()
                val views = RemoteViews(context.packageName, R.layout.widget).apply {
                    if (bitmap != null) setImageViewBitmap(R.id.albumArt, bitmap)
                    else setImageViewResource(R.id.albumArt, R.mipmap.ic_launcher)
                }
                appWidgetManager.updateAppWidget(appWidgetIds, views)
            }
        }
        ioScope.launch {
            repo.playerState.collect { playerState ->
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds =
                    appWidgetManager.getAppWidgetIds(ComponentName(context, AppWidgetProvider::class.java))
                val views = RemoteViews(context.packageName, R.layout.widget).apply {
                    when (playerState) {
                        PlayerState.PLAY -> setImageViewResource(R.id.playOrPause, R.drawable.ic_pause)
                        PlayerState.STOP -> setImageViewResource(R.id.playOrPause, R.drawable.ic_play)
                        PlayerState.PAUSE -> setImageViewResource(R.id.playOrPause, R.drawable.ic_play)
                    }
                }
                log("playerState=$playerState")
                appWidgetManager.updateAppWidget(appWidgetIds, views)
            }
        }
    }
}
