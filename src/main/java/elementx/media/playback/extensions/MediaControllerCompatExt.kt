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

package elementx.media.playback.extensions

import android.content.Context
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import elementx.media.playback.models.MediaItem
import elementx.media.playback.services.playingQueue
import elementx.media.playback.services.queueIndex

internal fun MediaControllerCompat.clearQueue() {
    transportControls.stop()
    playingQueue = mutableListOf<MediaItem>()
    println("Queue cleared")
}

internal fun MediaControllerCompat.isPlaying(): Boolean =
    playbackState != null && playbackState.state == PlaybackStateCompat.STATE_PLAYING


internal fun MediaControllerCompat.setQueueList(context: Context, list: MutableList<MediaItem>, position: Int = 0) {
    transportControls.stop()
    playingQueue = list
    context.store("playingQueue", list)
    queueIndex = position
}

internal fun MediaControllerCompat.playFromQueueIndex(index: Int = 0) {
    queueIndex = index
    // Call prepare now so pressing play just works.
    transportControls.prepare()
    transportControls.play()
}

internal fun MediaControllerCompat.addMediaToPlayingQueue(context: Context, item: MediaItem) {
    playingQueue.add(item)
    context.store("playingQueue", playingQueue)
}

internal fun MediaControllerCompat.addMediaNextInQueue(context: Context, item: MediaItem) {
    playingQueue.add(queueIndex + 1, item)
    context.store("playingQueue", playingQueue)
}
