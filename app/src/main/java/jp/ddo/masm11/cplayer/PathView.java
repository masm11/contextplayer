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

import android.content.Context;
import android.widget.TextView;
import android.text.SpannableString;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.util.AttributeSet;

public class PathView extends TextView {
    private static BackgroundColorSpan span = new BackgroundColorSpan(0xffdddddd);
    private String rootDir;
    private String topDir;
    private String path;
    
    public PathView(Context context, AttributeSet attrs) {
	super(context, attrs);
	topDir = "/";
	rootDir = "/";
    }
    
    public void setRootDir(String rootDir) {
	this.rootDir = rootDir;
	updateText();
    }
    
    public void setTopDir(String topDir) {
	this.topDir = topDir;
	updateText();
    }
    
    public void setPath(String path) {
	this.path = path;
	updateText();
    }
    
    private void updateText() {
	String root = rootDir;
	String top = topDir;
	String cur = this.path == null ? top : this.path;
	if (cur == null)
	    cur = "/";
	if (!root.endsWith("/"))
	    root = root + "/";
	if (!top.endsWith("/"))
	    top = top + "/";
	if (cur.startsWith(root) && top.startsWith(root)) {
	    int rootLen = root.length();
	    cur = cur.substring(rootLen);
	    top = top.substring(rootLen);
	    if (cur.length() == 0)
		cur = "./";
	    if (top.length() == 0)
		top = "./";
	}
	
	if (cur.startsWith(top)) {
	    SpannableString ss = new SpannableString(cur);
	    ss.setSpan(span, 0, top.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	    setText(ss);
	} else {
	    setText(cur);
	}
    }
}
