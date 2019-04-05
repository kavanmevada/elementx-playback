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

package elementx.media.playback.services

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import elementx.media.playback.extensions.setQueueList
import elementx.media.playback.mediacatalog.*
import elementx.media.playback.models.MediaItem
import elementx.media.playback.models.metadataCompat
import elementx.media.playback.notification.MediaNotification


internal var playingQueue = mutableListOf<MediaItem>()
internal var queueIndex = -1
internal var serviceInStartedState: Boolean = false

class MusicService : MediaBrowserServiceCompat() {

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        return BrowserRoot("root", rootHints)
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        MediaBrowser.getCatalog(baseContext, parentId, result)
    }

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaNotification: MediaNotification
    private lateinit var audioManager: AudioManager


    private var playOnAudioFocus = false

    val isPlaying get() = mediaPlayer != null && mediaPlayer!!.isPlaying

    private var mediaPlayer: MediaPlayer? = null
    private var playbackState: Int = 0

    // Work-around for a MediaPlayer bug related to the behavior of MediaPlayer.seekTo()
    // while not playing.
    private var seekWhileNotPlaying = -1

    private val availableActions: Long
        get() {
            var actions = (PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                    or PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                    or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
            actions = when (playbackState) {
                PlaybackStateCompat.STATE_STOPPED -> actions or (PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE)
                PlaybackStateCompat.STATE_PLAYING -> actions or (PlaybackStateCompat.ACTION_STOP
                        or PlaybackStateCompat.ACTION_PAUSE
                        or PlaybackStateCompat.ACTION_SEEK_TO)
                PlaybackStateCompat.STATE_PAUSED -> actions or (PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_STOP)
                else -> actions or (PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PLAY_PAUSE
                        or PlaybackStateCompat.ACTION_STOP
                        or PlaybackStateCompat.ACTION_PAUSE)
            }
            return actions
        }


    private var mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPrepare() {

            queueIndex = when {
                queueIndex == -1 -> 0
                playingQueue.isEmpty() -> -1
                else -> queueIndex
            }

            if (queueIndex < 0 && playingQueue.isEmpty()) {
                Log.d(TAG, "Nothing to play.")
                return
            }

            mediaSession.setQueue(mediaQueue)
            mediaSession.setMetadata(playingQueue[queueIndex].metadataCompat(baseContext))

            if (!mediaSession.isActive) {
                mediaSession.isActive = true
            }
        }

        override fun onSkipToQueueItem(id: Long) {
            queueIndex = id.toInt()
            onPrepare()
            onPlay()
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            val id = mediaId?.split("_")
            queueIndex = 0

            println(id?.get(0))
            when (id?.get(0)) {
                "ALBM" -> retrieveMediaFromAlbumAsync(id[1]) {
                    mediaSession.controller.setQueueList(baseContext, it); onPlay()
                }
                "ARTS" -> retrieveMediaFromArtistAsync(id[1]) {
                    mediaSession.controller.setQueueList(baseContext, it); onPlay()
                }
                "PLST" -> retrieveMediaFromPlaylistAsync(id[1]) {
                    mediaSession.controller.setQueueList(baseContext, it); onPlay()
                }
                "GNRE" -> retrieveMediaFromGenreAsync(id[1]) {
                    mediaSession.controller.setQueueList(baseContext, it); onPlay()
                }
                "QUEU" -> {
                    queueIndex = id[1].toInt(); onPlay()
                }
                "SHUFFLE_ALL" -> retrieveMediaAsync {
                    it.shuffle()
                    mediaSession.controller.setQueueList(baseContext, it); onPlay()
                }
            }
        }

        override fun onPlay() {
            if (!playingQueue.isEmpty()) {
                onPrepare()

                if ((playbackState == PlaybackStateCompat.STATE_PAUSED /* mediaChanged */)) {
                    play()
                    return
                }

                release()

                // Initialize mediaPlayer
                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer()
                    mediaPlayer!!.setOnCompletionListener {
                        //mPlaybackInfoListener.onPlaybackCompleted()
                        // The media player finished playing the current song, so we go ahead
                        // and start the next.
                        when (mediaSession.controller.repeatMode) {
                            PlaybackStateCompat.REPEAT_MODE_ONE -> onPlay()
                            PlaybackStateCompat.REPEAT_MODE_ALL -> onSkipToNext()
                            PlaybackStateCompat.REPEAT_MODE_NONE -> {
                                setNewState(PlaybackStateCompat.STATE_PAUSED)
                                onStop()
                            }
                        }
                    }
                }


                val mediaUri = playingQueue[queueIndex].mediaUri
                try {
                    mediaPlayer!!.setDataSource(baseContext, Uri.parse(mediaUri))
                    mediaPlayer!!.prepare()
                } catch (e: Exception) {
                    throw RuntimeException("Failed to set file Uri: $mediaUri", e)
                }

                play()

                Log.d(TAG, "onPlayFromMediaId: MediaSession active")

            } else Log.d(TAG, "Playing Queue is empty!")

        }

        override fun onPause() = pause()

        override fun onStop() {
            stop()
            mediaSession.isActive = false
        }

        override fun onSkipToNext() {
            if (!playingQueue.isEmpty()) {
                queueIndex = ++queueIndex % playingQueue.size
                playbackState = PlaybackStateCompat.STATE_SKIPPING_TO_NEXT
                onPlay()
            } else Log.d(TAG, "Playing Queue is empty!")
        }

        override fun onSkipToPrevious() {
            if (!playingQueue.isEmpty()) {
                queueIndex = if (queueIndex > 0) queueIndex - 1 else playingQueue.size - 1
                playbackState = PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS
                onPlay()
            } else Log.d(TAG, "Playing Queue is empty!")
        }

        override fun onSeekTo(pos: Long) {
            if (mediaPlayer != null) {
                if (!mediaPlayer!!.isPlaying) {
                    seekWhileNotPlaying = pos.toInt()
                }
                mediaPlayer!!.seekTo(pos.toInt())

                // Set the state (to the current state) because the position changed and should
                // be reported to clients.
                setNewState(playbackState)
            }
        }

        override fun onCustomAction(action: String?, extras: Bundle?) {
            when (action) {
                "thumbs_up" -> {
                    //val mediaId = playingQueue[queueIndex].mediaId
                    //setFavorite(mediaId, !isFavorite(mediaId))
                    //setNewState(playbackState)
                }
            }
        }


        override fun onSetRepeatMode(repeatMode: Int) {
            mediaSession.setRepeatMode(repeatMode)
        }

        override fun onSetShuffleMode(shuffleMode: Int) {
            if (shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL) {
                mediaSession.setShuffleMode(shuffleMode)
                playingQueue.shuffle()
                queueIndex = 0
                onPlay()
            }
        }
    }

    fun play() {
        val result = audioManager.requestAudioFocus(
            audioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //registerAudioNoisyReceiver here
            if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                mediaPlayer!!.start()
                setNewState(PlaybackStateCompat.STATE_PLAYING)
            }
        }
    }


    fun pause() {
        if (!playOnAudioFocus) {
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }

        //unregisterAudioNoisyReceiver here
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            mediaPlayer!!.pause()
            setNewState(PlaybackStateCompat.STATE_PAUSED)
        }
    }

    fun stop() {
        audioManager.abandonAudioFocus(audioFocusChangeListener)
        //unregisterAudioNoisyReceiver here

        // Regardless of whether or not the MediaPlayer has been created / started, the state must
        // be updated, so that MediaNotification can take down the notification.
        setNewState(PlaybackStateCompat.STATE_STOPPED)
        release()
    }


    override fun onCreate() {
        super.onCreate()

        // Create a new MediaSession.
        mediaSession = MediaSessionCompat(this, "MusicService")
        mediaSession.setCallback(mediaSessionCallback)
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)
        sessionToken = mediaSession.sessionToken

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager


        mediaNotification = MediaNotification(this)
        Log.d(TAG, "onCreate: MusicService creating MediaSession, and MediaNotification")
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }


    override fun onDestroy() {
        stop()
        mediaSession.release()
        Log.d(TAG, "onDestroy: MediaPlayerAdapter stopped, and MediaSession released")
    }

    private fun release() {
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }

    private fun moveServiceToStartedState(state: PlaybackStateCompat) {
        val notification = mediaNotification.getNotification(playingQueue[queueIndex], state, sessionToken!!)

        if (!serviceInStartedState) {
            ContextCompat.startForegroundService(
                this@MusicService,
                Intent(this@MusicService, MusicService::class.java)
            )
            serviceInStartedState = true
        }

        startForeground(MediaNotification.NOTIFICATION_ID, notification)
    }

    private fun updateNotificationForPause(state: PlaybackStateCompat) {
        stopForeground(false)
        val notification = mediaNotification.getNotification(
            playingQueue[queueIndex], state, sessionToken!!
        )
        mediaNotification.notificationManager
            .notify(MediaNotification.NOTIFICATION_ID, notification)
    }

    private fun moveServiceOutOfStartedState() {
        stopForeground(true)
        stopSelf()
        serviceInStartedState = false
    }


    // This is the main reducer for the player state machine.
    private fun setNewState(@PlaybackStateCompat.State newPlayerState: Int) {
        playbackState = newPlayerState

        // Work around for MediaPlayer.getCurrentPosition() when it changes while not playing.
        val reportPosition: Long
        if (seekWhileNotPlaying >= 0) {
            reportPosition = seekWhileNotPlaying.toLong()

            if (playbackState == PlaybackStateCompat.STATE_PLAYING) {
                seekWhileNotPlaying = -1
            }
        } else {
            reportPosition = (if (mediaPlayer == null) 0 else mediaPlayer!!.currentPosition).toLong()
        }

        val stateBuilder = PlaybackStateCompat.Builder()
        stateBuilder.setActions(availableActions)
        stateBuilder.setState(
            playbackState,
            reportPosition,
            1.0f,
            SystemClock.elapsedRealtime()
        )

        val state = stateBuilder.build()

        // Report the state to the MediaSession.
        mediaSession.setPlaybackState(state)

        // Manage the started state of this service.
        when (state.state) {
            PlaybackStateCompat.STATE_PLAYING -> moveServiceToStartedState(state)
            PlaybackStateCompat.STATE_PAUSED -> updateNotificationForPause(state)
            PlaybackStateCompat.STATE_STOPPED -> moveServiceOutOfStartedState()
        }
    }

    /**
     * Helper class for managing audio focus related tasks.
     */
    private val audioFocusChangeListener = object : AudioManager.OnAudioFocusChangeListener {
        override fun onAudioFocusChange(focusChange: Int) {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if (playOnAudioFocus && !isPlaying) {
                        play()
                    } else if (isPlaying) {
                        if (mediaPlayer != null) {
                            mediaPlayer!!.setVolume(
                                MEDIA_VOLUME_DEFAULT,
                                MEDIA_VOLUME_DEFAULT
                            )
                        }
                    }
                    playOnAudioFocus = false
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> if (mediaPlayer != null) {
                    mediaPlayer!!.setVolume(
                        MEDIA_VOLUME_DUCK,
                        MEDIA_VOLUME_DUCK
                    )
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> if (isPlaying) {
                    playOnAudioFocus = true
                    pause()
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    audioManager.abandonAudioFocus(this)
                    playOnAudioFocus = false
                    pause()
                }
            }
        }
    }


    companion object {
        private val TAG = MusicService::class.java.simpleName

        private const val MEDIA_VOLUME_DEFAULT = 1.0f
        private const val MEDIA_VOLUME_DUCK = 0.2f
    }


    // Create Playing Queue from playingQueueMetadata
    private val mediaQueue: List<MediaSessionCompat.QueueItem>
        get() {
            val queue = mutableListOf<MediaSessionCompat.QueueItem>()
            playingQueue.forEachIndexed { index, metadata ->
                val queueItem = MediaSessionCompat
                    .QueueItem(metadata.metadataCompat(baseContext)?.description, index.toLong())
                queue.add(queueItem)
            }
            return queue
        }

}

