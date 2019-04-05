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

package elementx.media.playback.mediacatalog

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import elementx.media.playback.models.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


val mediaItemProjection = arrayOf(
    MediaStore.Audio.Media._ID,
    MediaStore.Audio.Media.TITLE,
    MediaStore.Audio.Media.ARTIST,
    MediaStore.Audio.Media.ALBUM,
    MediaStore.Audio.Media.DURATION,
    MediaStore.Audio.Media.TRACK,
    MediaStore.Audio.Media.DATA,
    MediaStore.Audio.Media.ALBUM_ID,
    MediaStore.Audio.Media.ARTIST_ID
)

val Cursor.mediaItem
    get() = MediaItem(
        getString(0),
        getString(1),
        getString(2),
        getString(3),
        getLong(4),
        getLong(5),
        getString(6),
        "content://media/external/audio/albumart/${getLong(7)}"
    )

val albumItemProjection = arrayOf(
    MediaStore.Audio.Albums._ID,
    MediaStore.Audio.Albums.ALBUM,
    MediaStore.Audio.Albums.ARTIST,
    MediaStore.Audio.Albums.NUMBER_OF_SONGS,
    MediaStore.Audio.Albums.LAST_YEAR,
    MediaStore.Audio.Albums.ALBUM_ART
)

val Cursor.albumItem
    get() = AlbumItem(
        getString(0),
        getString(1),
        getString(2),
        getInt(3),
        getString(4) ?: "",
        "content://media/external/audio/albumart/${getLong(0)}"
    )

val artistItemProjection = arrayOf(
    MediaStore.Audio.Artists._ID,
    MediaStore.Audio.Artists.ARTIST,
    MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
    MediaStore.Audio.Artists.NUMBER_OF_TRACKS
)

val Cursor.artistItem
    get() = ArtistItem(
        getString(0),
        getString(1),
        getInt(2),
        getInt(3)
    )

val playlistItemProjection = arrayOf(
    MediaStore.Audio.Playlists._ID,
    MediaStore.Audio.Playlists.NAME,
    MediaStore.Audio.Playlists.DATE_ADDED,
    MediaStore.Audio.Playlists.DATE_MODIFIED,
    MediaStore.Audio.Playlists.DATA
)

val Cursor.playlistItem
    get() = PlaylistItem(
        getString(0),
        getString(1),
        getString(2),
        getString(3) ?: "",
        getString(4)
    )

val genreItemProjection = arrayOf(
    MediaStore.Audio.Genres._ID,
    MediaStore.Audio.Genres.NAME
)

val Cursor.genreItem
    get() = GenreItem(
        getString(0),
        getString(1)
    )


fun Context.itemCursor(uri: Uri, projection: Array<String>, selection: Int, vararg mediaIds: String = emptyArray()) =
    contentResolver.query(
        uri,
        projection,
        if (!mediaIds.isEmpty()) {
            projection[selection] + "=?"
        } else null,
        mediaIds, null
    )


fun Context.retrieveMediaAsync(
    vararg mediaIds: String = emptyArray(),
    onReady: (catalog: MutableList<MediaItem>) -> Unit
) =
    GlobalScope.launch {
        val retrievedList = mutableListOf<MediaItem>()

        itemCursor(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            mediaItemProjection, 0,
            *mediaIds
        )?.let {
            while (it.moveToNext()) {
                retrievedList.add(it.mediaItem)
            }
            it.close()
        }

        onReady(retrievedList)
    }

fun Context.retrieveAlbumAsync(
    vararg albumIds: String = emptyArray(),
    onReady: (catalog: MutableList<AlbumItem>) -> Unit
) =
    GlobalScope.launch {
        val retrievedList = mutableListOf<AlbumItem>()

        itemCursor(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            albumItemProjection, 0,
            *albumIds
        )?.let {
            while (it.moveToNext()) {
                retrievedList.add(it.albumItem)
            }
            it.close()
        }

        onReady(retrievedList)
    }

fun Context.retrieveArtistAsync(
    vararg artistIds: String = emptyArray(),
    onReady: (catalog: MutableList<ArtistItem>) -> Unit
) =
    GlobalScope.launch {
        val retrievedList = mutableListOf<ArtistItem>()

        itemCursor(
            MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
            artistItemProjection, 0,
            *artistIds
        )?.let {
            while (it.moveToNext()) {
                retrievedList.add(it.artistItem)
            }
            it.close()
        }

        onReady(retrievedList)
    }

fun Context.retrievePlaylistAsync(
    vararg playlistIds: String = emptyArray(),
    onReady: (catalog: MutableList<PlaylistItem>) -> Unit
) =
    GlobalScope.launch {
        val retrievedList = mutableListOf<PlaylistItem>()

        itemCursor(
            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
            playlistItemProjection, 0,
            *playlistIds
        )?.let {
            while (it.moveToNext()) {
                retrievedList.add(it.playlistItem)
            }
            it.close()
        }

        onReady(retrievedList)
    }

fun Context.retrieveGenreAsync(
    vararg genreIds: String = emptyArray(),
    onReady: (catalog: MutableList<GenreItem>) -> Unit
) =
    GlobalScope.launch {
        val retrievedList = mutableListOf<GenreItem>()

        itemCursor(
            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
            genreItemProjection, 0,
            *genreIds
        )?.let {
            while (it.moveToNext()) {
                retrievedList.add(it.genreItem)
            }
            it.close()
        }

        onReady(retrievedList)
    }


fun Context.retrieveMediaFromAlbumAsync(albumId: String, onReady: (catalog: MutableList<MediaItem>) -> Unit) =
    GlobalScope.launch {
        val retrievedList = mutableListOf<MediaItem>()

        itemCursor(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            mediaItemProjection, 7,
            albumId
        )?.let {
            while (it.moveToNext()) {
                retrievedList.add(it.mediaItem)
            }
            it.close()
        }

        onReady(retrievedList)
    }

fun Context.retrieveMediaFromArtistAsync(artistId: String, onReady: (catalog: MutableList<MediaItem>) -> Unit) =
    GlobalScope.launch {
        val retrievedList = mutableListOf<MediaItem>()

        itemCursor(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            mediaItemProjection, 8,
            artistId
        )?.let {
            while (it.moveToNext()) {
                retrievedList.add(it.mediaItem)
            }
            it.close()
        }

        onReady(retrievedList)
    }

fun Context.retrieveMediaFromPlaylistAsync(playlistId: String, onReady: (catalog: MutableList<MediaItem>) -> Unit) =
    GlobalScope.launch {
        val retrievedList = mutableListOf<MediaItem>()

        itemCursor(
            MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId.toLong()),
            mediaItemProjection, 0
        )?.let {
            while (it.moveToNext()) {
                retrievedList.add(it.mediaItem)
            }
            it.close()
        }

        onReady(retrievedList)
    }

fun Context.retrieveMediaFromGenreAsync(genreId: String, onReady: (catalog: MutableList<MediaItem>) -> Unit) =
    GlobalScope.launch {
        val retrievedList = mutableListOf<MediaItem>()

        itemCursor(
            MediaStore.Audio.Genres.Members.getContentUri("external", genreId.toLong()),
            mediaItemProjection, 0
        )?.let {
            while (it.moveToNext()) {
                retrievedList.add(it.mediaItem)
            }
            it.close()
        }
        onReady(retrievedList)
    }