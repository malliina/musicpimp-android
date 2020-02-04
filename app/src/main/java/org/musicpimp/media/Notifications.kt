package org.musicpimp.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import org.musicpimp.MainActivity
import org.musicpimp.R
import org.musicpimp.Track
import timber.log.Timber

class Notifications(private val ctx: Context) {
    companion object {
        private const val channelId = "org.musicpimp.audio.channel2"
        const val PIMP_NOTIFICATION_ID = 666
    }

    private val notificationManager: NotificationManager
        get() = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /** http://developer.android.com/guide/topics/ui/notifiers/notifications.html#Managing
     *
     * @param track track to show in notification
     */
    fun displayTrackNotification(track: Track, playing: Boolean, largeIcon: Bitmap?) {
        displayNotification(track, playing) { b ->
            if (largeIcon != null) b.setLargeIcon(largeIcon) else b
        }
    }

    private fun displayNotification(
        track: Track,
        playing: Boolean,
        f: (b: NotificationCompat.Builder) -> NotificationCompat.Builder
    ) {
        // Create the (mandatory) notification channel when running on Android Oreo or higher
        if (isAndroidOOrHigher()) {
            createChannel()
        }
        val contentText = if (playing) "Playing" else "Paused"
        val builder = f(
            NotificationCompat.Builder(ctx, channelId)
                .setSmallIcon(R.drawable.ic_library_music_background)
                .setContentTitle(track.title)
                .setContentText(contentText)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        )
        val intent = Intent(ctx.applicationContext, MainActivity::class.java)
        // Ensures that when the notification is clicked and the user is taken to the app,
        // clicking back leads out of the app.
        val stackBuilder = TaskStackBuilder.create(ctx)
        stackBuilder.addParentStack(MainActivity::class.java)
        stackBuilder.addNextIntent(intent)
        // 0 is requestCode, whatever the fuck that is
        val pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(pendingIntent)

        fun servicePendingIntent(action: String): PendingIntent {
            val mediaIntent = Intent(ctx, PimpMediaService::class.java).setAction(action)
            return PendingIntent.getService(ctx, 0, mediaIntent, 0)
        }

        fun addAction(action: String, drawable: Int, title: CharSequence) {
            val pi = servicePendingIntent(action)
            builder.addAction(drawable, title, pi)
        }

        if (playing) {
            addAction(PimpMediaService.PAUSE_ACTION, R.drawable.ic_pause_24px, "Pause")
        } else {
            addAction(PimpMediaService.RESUME_ACTION, R.drawable.ic_play_arrow_24px, "Play")
        }
        addAction(PimpMediaService.NEXT_ACTION, R.drawable.ic_skip_next_24px, "Next")
        addAction(PimpMediaService.CLOSE_ACTION, R.drawable.ic_close_black_24dp, "Close")
        // clicking "clear all" will have no effect
        builder.setOngoing(true)
        // the notification id allows us to update the notification later on
        notificationManager.notify(PIMP_NOTIFICATION_ID, builder.build())
    }

    fun cancel(): Unit = notificationManager.cancel(PIMP_NOTIFICATION_ID)

    // Does nothing on versions of Android earlier than O.
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        if (notificationManager.getNotificationChannel(channelId) == null) {
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
            notificationManager.createNotificationChannel(mChannel)
            Timber.d("New notification channel created.")
        } else {
            Timber.d("Existing notification channel reused.")
        }
    }

    private fun isAndroidOOrHigher(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
}
