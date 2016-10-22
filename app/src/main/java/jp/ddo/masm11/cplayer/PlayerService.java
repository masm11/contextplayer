package jp.ddo.masm11.cplayer;

import android.app.Service;
import android.media.MediaPlayer;
import android.net.Uri;
import android.content.Intent;
import android.os.IBinder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class PlayerService extends Service {
    private String topDir;
    private String playingPath, nextPath;
    private MediaPlayer curPlayer, nextPlayer;
    
    @Override
    public void onCreate() {
	topDir = "/sdcard/Music";
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	String action = intent != null ? intent.getAction() : null;
	if (action != null) {
	    String path;
	    int ctxt;
	    switch (action) {
	    case "PLAY":
		path = intent.getStringExtra("path");
		if (path == null)
		    break;
		play(path);
		enqueueNext();
		break;
		
	    case "SET_TOPDIR":
		path = intent.getStringExtra("path");
		if (path == null)
		    break;
		// setTopDir(path);
		break;
	    }
	}
	return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
	return null;
    }
    
    private void play(String path) {
	android.util.Log.i("PlayerService", "path=" + path);
	playingPath = path;
	
	try {
	    if (curPlayer != null) {
		curPlayer.release();
		curPlayer = null;
	    }
	    if (nextPlayer != null) {
		nextPlayer.release();
		nextPlayer = null;
	    }
	    
	    curPlayer = MediaPlayer.create(this, Uri.parse("file://" + playingPath));
	    curPlayer.start();
	} catch (Exception e) {
	    android.util.Log.e("cplayer", "exception", e);
	}
    }
    
    private void enqueueNext() {
	nextPath = selectNext();
	android.util.Log.i("PlayerService", "nextPath=" + nextPath);
	nextPlayer = MediaPlayer.create(this, Uri.parse("file://" + nextPath));
	curPlayer.setNextMediaPlayer(nextPlayer);
    }
    
    private String selectNext() {
	// fixme: もちっと効率良く。
	ArrayList<String> list = new ArrayList<>();
	scan(new File(topDir), list);
	Collections.sort(list);
	for (String path: list) {
	    if (path.compareTo(playingPath) > 0)
		return path;
	}
	try {
	    // loop.
	    return list.get(0);
	} catch (IndexOutOfBoundsException e) {
	    // ファイルが一つも見つからなかった。
	    return null;
	}
    }
    
    private void scan(File dir, ArrayList<String> scanResult) {
	for (File file: dir.listFiles()) {
	    if (file.isDirectory())
		scan(file, scanResult);
	    else
		scanResult.add(file.getAbsolutePath());
	}
    }
}
