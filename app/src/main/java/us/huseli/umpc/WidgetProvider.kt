package us.huseli.umpc

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.RemoteViews
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.media.session.MediaButtonReceiver
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.umpc.repository.MPDRepository
import javax.inject.Inject

@AndroidEntryPoint
class WidgetProvider : AppWidgetProvider(), LoggerInterface {
    @Inject
    lateinit var repo: MPDRepository

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(repo, context, appWidgetManager, appWidgetId)
        }
    }
}

internal fun updateAppWidget(
    repo: MPDRepository,
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
) {
    val pendingIntent: PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val bitmap = repo.currentSongAlbumArt.value?.fullImage?.asAndroidBitmap()
    // Construct the RemoteViews object
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

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}
