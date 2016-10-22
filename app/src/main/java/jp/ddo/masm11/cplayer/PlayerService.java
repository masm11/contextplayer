package jp.ddo.masm11.cplayer;

import android.app.Service;
import android.media.MediaPlayer;
import android.net.Uri;
import android.content.Intent;
import android.os.IBinder;

public class PlayerService extends Service {
    
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
		break;
		
	    case "SET_TOPDIR":
		path = intent.getStringExtra("path");
		if (path == null)
		    break;
		// setTopDir(path);
		break;
		
	    case "SWITCH":
		ctxt = intent.getIntExtra("context", -1);
		if (ctxt == -1)
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
	
	try {
	    MediaPlayer mp;
	    mp = MediaPlayer.create(this, Uri.parse("file://" + path));
	    mp.start();
	    // mp.setNextMediaPlayer(mp2);
	} catch (Exception e) {
	    android.util.Log.e("cplayer", "exception", e);
	}
    }
}
