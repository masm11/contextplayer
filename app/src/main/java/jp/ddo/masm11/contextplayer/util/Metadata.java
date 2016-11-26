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
package jp.ddo.masm11.contextplayer.util;

import android.media.MediaMetadataRetriever;

import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import jp.ddo.masm11.logger.Log;

public class Metadata {
    private static MediaMetadataRetriever retr = new MediaMetadataRetriever();
    
    private String path;
    private String title;
    private String artist;
    
    public Metadata(String path) {
	this.path = path;
    }
    
    public boolean extract() {
	if (tryExtractOgg())
	    return true;
	if (tryExtractID3v2())
	    return true;
	if (tryExtractOther())
	    return true;
	
	return false;
    }
    
    public String getTitle() {
	return title;
    }
    
    public String getArtist() {
	return artist;
    }
    
    /* MediaMetadataRetriever に任せると曲名が化ける場合があるので、
     * 自前で取り出す。
     * 参考:
     *  http://www.xiph.org/vorbis/doc/Vorbis_I_spec.html
     */
    private boolean tryExtractOgg() {
	BufferedInputStream bis = null;
	
	try {
	    bis = new BufferedInputStream(new FileInputStream(path));
	    
	    if (bis.read() != 'O')
		return false;
	    if (bis.read() != 'g')
		return false;
	    if (bis.read() != 'g')
		return false;
	    if (bis.read() != 'S')
		return false;
	    
	    int b;
	    int step = 0;
	    for (int i = 0; step < 7 && i < 0x10000; i++) {
		b = bis.read();
		
		if (b == -1)
		    return false;
		
		switch (step) {
		case 0:
		    if (b == 0x03)
			step++;
		    break;
		    
		case 1:
		    switch (b) {
		    case 'v':	step++;		break;
		    case 0x03:	step = 1;	break;
		    default:	step = 0;	break;
		    }
		    break;
		    
		case 2:
		    switch (b) {
		    case 'o':	step++;		break;
		    case 0x03:	step = 1;	break;
		    default:	step = 0;	break;
		    }
		    break;
		    
		case 3:
		    switch (b) {
		    case 'r':	step++;		break;
		    case 0x03:	step = 1;	break;
		    default:	step = 0;	break;
		    }
		    break;
		    
		case 4:
		    switch (b) {
		    case 'b':	step++;		break;
		    case 0x03:	step = 1;	break;
		    default:	step = 0;	break;
		    }
		    break;
		    
		case 5:
		    switch (b) {
		    case 'i':	step++;		break;
		    case 0x03:	step = 1;	break;
		    default:	step = 0;	break;
		    }
		    break;
		    
		case 6:
		    switch (b) {
		    case 's':	step++;		break;
		    case 0x03:	step = 1;	break;
		    default:	step = 0;	break;
		    }
		    break;
		}
	    }
	    if (step != 7)
		return false;  
	    
	    int b1, b2, b3, b4;
	    b1 = bis.read();
	    b2 = bis.read();
	    b3 = bis.read();
	    b4 = bis.read();
	    if (b4 == -1)
		return false;
	    int vendorLength = b4 << 24 | b3 << 16 | b2 << 8 | b1;
	    
	    for (int i = 0; i < vendorLength; i++) {
		if (bis.read() == -1)
		    return false;
	    }
	    
	    b1 = bis.read();
	    b2 = bis.read();
	    b3 = bis.read();
	    b4 = bis.read();
	    if (b4 == -1)
		return false;
	    int numComments = b4 << 24 | b3 << 16 | b2 << 8 | b1;
	    
	    for (int i = 0; i < numComments; i++) {
		b1 = bis.read();
		b2 = bis.read();
		b3 = bis.read();
		b4 = bis.read();
		if (b4 == -1)
		    return false;
		int length = b4 << 24 | b3 << 16 | b2 << 8 | b1;
		
		byte[] buf = new byte[length];
		if (bis.read(buf) != length)
		    return false;
		String str = new String(buf);
		int eq = str.indexOf('=');
		if (eq == -1)
		    continue;
		String key = str.substring(0, eq);
		String val = str.substring(eq + 1);
		if (key.equalsIgnoreCase("TITLE"))
		    title = val;
		if (key.equalsIgnoreCase("ARTIST"))
		    artist = val;
	    }
	    
	    b = bis.read();
	    if (b == -1)
		return false;
	    if ((b & 0x01) != 1)
		return false;
	    
	    return true;
	} catch (IOException e) {
	    Log.e("ioexception", e);
	    return false;
	} finally {
	    if (bis != null) {
		try {
		    bis.close();
		} catch (IOException e) {
		    Log.e("ioexception", e);
		}
	    }
	}
    }
    
    private boolean tryExtractID3v2() {
	BufferedInputStream bis = null;
	
	Log.d("path=%s", path);
	try {
	    bis = new BufferedInputStream(new FileInputStream(path));
	    
	    if (bis.read() != 'I')
		return false;
	    if (bis.read() != 'D')
		return false;
	    if (bis.read() != '3')
		return false;
	    Log.d("ID3 found.");
	    
	    int majorVer = bis.read();
	    int minorVer = bis.read();
	    if (minorVer == -1)
		return false;
	    Log.d("major/minorVer: %d, %d.", majorVer, minorVer);
	    
	    int flags = bis.read();
	    if (flags == -1)
		return false;
	    Log.d("flags=%d", flags);
	    
	    int size = readSyncsafeInt(bis);
	    if (size == -1)
		return false;
	    Log.d("size=%d\n", size);
	    
	    // 拡張ヘッダがあるなら読み捨てる。
	    if ((flags & (1<<6)) != 0) {
		Log.d("ext header exists.");
		int sz;
		if (majorVer < 4)
		    sz = readInt(bis);
		else
		    sz = readSyncsafeInt(bis);
		if (sz == -1)
		    return false;
		for (int i = 0; i < sz - 4; i++)
		    bis.read();
	    }
	    
	    Log.d("Now, frames.");
	    while (true) {
		byte[] frameId = new byte[4];
		frameId[0] = (byte) bis.read();
		frameId[1] = (byte) bis.read();
		frameId[2] = (byte) bis.read();
		if (!isValidFrameIdChar(frameId[0]))
		    break;
		if (!isValidFrameIdChar(frameId[1]))
		    break;
		if (!isValidFrameIdChar(frameId[2]))
		    break;
		if (majorVer >= 3) {
		    frameId[3] = (byte) bis.read();
		    if (!isValidFrameIdChar(frameId[3]))
			break;
		}
		Log.d("frameId: %d, %d, %d, %d.", frameId[0], frameId[1], frameId[2], frameId[3]);
		
		int sz;
		switch (majorVer) {
		case 0:
		case 1:
		case 2:
		    sz = readInt3(bis);
		    break;
		case 3:
		    sz = readInt(bis);
		    break;
		case 4:
		default:
		    sz = readSyncsafeInt(bis);
		    break;
		}
		Log.d("sz=%d.", sz);
		
		// flag を読み捨てる。
		if (majorVer >= 3) {
		    bis.read();
		    bis.read();
		}
		
		byte[] data = new byte[sz];
		for (int i = 0; i < sz; i++) {
		    int b = bis.read();
		    if (b == -1)
			return false;
		    data[i] = (byte) b;
		}
		
		boolean isTitle = false;
		boolean isArtist = false;
		if (testFrameId(frameId, new byte[] { 'T', 'T', '2' }, new byte[] { 'T', 'I', 'T', '2' })) {
		    Log.d("is title.");
		    isTitle = true;
		} else if (testFrameId(frameId, new byte[] { 'T', 'P', '1' }, new byte[] { 'T', 'P', 'E', '1' })) {
		    Log.d("is artist.");
		    isArtist = true;
		}
		
		if (isTitle || isArtist) {
		    String encoding = null;
		    int start;
		    
		    StringBuilder sb = new StringBuilder();
		    for (int i = 0; i < data.length; i++)
			sb.append(String.format(" %02x", data[i]));
		    Log.d("data:%s", sb.toString());
		    
		    switch (data[0]) {
		    case 0:	// ISO-8859-1
			encoding = "ISO-8859-1";
			start = 1;
			break;
			
		    case 1:	// UTF-16 with BOM
			if (data.length < 3) {
			    start = -1;
			    break;
			}
			if (data[1] == 0xfe && data[2] == 0xff)
			    encoding = "UTF-16BE";
			else if (data[1] == 0xff && data[2] == 0xfe)
			    encoding = "UTF-16LE";
			else {
			    start = -1;
			    break;
			}
			start = 3;
			break;
			
		    case 2:	// UTF-16BE without BOM
			if (majorVer < 4) {
			    start = -1;
			    break;
			}
			encoding = "UTF-16BE";
			start = 1;
			break;
			
		    case 3:	// UTF-8
			if (majorVer < 4) {
			    start = -1;
			    break;
			}
			encoding = "UTF-8";
			start = 1;
			break;
			
		    default:
			start = -1;
			break;
		    }
		    if (start < 0)
			continue;
		    
		    Log.d("encoding=%s", encoding);
		    Log.d("start=%d", start);
		    
		    // バイト列のバイト数。
		    // terminator(0x00) は必須ではないが、あればそこまで。
		    int len = 0;
		    if (!encoding.startsWith("UTF-16")) {
			while (true) {
			    if (start + len >= data.length)
				break;
			    if (data[start + len] == 0)
				break;
			    len++;
			}
		    } else {
			while (true) {
			    if (start + len + 1 >= data.length)
				break;
			    if (data[start + len] == 0 && data[start + len + 1] == 0)
				break;
			    len += 2;
			}
		    }
		    Log.d("len=%d", len);
		    
		    try {
			String str = new String(data, start, len, encoding);
			Log.d("str=%s", str);
			if (isTitle)
			    title = str;
			if (isArtist)
			    artist = str;
		    } catch (UnsupportedEncodingException e) {
			Log.e("unsupportedencodingexception", e);
		    }
		}
	    }
	    
	    Log.d("done.");
	    return true;
	} catch (IOException e) {
	    Log.e("ioexception", e);
	    return false;
	} finally {
	    if (bis != null) {
		try {
		    bis.close();
		} catch (IOException e) {
		    Log.e("ioexception", e);
		}
	    }
	}
    }
    
    private boolean isValidFrameIdChar(byte b) {
	if (b >= 'A' && b <= 'Z')
	    return true;
	if (b >= '0' && b <= '9')
	    return true;
	return false;
    }
    
    private boolean testFrameId(byte[] id, byte[] for22, byte[] for23) {
	if (id[0] == for22[0]
		&& id[1] == for22[1]
		&& id[2] == for22[2]
		&& id[3] == 0)
	    return true;
	if (id[0] == for23[0]
		&& id[1] == for23[1]
		&& id[2] == for23[2]
		&& id[3] == for23[3])
	    return true;
	return false;
    }
    
    private int readInt(BufferedInputStream is)
	    throws IOException {
	int b1 = is.read();
	int b2 = is.read();
	int b3 = is.read();
	int b4 = is.read();
	if (b4 == -1)
	    return -1;
	
	return b1 << 24 | b2 << 16 | b3 << 8 | b4;
    }
    
    private int readInt3(BufferedInputStream is)
	    throws IOException {
	int b1 = is.read();
	int b2 = is.read();
	int b3 = is.read();
	if (b3 == -1)
	    return -1;
	
	return b1 << 16 | b2 << 8 | b3;
    }
    
    private int readSyncsafeInt(BufferedInputStream is)
	    throws IOException {
	int b1 = is.read();
	int b2 = is.read();
	int b3 = is.read();
	int b4 = is.read();
	if (b4 == -1)
	    return -1;
	
	if ((b1 & 0x80) != 0)
	    return -1;
	if ((b2 & 0x80) != 0)
	    return -1;
	if ((b3 & 0x80) != 0)
	    return -1;
	if ((b4 & 0x80) != 0)
	    return -1;
	
	return b1 << 21 | b2 << 14 | b3 << 7 | b4;
    }
    
    private boolean tryExtractOther() {
	synchronized (retr) {
	    try {
		retr.setDataSource(path);
		title = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
		artist = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
		
		return true;
	    } catch (Exception e) {
		Log.i("exception", e);
		return false;
	    }
	}
    }
}
