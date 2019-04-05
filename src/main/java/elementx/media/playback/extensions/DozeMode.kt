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
import android.util.Log
import android.util.LruCache
import java.io.*

private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
private val lru: LruCache<String, Any> = LruCache(maxMemory)

fun Context.store(id: String, obj: Any) {
    try {
        //lru.put(id, obj)
        val fos = FileOutputStream(File(filesDir, id))
        val os = ObjectOutputStream(fos)
        os.writeObject(obj)
        os.close()
    } catch (e: Exception) {
        Log.e("DozeMode", "Error saving state !!")
    }
}

fun <T> Context.restore(id: String, func: ((T) -> Unit)): T? {
    var obj: T? = null

    //val memoryCache = lru.get(id) as T?
    //if (memoryCache!=null) return memoryCache

    val driveCache = File(filesDir, id)
    if (driveCache.exists()) {
        try {
            val fis = FileInputStream(driveCache)
            val stream = ObjectInputStream(fis)
            obj = stream.readObject() as T?
            stream.close()

            if (obj != null) func(obj)
        } catch (e: Exception) {
            Log.e("DozeMode", "Error retrieving state !!")
        }
    }
    return obj
}

fun Context.deleteCache() = try {
    cacheDir.delete()
} catch (e: Exception) {
}