package jp.ddo.masm11.cplayer;

import android.support.v7.app.NotificationCompat;
import android.app.Service;
import android.app.NotificationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.content.Intent;
import android.os.IBinder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class PlayerService extends Service {
    private String topDir;
    private String playingPath, nextPath;
    private MediaPlayer curPlayer, nextPlayer;
    
    @Override
    public void onCreate() {
	Log.init(getExternalCacheDir());
	
	topDir = "/sdcard/Music";
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	String action = intent != null ? intent.getAction() : null;
	if (action != null) {
	    String path;
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
		setTopDir(path);
		break;
		
	    case "SWITCH":
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
	Log.i("path=%s", path);
	playingPath = path;
	
	try {
	    if (nextPlayer != null) {
		nextPlayer.release();
		nextPlayer = null;
	    }
	} catch (Exception e) {
	    Log.e(e, "exception");
	}
	try {
	    if (curPlayer != null) {
		curPlayer.release();
		curPlayer = null;
	    }
	} catch (Exception e) {
	    Log.e(e, "exception");
	}
	
	Object[] ret = createMediaPlayer(playingPath);
	if (ret == null) {
	    Log.w("No audio file found.");
	    return;
	}
	curPlayer = (MediaPlayer) ret[0];
	playingPath = (String) ret[1];
	try {
	    curPlayer.start();
	} catch (Exception e) {
	    Log.e(e, "exception");
	}
	setForeground(true);
    }
    
    private void stop() {
	setForeground(false);
	try {
	    if (nextPlayer != null) {
		nextPlayer.release();
		nextPlayer = null;
	    }
	} catch (Exception e) {
	    Log.e(e, "exception");
	}
	try {
	    if (curPlayer != null) {
		curPlayer.release();
		curPlayer = null;
	    }
	} catch (Exception e) {
	    Log.e(e, "exception");
	}
    }
    
    private void enqueueNext() {
	try {
	    if (nextPlayer != null) {
		nextPlayer.release();
		nextPlayer = null;
	    }
	} catch (Exception e) {
	    Log.e(e, "exception");
	}
	
	Object[] ret = createMediaPlayer(selectNext(playingPath));
	if (ret == null) {
	    Log.w("No audio file found.");
	    return;
	}
	nextPlayer = (MediaPlayer) ret[0];
	nextPath = (String) ret[1];
	try {
	    curPlayer.setNextMediaPlayer(nextPlayer);
	} catch (Exception e) {
	    Log.e(e, "exception");
	}
    }
    
    private Object[] createMediaPlayer(String path) {
	HashSet<String> tested = new HashSet<>();
	MediaPlayer player = null;
	while (true) {
	    try {
		if (path == null || tested.contains(path)) {
		    // 再生できるものがない…
		    return null;
		}
		tested.add(path);
		
		player = MediaPlayer.create(this, Uri.parse("file://" + path));
		if (player == null) {
		    Log.w("MediaPlayer.create() failed: %s", path);
		    path = selectNext(path);
		    continue;
		}
		player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
		    @Override
		    public void onCompletion(MediaPlayer mp) {
			playingPath = nextPath;
			curPlayer = nextPlayer;
			mp.release();
			nextPath = null;
			nextPlayer = null;
			enqueueNext();
		    }
		});
		
		return new Object[] { player, path };
	    } catch (Exception e) {
		Log.e(e, "exception");
	    }
	    
	    return null;
	}
    }
    
    private String selectNext(String nextOf) {
	// fixme: もちっと効率良く。
	ArrayList<String> list = new ArrayList<>();
	scan(new File(topDir), list);
	Collections.sort(list);
	for (String path: list) {
	    if (path.compareTo(nextOf) > 0)
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
    
    private void setTopDir(String path) {
	topDir = path;
	// 「次の曲」が変わる可能性があるので、enqueue しなおす。
	if (curPlayer != null)
	    enqueueNext();
    }
    
    private void setForeground(boolean on) {
	if (on) {
	    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	    NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
	    builder.setContentTitle("Context Player");
	    builder.setContentText("Playing...");
	    builder.setSmallIcon(R.mipmap.ic_launcher);
	    startForeground(1, builder.build());
	} else {
	    stopForeground(false);
	}
    }
}
