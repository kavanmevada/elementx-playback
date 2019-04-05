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
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.media.MediaBrowserServiceCompat
import elementx.media.playback.services.playingQueue


object MediaBrowser {

    private fun createBrowsableMediaItem(
        mediaId: String,
        folderName: String,
        iconUri: Uri?
    ): MediaBrowserCompat.MediaItem {
        val mediaDescriptionBuilder = MediaDescriptionCompat.Builder()
        mediaDescriptionBuilder.setMediaId(mediaId)
        mediaDescriptionBuilder.setTitle(folderName)
        iconUri?.let { mediaDescriptionBuilder.setIconUri(iconUri) }
        val extras = Bundle()
        extras.putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 1)
        extras.putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", if (mediaId == "albumsList") 2 else 1)
        mediaDescriptionBuilder.setExtras(extras)
        return MediaBrowserCompat.MediaItem(
            mediaDescriptionBuilder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        )
    }


    private fun Context.createMediaItem(
        mediaId: String,
        itemName: String,
        itemDes: String? = null,
        iconUri: Uri? = null
    ): MediaBrowserCompat.MediaItem {
        val mediaDescriptionBuilder = MediaDescriptionCompat.Builder()
        mediaDescriptionBuilder.setMediaId(mediaId)
        mediaDescriptionBuilder.setTitle(itemName)
        itemDes?.let { mediaDescriptionBuilder.setSubtitle(itemDes) }
        iconUri?.let {
            mediaDescriptionBuilder.setIconBitmap(
                MediaStore.Images.Media.getBitmap(
                    contentResolver,
                    iconUri
                )
            )
        }
        return MediaBrowserCompat.MediaItem(
            mediaDescriptionBuilder.build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        )
    }


    fun getCatalog(
        context: Context,
        parentId: String,
        result: MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        result.detach()
        when (parentId) {
            "root" -> {
                val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()
                mediaItems.add(
                    MediaBrowserCompat.MediaItem(
                        MediaDescriptionCompat.Builder()
                            .setMediaId("SHUFFLE_ALL")
                            .setTitle("Shuffle all songs")
                            //.setIconUri(Uri.parse("android.resource://${context.packageName}/drawable/${R.drawable.ic_shuffle_24px}"))
                            .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                    )
                )
                mediaItems.add(
                    createBrowsableMediaItem(
                        "albumsList", "Albums", null
                        /* Uri.parse("android.resource://${context.packageName}/drawable/${R.drawable.ic_album_24px}" */
                    )
                )
                mediaItems.add(
                    createBrowsableMediaItem(
                        "artistList", "Artists", null
                    )
                )
                mediaItems.add(
                    createBrowsableMediaItem(
                        "genreList", "Genre", null
                    )
                )
                mediaItems.add(
                    createBrowsableMediaItem(
                        "playlistList", "Playlist", null
                    )
                )


                result.sendResult(mediaItems)
            }
            "albumsList" -> {
                context.retrieveAlbumAsync {
                    result.sendResult(it.map {
                        context.createMediaItem("ALBM_${it.albumId}", it.title, it.artist, Uri.parse(it.coverUri))
                    })
                }
            }
            "artistList" -> {
                context.retrieveArtistAsync {
                    result.sendResult(it.map {
                        context.createMediaItem(
                            "ARTS_${it.artistId}",
                            it.name,
                            "${it.albumCount} Albums and ${it.trackCount} Songs"
                        )
                    })
                }
            }
            "playlistList" -> {
                context.retrievePlaylistAsync {
                    result.sendResult(it.map {
                        context.createMediaItem("PLST_${it.playlistId}", it.name, it.dateModified)
                    })
                }
            }
            "genreList" -> {
                context.retrieveGenreAsync {
                    result.sendResult(it.map {
                        context.createMediaItem("GNRE_${it.genreId}", it.name)
                    })
                }
            }
            "playingQueue" -> {
                result.sendResult(playingQueue.mapIndexed { i, it ->
                    context.createMediaItem("QUEU_${i}", it.title, it.artist)
                })
            }
        }
    }
}