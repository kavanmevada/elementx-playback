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

package elementx.media.playback.models

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import java.io.Serializable

class MediaItem(
    var mediaId: String, var title: String,
    var artist: String, var album: String,
    var duration: Long, var trackNo: Long,
    var mediaUri: String, var coverUri: String?
) : Serializable

fun MediaItem.metadataCompat(context: Context): MediaMetadataCompat? {
    return MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
        .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNo)
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, mediaUri)
        .putBitmap(
            MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
            MediaStore.Images.Media.getBitmap(
                context.contentResolver,
                Uri.parse(coverUri)
            )
        )
        .build()
}