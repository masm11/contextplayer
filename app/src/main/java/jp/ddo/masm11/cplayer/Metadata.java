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
package jp.ddo.masm11.cplayer;

import android.media.MediaMetadataRetriever;

import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

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
	    Log.e(e, "ioexception");
	    return false;
	} finally {
	    if (bis != null) {
		try {
		    bis.close();
		} catch (IOException e) {
		    Log.e(e, "ioexception");
		}
	    }
	}
    }
    
    private boolean tryExtractOther() {
	synchronized (retr) {
	    try {
		retr.setDataSource(path);
		title = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
		artist = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
		
		return true;
	    } catch (Exception e) {
		Log.i(e, "exception");
		return false;
	    }
	}
    }
}
