package jp.ddo.masm11.cplayer;

import android.support.v7.app.NotificationCompat;
import android.app.Service;
import android.app.NotificationManager;
import android.media.MediaPlayer;
import android.media.MediaTimestamp;
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
    private long contextId;
    
    @Override
    public void onCreate() {
	Log.init(getExternalCacheDir());
	
	loadContext();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	String action = intent != null ? intent.getAction() : null;
	if (action != null) {
	    String path;
	    switch (action) {
	    case "PLAY":
		/* 再生を開始する。
		 *  - path が指定されている場合:
		 *    → その path から開始し、再生できる曲を曲先頭から再生する。
		 *  - path が指定されていない場合:
		 *    - curPlayer != null の場合:
		 *      → curPlayer.play() する。
		 *    - curPlayer == null の場合:
		 *      - playingPath != null の場合:
		 *        → その path から開始し、再生できる曲を曲先頭から再生する。
		 *      - playingPath == null の場合:
		 *        → topDir 内で最初に再生できる曲を再生する。
		 */
		path = intent.getStringExtra("path");
		play(path);
		break;
		
	    case "PAUSE":
		/* 再生を一時停止する。
		 *  - curPlayer != null の場合
		 *    → pause() し、context を保存する
		 *  - curPlayer == null の場合
		 *    → 何もしない
		 */
		pause();
		break;
		
	    case "SET_TOPDIR":
		/* topDir を変更する。
		 *  - path != null の場合:
		 *    → topDir を設定し、enqueueNext() し直す。
		 *  - path == null の場合:
		 *    → 何もしない。
		 */
		path = intent.getStringExtra("path");
		setTopDir(path);
		break;
		
	    case "SWITCH":
		/* context を switch する。
		 * 今再生中なら pause() し、context を保存する。
		 * context を読み出し、再生を再開する。
		 */
		switchContext();
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
	
	try {
	    if (nextPlayer != null) {
		nextPlayer.release();
		nextPlayer = null;
	    }
	} catch (Exception e) {
	    Log.e(e, "exception");
	}
	
	if (path != null) {
	    // path が指定された。
	    // その path から開始し、再生できる曲を曲先頭から再生する。
	    
	    try {
		if (curPlayer != null) {
		    curPlayer.release();
		    curPlayer = null;
		}
	    } catch (Exception e) {
		Log.e(e, "exception");
	    }
	    
	    Object[] ret = createMediaPlayer(path, 0);
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
	} else if (curPlayer != null) {
	    // path が指定されてない && 再生途中だった
	    // 再生再開
	    curPlayer.start();
	} else if (playingPath != null) {
	    // path が指定されてない && 再生途中でない && context に playingPath がある
	    // その path から開始し、再生できる曲を曲先頭から再生する。
	    
	    try {
		if (curPlayer != null) {
		    curPlayer.release();
		    curPlayer = null;
		}
	    } catch (Exception e) {
		Log.e(e, "exception");
	    }
	    
	    Object[] ret = createMediaPlayer(playingPath, 0);
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
	} else {
	    // 何もない
	    // topDir 内から再生できる曲を探し、曲先頭から再生する。
	    
	    try {
		if (curPlayer != null) {
		    curPlayer.release();
		    curPlayer = null;
		}
	    } catch (Exception e) {
		Log.e(e, "exception");
	    }
	    
	    Object[] ret = createMediaPlayer("", 0);
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
	}
	
	setForeground(true);
	enqueueNext();
    }
    
    private void pause() {
	try {
	    if (curPlayer != null) {
		setForeground(false);
		curPlayer.pause();
		saveContext();
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
	
	Object[] ret = createMediaPlayer(selectNext(playingPath), 0);
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
    
    private Object[] createMediaPlayer(String path, int pos) {
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
		    pos = 0;	// お目当てのファイルが見つからなかった。次のファイルの先頭からとする。
		    continue;
		}
		player.seekTo(pos);
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
	if (path != null) {
	    topDir = path;
	    // 「次の曲」が変わる可能性があるので、enqueue しなおす。
	    if (curPlayer != null)
		enqueueNext();
	}
    }
    
    private void switchContext() {
	if (curPlayer != null) {
	    curPlayer.pause();
	    setForeground(false);
	}
	
	saveContext();
	loadContext();
	
	if (curPlayer != null) {
	    try {
		curPlayer.start();
	    } catch (Exception e) {
		Log.e(e, "exception");
	    }
	    
	    setForeground(true);
	    enqueueNext();
	}
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
	    stopForeground(true);
	}
    }
    
    private void saveContext() {
	Log.d("contextId=%d", contextId);
	PlayContext ctxt = PlayContext.find(contextId);
	if (ctxt != null && curPlayer != null) {
	    ctxt.path = playingPath;
	    MediaTimestamp stamp = curPlayer.getTimestamp();
	    ctxt.pos = stamp.getAnchorMediaTimeUs() / 1000;	// us -> ms
	    Log.d("ctxt saving...");
	    ctxt.save();
	}
    }
    
    private void loadContext() {
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
		curPlayer.pause();
		setForeground(false);
		saveContext();
	    }
	} catch (Exception e) {
	    Log.e(e, "exception");
	}
	
	contextId = Long.parseLong(Config.findByKey("context_id").value);
	PlayContext ctxt = PlayContext.find(contextId);
	if (ctxt != null) {
	    playingPath = ctxt.path;
	    topDir = ctxt.topDir;
	    
	    if (playingPath != null) {
		Object[] ret = createMediaPlayer(playingPath, (int) ctxt.pos);
		if (ret == null) {
		    Log.w("No audio file found.");
		    return;
		}
		curPlayer = (MediaPlayer) ret[0];
		playingPath = (String) ret[1];
	    }
	}
    }
    
    @Override
    public void onDestroy() {
	saveContext();
    }
}
