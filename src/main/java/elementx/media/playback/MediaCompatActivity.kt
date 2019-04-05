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

package elementx.media.playback

import android.animation.ValueAnimator
import android.content.ComponentName
import android.os.RemoteException
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import elementx.media.playback.extensions.restore
import elementx.media.playback.extensions.setQueueList
import elementx.media.playback.models.MediaItem
import elementx.media.playback.services.MusicService
import elementx.media.playback.services.serviceInStartedState

open class MediaCompatActivity : AppCompatActivity() {
    private lateinit var mediaBrowser: MediaBrowserCompat
    internal lateinit var mediaController: MediaControllerCompat
    internal lateinit var mediaSeekBar: SeekBar

    open fun OnMetadataChanged(metadata: MediaMetadataCompat?) {}
    open fun OnPlaybackStateChanged(state: PlaybackStateCompat?) {}


    override fun onStart() {
        super.onStart()
        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, MusicService::class.java),
            connectionCallback, null
        )
        mediaBrowser.connect()
    }

    override fun onStop() {
        super.onStop()
        if (mediaBrowser.isConnected) mediaBrowser.unsubscribe("root")
        mediaBrowser.disconnect()
    }

    private var progressAnimator: ValueAnimator? = null

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)


            //SeekBar implementation

            // If there's an ongoing animation, stop it now.
            if (progressAnimator != null) {
                progressAnimator?.cancel()
                progressAnimator = null
            }

            val progress = state?.position?.toInt() ?: 0

            mediaSeekBar.progress = progress
            // If the media is playing then the seekbar should follow it, and the easiest
            // way to do that is to create a ValueAnimator to update it so the bar reaches
            // the end of the media the same time as playback gets there (or close enough).
            if (state != null && state.state == PlaybackStateCompat.STATE_PLAYING) {
                val timeToEnd = ((mediaSeekBar.max - progress) / state.playbackSpeed).toInt()

                if (timeToEnd >= 0) {
                    progressAnimator = ValueAnimator.ofInt(progress, mediaSeekBar.max)
                        .setDuration(timeToEnd.toLong())
                    progressAnimator!!.interpolator = LinearInterpolator()
                    progressAnimator?.addUpdateListener {
                        mediaSeekBar.progress = it.animatedValue as Int
                    }
                    progressAnimator!!.start()
                }
            }

            OnPlaybackStateChanged(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            mediaSeekBar.max = (metadata?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) ?: 100).toInt()

            if (metadata == null) return

            OnMetadataChanged(metadata)
        }
    }


    private val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            try {
                mediaController = MediaControllerCompat(this@MediaCompatActivity, mediaBrowser.sessionToken)
                MediaControllerCompat.setMediaController(this@MediaCompatActivity, mediaController)
                mediaController.registerCallback(controllerCallback)

                mediaSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener)

                if (!serviceInStartedState) {
                    restore<MutableList<MediaItem>>("playingQueue") {
                        mediaController.setQueueList(applicationContext, it, 0)
                        mediaController.transportControls.prepare()

                        controllerCallback.onMetadataChanged(mediaController.metadata)
                        controllerCallback.onPlaybackStateChanged(mediaController.playbackState)
                    }
                } else {
                    controllerCallback.onMetadataChanged(mediaController.metadata)
                    controllerCallback.onPlaybackStateChanged(mediaController.playbackState)
                }

            } catch (e: RemoteException) {
                throw RuntimeException(e)
            }
        }
    }

    private val onSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            progressAnimator?.cancel()
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            mediaController.transportControls.seekTo(seekBar.progress.toLong())
            updateSeekBar(mediaController.playbackState, seekBar.progress)
        }
    }


    fun updateSeekBar(state: PlaybackStateCompat?, progress: Int) {
        mediaSeekBar.progress = progress
        // If the media is playing then the seekbar should follow it, and the easiest
        // way to do that is to create a ValueAnimator to update it so the bar reaches
        // the end of the media the same time as playback gets there (or close enough).
        if (state != null && state.state == PlaybackStateCompat.STATE_PLAYING) {
            val timeToEnd = ((mediaSeekBar.max - progress) / state.playbackSpeed).toInt()

            if (timeToEnd >= 0) {
                progressAnimator = ValueAnimator.ofInt(progress, mediaSeekBar.max)
                    .setDuration(timeToEnd.toLong())
                progressAnimator!!.interpolator = LinearInterpolator()
                progressAnimator?.addUpdateListener {
                    mediaSeekBar.progress = it.animatedValue as Int
                }
                progressAnimator!!.start()
            }
        }
    }
}