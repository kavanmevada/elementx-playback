/*
 * Copyright (C) 2019  Kavan Mevada
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package elementx.media.playback.notification

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import elementx.media.playback.R
import elementx.media.playback.models.MediaItem
import elementx.media.playback.models.metadataCompat
import elementx.media.playback.services.MusicService


/**
 * Keeps track of a notification and updates it automatically for a given MediaSession. This is
 * required so that the music service don't get killed during playback.
 */
class MediaNotification(private val service: MusicService) {

    private val pauseAction: NotificationCompat.Action = NotificationCompat.Action(
        R.drawable.ic_twotone_pause_24px,
        "Pause",
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_PAUSE
        )
    )


    private val nextAction: NotificationCompat.Action = NotificationCompat.Action(
        R.drawable.ic_twotone_skip_next_24px,
        "Next",
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        )
    )


    private val prevAction: NotificationCompat.Action = NotificationCompat.Action(
        R.drawable.ic_twotone_skip_previous_24px,
        "Previous",
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        )
    )

    val notificationManager: NotificationManager =
        service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val playAction: NotificationCompat.Action = NotificationCompat.Action(
        R.drawable.ic_twotone_play_arrow_24px,
        "Play",
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_PLAY
        )
    )

    private val isAndroidOOrHigher = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O


    init {
        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        notificationManager.cancelAll()
    }

    fun getNotification(
        metadata: MediaItem,
        state: PlaybackStateCompat,
        token: MediaSessionCompat.Token
    ): Notification {
        val isPlaying = state.state == PlaybackStateCompat.STATE_PLAYING
        val builder = buildNotification(state, token, isPlaying, metadata)
        return builder.build()
    }


    private fun buildNotification(
        state: PlaybackStateCompat,
        token: MediaSessionCompat.Token,
        isPlaying: Boolean,
        description: MediaItem
    ): NotificationCompat.Builder {

        // Create the (mandatory) notification channel when running on Android Oreo.
        createChannel()

        val builder = NotificationCompat.Builder(
            service,
            CHANNEL_ID
        )
        builder.setStyle(
            MediaStyle()
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
            .setColor(0xe91e63)
            .setSmallIcon(R.drawable.ic_twotone_music_note_24px)
            // Pending intent that is fired when user clicks on notification.
//            .setContentIntent(createContentIntent())
            // Title - Usually Song name.
            .setContentTitle(description.title)
            // Subtitle - Usually Artist name.
            .setContentText(description.artist)
            .setLargeIcon(description.metadataCompat(service)?.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART))
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
        if (state.actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L) {
            builder.addAction(prevAction)
        }

        builder.addAction(if (isPlaying) pauseAction else playAction)

        // If skip to prev action is enabled.
        if (state.actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L) {
            builder.addAction(nextAction)
        }

        return builder
    }

    // Does nothing on versions of Android earlier than O.

    @TargetApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            // The user-visible name of the channel.
            val name = "MediaSession"
            // The user-visible description of the channel.
            val description = "MediaSession and MediaPlayer"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            // Configure the notification channel.
            channel.description = description
            channel.enableLights(true)
            // Sets the notification light color for notifications posted to this
            // channel, if the device supports this feature.
            channel.lightColor = Color.RED
            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "createChannel: New channel created")
        } else {
            Log.d(TAG, "createChannel: Existing channel reused")
        }
    }

//    private fun createContentIntent(): PendingIntent {
//        val openUI = Intent(service, MainActivity::class.java)
//        openUI.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
//        return PendingIntent.getActivity(
//            service,
//            REQUEST_CODE, openUI, PendingIntent.FLAG_CANCEL_CURRENT
//        )
//    }

    companion object {

        val NOTIFICATION_ID = 412

        private val TAG = MediaNotification::class.java.simpleName
        private val CHANNEL_ID = "media_playback_channel"
        private val REQUEST_CODE = 501
    }

}