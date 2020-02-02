package org.musicpimp.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.RequiresApi

import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver
import org.musicpimp.MainActivity

import org.musicpimp.R
import org.musicpimp.TrackId
import timber.log.Timber

/**
 * Keeps track of a notification and updates it automatically for a given MediaSession. This is
 * required so that the music service don't get killed during playback.
 */
class MediaNotificationManager(val service: PimpMediaService) {
    companion object {
        const val notificationId = 412
    }
    private val tag = "MediaNotificationManager"
    private val channelId = "org.musicpimp.audio.channel"
    private val requestCode = 501

    private val mPlayAction: NotificationCompat.Action = NotificationCompat.Action(
        R.drawable.ic_play_arrow_24px,
        service.getString(R.string.play),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_PLAY
        )
    )
    private val mPauseAction: NotificationCompat.Action = NotificationCompat.Action(
        R.drawable.ic_pause_24px,
        service.getString(R.string.pause),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_PAUSE
        )
    )
    private val mNextAction: NotificationCompat.Action = NotificationCompat.Action(
        R.drawable.ic_skip_next_24px,
        service.getString(R.string.next),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        )
    )
    private val mPrevAction: NotificationCompat.Action = NotificationCompat.Action(
        R.drawable.ic_skip_previous_24px,
        service.getString(R.string.previous),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        )
    )
    val mNotificationManager: NotificationManager =
        service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


    init {
        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        mNotificationManager.cancelAll()
    }

    fun onDestroy() {
        Timber.d(tag, "onDestroy: ")
    }

    fun getNotification(
        metadata: MediaMetadataCompat,
        state: PlaybackStateCompat,
        token: MediaSessionCompat.Token
    ): Notification {
        val isPlaying = state.state == PlaybackStateCompat.STATE_PLAYING
        val description = metadata.description
        val builder =
            buildNotification(state, token, isPlaying, description)
        return builder.build()
    }

    private fun buildNotification(
        state: PlaybackStateCompat,
        token: MediaSessionCompat.Token,
        isPlaying: Boolean,
        description: MediaDescriptionCompat
    ): NotificationCompat.Builder {
        // Create the (mandatory) notification channel when running on Android Oreo.
        if (isAndroidOOrHigher()) {
            createChannel()
        }

        val builder = NotificationCompat.Builder(service, channelId)
        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(token)
                .setShowActionsInCompactView(0, 1, 2)
                // For backwards compatibility with Android L and earlier.
                .setShowCancelButton(true)
                .setCancelButtonIntent(
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        service,
                        PlaybackStateCompat.ACTION_STOP
                    )
                )
        )
            .setColor(ContextCompat.getColor(service, R.color.colorAccent))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            // Pending intent that is fired when user clicks on notification.
            .setContentIntent(createContentIntent())
            .setContentTitle(description.title)
            .setContentText(description.subtitle)
//            .setLargeIcon(description.mediaId?.let { id -> library.albumCover(TrackId(id), service) })
            // When notification is deleted (when playback is paused and notification can be
            // deleted) fire MediaButtonPendingIntent with ACTION_STOP.
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    service, PlaybackStateCompat.ACTION_STOP
                )
            )
            // Show controls on lock screen even when user hides sensitive content.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // If skip to next action is enabled.
        if ((state.actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0L) {
            builder.addAction(mPrevAction)
        }

        builder.addAction(if (isPlaying) mPauseAction else mPlayAction)

        // If skip to prev action is enabled.
        if ((state.actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0L) {
            builder.addAction(mNextAction)
        }

        return builder
    }

    // Does nothing on versions of Android earlier than O.
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        if (mNotificationManager.getNotificationChannel(channelId) == null) {
            // The user-visible name of the channel.
            val name = "MediaSession"
            // The user-visible description of the channel.
            val description = "MediaSession and MediaPlayer"
            val importance = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel(channelId, name, importance).apply {
                // Configure the notification channel.
                setDescription(description)
                enableLights(true)
                // Sets the notification light color for notifications posted to this
                // channel, if the device supports this feature.
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            }
            mNotificationManager.createNotificationChannel(mChannel)
            Log.d(tag, "createChannel: New channel created")
        } else {
            Log.d(tag, "createChannel: Existing channel reused")
        }
    }

    private fun isAndroidOOrHigher(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    private fun createContentIntent(): PendingIntent {
        val openUI = Intent(service, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            service, requestCode, openUI, PendingIntent.FLAG_CANCEL_CURRENT
        )
    }
}
