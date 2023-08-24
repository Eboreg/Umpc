package us.huseli.umpc

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import android.widget.RemoteViews
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.umpc.Constants.MEDIASERVICE_ROOT_ID
import us.huseli.umpc.Constants.NOTIFICATION_CHANNEL_ID_NOW_PLAYING
import us.huseli.umpc.Constants.NOTIFICATION_ID_NOW_PLAYING
import us.huseli.umpc.repository.AlbumArtRepository
import us.huseli.umpc.repository.MPDRepository
import javax.inject.Inject

@AndroidEntryPoint
class MediaService : MediaBrowserServiceCompat(), LoggerInterface {
    @Inject
    lateinit var repo: MPDRepository
    @Inject
    lateinit var albumArtRepo: AlbumArtRepository
    @Inject
    lateinit var ioScope: CoroutineScope

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private val mutex = Mutex()
    private var notification: Notification? = null
    private var isConnected: Boolean = false

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        log("onStartCommand: intent=$intent, flags=$flags, startId=$startId", Log.DEBUG)

        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
        val event = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.extras?.getParcelable("android.intent.extra.KEY_EVENT", KeyEvent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.extras?.getParcelable("android.intent.extra.KEY_EVENT")
        }
        when (event?.keyCode) {
            KeyEvent.KEYCODE_MEDIA_NEXT -> repo.next()
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> repo.previousOrRestart()
            KeyEvent.KEYCODE_MEDIA_PLAY -> repo.play()
            KeyEvent.KEYCODE_MEDIA_PAUSE -> repo.pause()
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> repo.playOrPause()
            KeyEvent.KEYCODE_MEDIA_STOP -> repo.stop()
        }
        appWidgetIds?.forEach { appWidgetId ->
            val views = RemoteViews(applicationContext.packageName, R.layout.widget).apply {
                setTextViewText(R.id.song, repo.currentSong.value?.title ?: "")
                setTextViewText(R.id.artistAndAlbum, repo.currentSong.value?.let { "${it.artist} • ${it.album}" } ?: "")
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        log("onCreate", Log.DEBUG)

        notificationBuilder = getNotificationBuilder()
        showNotification()
        startListeners()
    }

    private fun showNotification() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            with(NotificationManagerCompat.from(this)) {
                notification = notificationBuilder.build().also {
                    notify(NOTIFICATION_ID_NOW_PLAYING, it)
                }
            }
        }
        /*
        notification = notificationBuilder.build().also {
            startForeground(NOTIFICATION_ID_NOW_PLAYING, it)
        }
         */
    }

    private fun hideNotification() {
        with(NotificationManagerCompat.from(this)) {
            cancel(NOTIFICATION_ID_NOW_PLAYING)
        }
        // stopForeground(Service.STOP_FOREGROUND_REMOVE)
    }

    private fun getNotificationBuilder(): NotificationCompat.Builder {
        val intent = Intent(baseContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            baseContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(baseContext, NOTIFICATION_CHANNEL_ID_NOW_PLAYING)
            .setContentIntent(pendingIntent)
            .setContentText("Uffeli buffeli")
            .setContentTitle(baseContext.getString(R.string.app_name))
            // .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1, 2))
        forEachAction { builder.addAction(it) }
        return builder
    }

    private inline fun forEachAction(func: (NotificationCompat.Action) -> Unit) {
        func(
            NotificationCompat.Action(
                R.drawable.ic_previous,
                baseContext.getString(R.string.previous),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    baseContext,
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS,
                ),
            )
        )
        if (repo.playerState.value == PlayerState.PLAY) func(
            NotificationCompat.Action(
                R.drawable.ic_pause,
                baseContext.getString(R.string.pause),
                MediaButtonReceiver.buildMediaButtonPendingIntent(baseContext, PlaybackStateCompat.ACTION_PAUSE),
            )
        ) else func(
            NotificationCompat.Action(
                R.drawable.ic_play,
                baseContext.getString(R.string.play),
                MediaButtonReceiver.buildMediaButtonPendingIntent(baseContext, PlaybackStateCompat.ACTION_PLAY),
            )
        )
        func(
            NotificationCompat.Action(
                R.drawable.ic_next,
                baseContext.getString(R.string.next),
                MediaButtonReceiver.buildMediaButtonPendingIntent(baseContext, PlaybackStateCompat.ACTION_SKIP_TO_NEXT),
            )
        )
    }

    private fun updateWidget() {
        val context = this
        val bitmap = albumArtRepo.currentSongAlbumArt.value?.fullImage?.asAndroidBitmap()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, WidgetProvider::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val views = RemoteViews(context.packageName, R.layout.widget).apply {
            if (bitmap != null && isConnected) setImageViewBitmap(R.id.albumArt, bitmap)
            else setImageViewResource(R.id.albumArt, R.mipmap.ic_launcher)
            setOnClickPendingIntent(android.R.id.background, pendingIntent)
            setTextViewText(R.id.song, repo.currentSong.value?.title ?: "")
            setTextViewText(
                R.id.artistAndAlbum,
                repo.currentSong.value?.let { "${it.artist} • ${it.album.name}" } ?: ""
            )
            setOnClickPendingIntent(
                R.id.playOrPause,
                MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY_PAUSE),
            )
            if (isConnected && repo.playerState.value != PlayerState.STOP) {
                setOnClickPendingIntent(
                    R.id.previous,
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    ),
                )
                setOnClickPendingIntent(
                    R.id.stop,
                    MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP),
                )
                setOnClickPendingIntent(
                    R.id.next,
                    MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT),
                )
                setBoolean(R.id.previous, "setEnabled", true)
                setBoolean(R.id.stop, "setEnabled", true)
                setBoolean(R.id.next, "setEnabled", true)
            } else {
                setBoolean(R.id.previous, "setEnabled", false)
                setBoolean(R.id.stop, "setEnabled", false)
                setBoolean(R.id.next, "setEnabled", false)
            }
            setBoolean(R.id.playOrPause, "setEnabled", isConnected)
            setImageViewResource(
                R.id.playOrPause,
                when (repo.playerState.value) {
                    PlayerState.PLAY -> R.drawable.ic_pause
                    else -> R.drawable.ic_play
                }
            )
        }
        appWidgetManager.updateAppWidget(componentName, views)
    }

    private fun startListeners() {
        ioScope.launch {
            repo.currentSong.collect { song ->
                mutex.withLock {
                    updateWidget()
                    if (song != null) {
                        notificationBuilder.setContentTitle(song.title)
                        notificationBuilder.setContentText("${song.artist} • ${song.album.name}")
                        showNotification()
                    }
                }
            }
        }

        ioScope.launch {
            albumArtRepo.currentSongAlbumArt.collect { albumArt ->
                val bitmap = albumArt?.fullImage?.asAndroidBitmap()
                log("bitmap: width=${bitmap?.width}, height=${bitmap?.height}", Log.DEBUG)
                mutex.withLock {
                    updateWidget()
                    notificationBuilder.setLargeIcon(bitmap)
                    showNotification()
                }
            }
        }

        ioScope.launch {
            repo.playerState.collect { playerState ->
                mutex.withLock {
                    updateWidget()
                    when (playerState) {
                        PlayerState.PLAY -> {
                            notificationBuilder.clearActions()
                            forEachAction { notificationBuilder.addAction(it) }
                            showNotification()
                        }
                        PlayerState.STOP -> hideNotification()
                        PlayerState.PAUSE -> {
                            notificationBuilder.clearActions()
                            forEachAction { notificationBuilder.addAction(it) }
                            showNotification()
                        }
                        PlayerState.UNKNOWN -> {}
                    }
                }
            }
        }

        ioScope.launch {
            repo.connectedServer.collect { connectedServer ->
                mutex.withLock {
                    this@MediaService.isConnected = connectedServer != null
                    updateWidget()
                    when (connectedServer) {
                        null -> hideNotification()
                        else -> showNotification()
                    }
                }
            }
        }
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot =
        BrowserRoot(MEDIASERVICE_ROOT_ID, null)

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(null)
    }
}
