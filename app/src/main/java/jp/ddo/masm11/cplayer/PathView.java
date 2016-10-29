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
	String cur = this.path;
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
