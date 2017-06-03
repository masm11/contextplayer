/* Context Player - Audio Player with Contexts
    Copyright (C) 2016 Yuuki Harano

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package jp.ddo.masm11.contextplayer.util

import android.media.MediaMetadataRetriever

import java.io.FileInputStream
import java.io.BufferedInputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset;
import java.util.concurrent.locks.ReentrantLock

import jp.ddo.masm11.logger.Log

class Metadata(private val path: String) {
    var title: String? = null
        private set
    var artist: String? = null
        private set

    fun extract(): Boolean {
        if (tryExtractOgg())
            return true
        if (tryExtractID3v2())
            return true
        if (tryExtractOther())
            return true

        return false
    }

    /* MediaMetadataRetriever に任せると曲名が化ける場合があるので、
     * 自前で取り出す。
     * 参考:
     *  http://www.xiph.org/vorbis/doc/Vorbis_I_spec.html
     */
    private fun tryExtractOgg(): Boolean {
	try {
	    return BufferedInputStream(FileInputStream(path)).use<BufferedInputStream, Boolean> {
		if (it.read() != 'O'.toInt())
		    return false
		if (it.read() != 'g'.toInt())
		    return false
		if (it.read() != 'g'.toInt())
		    return false
		if (it.read() != 'S'.toInt())
		    return false

		val test = intArrayOf(0x03, 'v'.toInt(), 'o'.toInt(), 'r'.toInt(), 'b'.toInt(), 'i'.toInt(), 's'.toInt())
		var sample = IntArray(test.size)
		var isVorbis = false
		for (i in 0 until 0x10000) {
		    val b = it.read()
		    if (b == -1)
		        return false
		    sample = sample.copyOfRange(1, test.size).plus(b)
		    if (sample.contentEquals(test)) {
			isVorbis = true
			break
		    }
		}
		if (!isVorbis)
		    return false

		val vendorLength = readLEInt(it)
		if (vendorLength == null)
		    return false

		for (i in 0 until vendorLength) {
		    if (it.read() == -1)
			return false
		}

		val numComments = readLEInt(it)
		if (numComments == null)
		    return false

		for (i in 0 until numComments) {
		    val length = readLEInt(it)
		    if (length == null)
			return false

		    val buf = ByteArray(length)
		    if (it.read(buf) != length)
			return false
		    val str = buf.toString(Charset.forName("UTF-8"))
		    val eq = str.indexOf('=')
		    if (eq == -1)
			continue
		    val key = str.substring(0, eq)
		    val value = str.substring(eq + 1)
		    if (key.equals("TITLE", ignoreCase = true))
			title = value
		    if (key.equals("ARTIST", ignoreCase = true))
			artist = value
		}

		run {
		    val b = it.read()
		    if (b == -1)
			return false
		    if (b and 0x01 != 1)
			return false
		}

		return true
	    }
	} catch (e: Exception) {
            Log.e("exception", e)
            return false
	}
    }

    private fun tryExtractID3v2(): Boolean {
        try {
            return BufferedInputStream(FileInputStream(path)).use<BufferedInputStream, Boolean> {
		if (it.read() != 'I'.toInt())
		    return false
		if (it.read() != 'D'.toInt())
		    return false
		if (it.read() != '3'.toInt())
		    return false
		Log.d("ID3 found.")

		val majorVer = it.read()
		val minorVer = it.read()
		if (minorVer == -1)
		    return false
		Log.d("major/minorVer: %d, %d.", majorVer, minorVer)

		val flags = it.read()
		if (flags == -1)
		    return false
		Log.d("flags=%d", flags)

		val size = readSyncsafeInt(it)
		if (size == null)
		    return false
		Log.d("size=%d\n", size)

		// 拡張ヘッダがあるなら読み捨てる。
		if (flags and (1 shl 6) != 0) {
		    Log.d("ext header exists.")
		    val sz: Int?
		    if (majorVer < 4)
			sz = readInt(it)
		    else
			sz = readSyncsafeInt(it)
		    if (sz == null)
			return false
		    for (i in 0 until sz - 4)
			it.read()
		}

		Log.d("Now, frames.")
		while (true) {
		    val frameId = ByteArray(4)
		    frameId[0] = it.read().toByte()
		    frameId[1] = it.read().toByte()
		    frameId[2] = it.read().toByte()
		    if (!isValidFrameIdChar(frameId[0]))
			break
		    if (!isValidFrameIdChar(frameId[1]))
			break
		    if (!isValidFrameIdChar(frameId[2]))
			break
		    if (majorVer >= 3) {
			frameId[3] = it.read().toByte()
			if (!isValidFrameIdChar(frameId[3]))
			    break
		    }
		    Log.d("frameId: %d, %d, %d, %d.", frameId[0], frameId[1], frameId[2], frameId[3])

		    val sz: Int?
		    when (majorVer) {
			0, 1, 2 -> sz = readInt3(it)
			3 -> sz = readInt(it)
			// 4,
			else -> {
			    sz = readSyncsafeInt(it)
			}
		    }
		    if (sz == null)
		        return false
		    Log.d("sz=%d.", sz)

		    // flag を読み捨てる。
		    if (majorVer >= 3) {
			it.read()
			it.read()
		    }

		    val data = ByteArray(sz)
		    if (it.read(data) != sz)
		        return false

		    var isTitle = false
		    var isArtist = false
		    if (testFrameId(frameId, BYTE_ARRAY_TT2, BYTE_ARRAY_TIT2)) {
			Log.d("is title.")
			isTitle = true
		    } else if (testFrameId(frameId, BYTE_ARRAY_TP1, BYTE_ARRAY_TPE1)) {
			Log.d("is artist.")
			isArtist = true
		    }

		    if (isTitle || isArtist) {
			var encoding: String? = null
			val start: Int

			val sb = StringBuilder()
			for (b in data)
			    sb.append(String.format(" %02x", b))
			Log.d("data:%s", sb.toString())

			when (data[0].toInt()) {
			    0 -> {    // ISO-8859-1
				encoding = "ISO-8859-1"
				start = 1
			    }

			    1 -> {    // UTF-16 with BOM
				when {
				    data.size < 3 -> {
					start = -1
				    }
				    data[1] == 0xfe.toByte() && data[2] == 0xff.toByte() -> {
					encoding = "UTF-16BE"
					start = 3
				    }
				    data[1] == 0xff.toByte() && data[2] == 0xfe.toByte() -> {
					encoding = "UTF-16LE"
					start = 3
				    }
				    else -> {
					start = -1
				    }
				}
			    }

			    2 -> {    // UTF-16BE without BOM
				if (majorVer < 4) {
				    start = -1
				} else {
				    encoding = "UTF-16BE"
				    start = 1
				}
			    }

			    3 -> {    // UTF-8
				if (majorVer < 4) {
				    start = -1
				} else {
				    encoding = "UTF-8"
				    start = 1
				}
			    }

			    else -> start = -1
			}
			if (start < 0 || encoding == null)
			    continue

			Log.d("encoding=%s", encoding)
			Log.d("start=%d", start)

			// バイト列のバイト数。
			// terminator(0x00) は必須ではないが、あればそこまで。
			var len = 0
			if (!encoding.startsWith("UTF-16")) {
			    while (true) {
				if (start + len >= data.size)
				    break
				if (data[start + len].toInt() == 0)
				    break
				len++
			    }
			} else {
			    while (true) {
				if (start + len + 1 >= data.size)
				    break
				if (data[start + len] == 0.toByte() && data[start + len + 1] == 0.toByte())
				    break
				len += 2
			    }
			}
			Log.d("len=%d", len)

			try {
			    val str = String(data, start, len, Charset.forName(encoding))
			    Log.d("str=%s", str)
			    if (isTitle)
				title = str
			    if (isArtist)
				artist = str
			} catch (e: UnsupportedEncodingException) {
			    Log.e("unsupportedencodingexception", e)
			}

		    }
		}

		Log.d("done.")
		return true
	    }
	} catch (e: IOException) {
	    Log.e("ioexception", e)
	    return false
	}
    }

    private fun isValidFrameIdChar(b: Byte): Boolean {
        if (b >= 'A'.toByte() && b <= 'Z'.toByte())
            return true
        if (b >= '0'.toByte() && b <= '9'.toByte())
            return true
        return false
    }

    private fun testFrameId(id: ByteArray, for22: ByteArray, for23: ByteArray): Boolean {
        if (id[0] == for22[0]
                && id[1] == for22[1]
                && id[2] == for22[2]
                && id[3].toInt() == 0)
            return true
        if (id[0] == for23[0]
                && id[1] == for23[1]
                && id[2] == for23[2]
                && id[3] == for23[3])
            return true
        return false
    }

    private fun readLEInt(bis: BufferedInputStream): Int? {
	val b1: Int = bis.read()
	val b2: Int = bis.read()
	val b3: Int = bis.read()
	val b4: Int = bis.read()
	if (b4 == -1)
	    return null
	return b4 shl 24 or (b3 shl 16) or (b2 shl 8) or b1
    }
    
    private fun readInt(istrm: BufferedInputStream): Int? {
        val b1 = istrm.read()
        val b2 = istrm.read()
        val b3 = istrm.read()
        val b4 = istrm.read()
        if (b4 == -1)
            return null

        return b1 shl 24 or (b2 shl 16) or (b3 shl 8) or b4
    }

    private fun readInt3(istrm: BufferedInputStream): Int? {
        val b1 = istrm.read()
        val b2 = istrm.read()
        val b3 = istrm.read()
        if (b3 == -1)
            return null

        return b1 shl 16 or (b2 shl 8) or b3
    }

    private fun readSyncsafeInt(istrm: BufferedInputStream): Int? {
        val b1 = istrm.read()
        val b2 = istrm.read()
        val b3 = istrm.read()
        val b4 = istrm.read()
        if (b4 == -1)
            return null

        if (b1 and 0x80 != 0)
            return null
        if (b2 and 0x80 != 0)
            return null
        if (b3 and 0x80 != 0)
            return null
        if (b4 and 0x80 != 0)
            return null

        return b1 shl 21 or (b2 shl 14) or (b3 shl 7) or b4
    }

    private fun tryExtractOther(): Boolean {
	mutex.lock()
	try {
	    retr.setDataSource(path)
	    title = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
	    artist = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)

	    return true
	} catch (e: Exception) {
	    Log.i("exception", e)
	    return false
	} finally {
	    mutex.unlock()
	}
    }

    companion object {
        private val retr = MediaMetadataRetriever()
	private val mutex = ReentrantLock()

	private val BYTE_ARRAY_TT2 = "TT2".toByteArray()
	private val BYTE_ARRAY_TIT2 = "TIT2".toByteArray()
	private val BYTE_ARRAY_TP1 = "TP1".toByteArray()
	private val BYTE_ARRAY_TPE1 = "TPE1".toByteArray()
    }
}
