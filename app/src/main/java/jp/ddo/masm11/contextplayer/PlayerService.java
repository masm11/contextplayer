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
package jp.ddo.masm11.contextplayer;

import android.support.v7.app.NotificationCompat;
import android.app.Service;
import android.app.NotificationManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.media.MediaPlayer;
import android.media.MediaTimestamp;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.os.IBinder;
import android.os.Binder;
import android.os.Handler;

import java.io.File;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Collections;
import java.util.HashSet;
import java.util.WeakHashMap;
import java.util.Set;
import java.util.Locale;

public class PlayerService extends Service {
    public final static String ACTION_A2DP_DISCONNECTED = "jp.ddo.masm11.contextplayer.A2DP_DISCONNECTED";
    public final static String ACTION_HEADSET_UNPLUGGED = "jp.ddo.masm11.contextplayer.HEADSET_UNPLUGGED";
    
    public class CurrentStatus {
	public final long contextId;
	public final String path;
	public final String topDir;
	public final int position;
	public final int duration;
	public CurrentStatus() {
	    this.contextId = PlayerService.this.contextId;
	    this.path = playingPath;
	    this.topDir = PlayerService.this.topDir;
	    this.position = curPlayer == null ? 0 : curPlayer.getCurrentPosition();
	    this.duration = curPlayer == null ? 0 : curPlayer.getDuration();
	}
    }
    
    public interface OnStatusChangedListener {
	void onStatusChanged(CurrentStatus status);
    }
    
    private String topDir;
    private String playingPath, nextPath;
    private MediaPlayer curPlayer, nextPlayer;
    private long contextId;
    private AudioManager audioManager;
    private AudioAttributes audioAttributes;
    private int audioSessionId;
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;
    private Thread broadcaster;
    private Handler handler;
    private Set<OnStatusChangedListener> statusChangedListeners;
    private BroadcastReceiver headsetReceiver;
    
    public void setOnStatusChangedListener(OnStatusChangedListener listener) {
	Log.d("listener=%s", listener.toString());
	statusChangedListeners.add(listener);
    }
    
    @Override
    public void onCreate() {
	Log.init(getExternalCacheDir());
	
	statusChangedListeners = Collections.newSetFromMap(
		new WeakHashMap<OnStatusChangedListener, Boolean>());
	
	headsetReceiver = new HeadsetReceiver();
	registerReceiver(headsetReceiver, new IntentFilter(AudioManager.ACTION_HEADSET_PLUG));
	
	audioAttributes = new AudioAttributes.Builder()
		.setUsage(AudioAttributes.USAGE_MEDIA)
		.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
		.build();
	audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
	audioSessionId = audioManager.generateAudioSessionId();
	
	audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
	    @Override
	    public void onAudioFocusChange(int focusChange) {
		handleAudioFocusChangeEvent(focusChange);
	    }
	};
	
	handler = new Handler();
	
	loadContext();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	String action = intent != null ? intent.getAction() : null;
	Log.d("action=%s", action);
	if (action != null) {
	    switch (action) {
	    case ACTION_A2DP_DISCONNECTED:
		pause();
		break;
	    case ACTION_HEADSET_UNPLUGGED:
		pause();
		break;
	    }
	}
	return START_NOT_STICKY;
    }
    
    public class PlayerServiceBinder extends Binder {
	public void setOnStatusChangedListener(OnStatusChangedListener listener) {
	    PlayerService.this.setOnStatusChangedListener(listener);
	}
	public CurrentStatus getCurrentStatus() {
	    return PlayerService.this.getCurrentStatus();
	}
	public void seek(int pos) {
	    PlayerService.this.seek(pos);
	}
	public void play(String path) {
	    PlayerService.this.play(path);
	}
	public void pause() {
	    PlayerService.this.pause();
	}
	public void prevTrack() {
	    PlayerService.this.prevTrack();
	}
	public void nextTrack() {
	    PlayerService.this.nextTrack();
	}
	public void switchContext() {
	    PlayerService.this.switchContext();
	}
	public void setTopDir(String topDir) {
	    PlayerService.this.setTopDir(topDir);
	}
    }
    
    @Override
    public IBinder onBind(Intent intent) {
	return new PlayerServiceBinder();
    }
    
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
    private void play(String path) {
	Log.i("path=%s", path);
	
	Log.d("release nextPlayer");
	releaseNextPlayer();
	
	if (path != null) {
	    // path が指定された。
	    // その path から開始し、再生できる曲を曲先頭から再生する。
	    Log.d("path=%s", path);
	    
	    Log.d("release curPlayer");
	    releaseCurPlayer();
	    
	    Log.d("createMediaPlayer");
	    Object[] ret = createMediaPlayer(path, 0, false);
	    if (ret == null) {
		Log.w("No audio file found.");
		return;
	    }
	    Log.d("createMediaPlayer OK.");
	    curPlayer = (MediaPlayer) ret[0];
	    playingPath = (String) ret[1];
	    Log.d("curPlayer=%s", curPlayer.toString());
	    Log.d("playingPath=%s", playingPath);
	} else if (curPlayer != null) {
	    // path が指定されてない && 再生途中だった
	    // 再生再開
	    Log.d("curPlayer exists. starting it.");
	} else if (playingPath != null) {
	    // path が指定されてない && 再生途中でない && context に playingPath がある
	    // その path から開始し、再生できる曲を曲先頭から再生する。
	    Log.d("playingPath=%s", playingPath);
	    
	    Log.d("release nextPlayer");
	    releaseCurPlayer();
	    
	    Log.d("creating mediaplayer.");
	    Object[] ret = createMediaPlayer(playingPath, 0, false);
	    if (ret == null) {
		Log.w("No audio file found.");
		return;
	    }
	    Log.d("creating mediaplayer OK.");
	    curPlayer = (MediaPlayer) ret[0];
	    playingPath = (String) ret[1];
	    Log.d("curPlayer=%s", curPlayer.toString());
	    Log.d("playingPath=%s", playingPath);
	} else {
	    // 何もない
	    // topDir 内から再生できる曲を探し、曲先頭から再生する。
	    Log.d("none.");
	    
	    Log.d("release curPlayer.");
	    releaseCurPlayer();
	    
	    Log.d("creating mediaplayer.");
	    Object[] ret = createMediaPlayer("", 0, false);
	    if (ret == null) {
		Log.w("No audio file found.");
		return;
	    }
	    Log.d("creating mediaplayer OK.");
	    curPlayer = (MediaPlayer) ret[0];
	    playingPath = (String) ret[1];
	    Log.d("curPlayer=%s", curPlayer.toString());
	    Log.d("playingPath=%s", playingPath);
	}
	
	Log.d("starting.");
	startPlay();
	Log.d("enqueue next player.");
	enqueueNext();
    }
    
    /* 再生を一時停止する。
     *  - curPlayer != null の場合
     *    → pause() し、context を保存する
     *  - curPlayer == null の場合
     *    → 何もしない
     */
    private void pause() {
	Log.d("");
	stopPlay();
    }
    
    private void prevTrack() {
	if (curPlayer != null) {
	    int pos = curPlayer.getCurrentPosition();
	    if (pos >= 3 * 1000)
		curPlayer.seekTo(0);
	    else {
		releaseNextPlayer();
		releaseCurPlayer();
		
		Object[] ret = createMediaPlayer(selectPrev(playingPath), 0, true);
		if (ret == null) {
		    Log.w("No audio file.");
		    stopPlay();
		} else {
		    curPlayer = (MediaPlayer) ret[0];
		    playingPath = (String) ret[1];
		    curPlayer.start();
		    enqueueNext();
		}
	    }
	}
    }
    
    private void nextTrack() {
	if (curPlayer != null) {
	    releaseCurPlayer();
	    
	    playingPath = nextPath;
	    curPlayer = nextPlayer;
	    nextPath = null;
	    nextPlayer = null;
	    
	    if (curPlayer != null) {
		curPlayer.start();
		enqueueNext();
	    }
	}
    }
    
    private void seek(int pos) {
	Log.d("pos=%d.", pos);
	if (pos != -1 && curPlayer != null)
	    curPlayer.seekTo(pos);
    }
    
    // curPlayer がセットされた状態で呼ばれ、
    // 再生を start する。
    private void startPlay() {
	if (curPlayer != null) {
	    Log.d("request audio focus.");
	    audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
	    
	    try {
		Log.d("starting.");
		curPlayer.start();
	    } catch (Exception e) {
		Log.e(e, "exception");
	    }
	    
	    Log.d("set to foreground");
	    setForeground(true);
	    
	    startBroadcast();
	    
	    saveContext();
	}
    }
    
    private void stopPlay() {
	try {
	    stopBroadcast();
	    
	    Log.d("set to non-foreground");
	    setForeground(false);
	    
	    if (curPlayer != null) {
		Log.d("pause %s", curPlayer.toString());
		curPlayer.pause();
	    }
	    
	    Log.d("abandon audio focus.");
	    audioManager.abandonAudioFocus(audioFocusChangeListener);
	    
	    Log.d("save context");
	    saveContext();
	} catch (Exception e) {
	    Log.e(e, "exception");
	}
    }
    
    private void handleAudioFocusChangeEvent(int focusChange) {
	Log.d("focusChange=%d.", focusChange);
	switch (focusChange) {
	case AudioManager.AUDIOFOCUS_GAIN:
	    if (curPlayer != null)
		curPlayer.setVolume(1.0f, 1.0f);
	    if (nextPlayer != null)
		nextPlayer.setVolume(1.0f, 1.0f);
	    break;
	case AudioManager.AUDIOFOCUS_LOSS:
	case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
	    pause();
	    break;
	case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
	    if (curPlayer != null)
		curPlayer.setVolume(0.25f, 0.25f);
	    if (nextPlayer != null)
		nextPlayer.setVolume(0.25f, 0.25f);
	    break;
	}
    }

    private void enqueueNext() {
	Log.d("release nextPlayer");
	releaseNextPlayer();
	
	Log.d("creating mediaplayer");
	Object[] ret = createMediaPlayer(selectNext(playingPath), 0, false);
	if (ret == null) {
	    Log.w("No audio file found.");
	    return;
	}
	Log.d("creating mediaplayer OK.");
	nextPlayer = (MediaPlayer) ret[0];
	nextPath = (String) ret[1];
	Log.d("nextPlayer=%s", nextPlayer.toString());
	Log.d("nextPath=%s", nextPath);
	try {
	    Log.d("setting it as nextmediaplayer.");
	    curPlayer.setNextMediaPlayer(nextPlayer);
	} catch (Exception e) {
	    Log.e(e, "exception");
	}
    }
    
    private Object[] createMediaPlayer(String path, int pos, boolean back) {
	Log.d("path=%s", path);
	Log.d("pos=%d", pos);
	HashSet<String> tested = new HashSet<>();
	MediaPlayer player = null;
	while (true) {
	    Log.d("iter");
	    try {
		Log.d("path=%s", path);
		if (path == null || tested.contains(path)) {
		    // 再生できるものがない…
		    Log.d("No audio file.");
		    return null;
		}
		tested.add(path);
		
		Log.d("try create mediaplayer.");
		player = MediaPlayer.create(this, Uri.parse("file://" + path), null, audioAttributes, audioSessionId);
		if (player == null) {
		    Log.w("MediaPlayer.create() failed: %s", path);
		    path = back ? selectPrev(path) : selectNext(path);
		    pos = 0;	// お目当てのファイルが見つからなかった。次のファイルの先頭からとする。
		    continue;
		}
		Log.d("create mediaplayer ok.");
		if (pos > 0) {	// 0 の場合に seekTo() すると、曲の頭が切れるみたい?
		    Log.d("seek to %d.", pos);
		    player.seekTo(pos);
		}
		player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
		    @Override
		    public void onCompletion(MediaPlayer mp) {
			Log.d("shifting");
			playingPath = nextPath;
			curPlayer = nextPlayer;
			Log.d("now playingPath=%s", playingPath);
			Log.d("now curPlayer=%s", curPlayer == null ? "null" : curPlayer.toString());
			Log.d("releasing %s", mp.toString());
			mp.release();
			Log.d("clearing nextPath/nextPlayer");
			nextPath = null;
			nextPlayer = null;
			
			saveContext();
			
			if (curPlayer != null) {
			    Log.d("enqueue next mediaplayer.");
			    enqueueNext();
			} else
			    stopPlay();
		    }
		});
		player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
		    @Override
		    public boolean onError(MediaPlayer mp, int what, int extra) {
			Log.d("error reported. %d, %d.", what, extra);
			
			// 両方 release して新たに作り直す。
			releaseNextPlayer();
			releaseCurPlayer();
			
			Log.d("creating mediaplayer.");
			Object[] ret = createMediaPlayer(nextPath, 0, false);
			if (ret == null) {
			    Log.w("No audio file found.");
			    stopPlay();
			    return true;
			}
			
			curPlayer = (MediaPlayer) ret[0];
			playingPath = (String) ret[1];
			
			Log.d("starting it.");
			curPlayer.start();
			
			Log.d("enqueuing next.");
			enqueueNext();
			
			saveContext();
			
			return true;
		    }
		});
		
		Log.d("done. player=%s, path=%s", player.toString(), path);
		return new Object[] { player, path };
	    } catch (Exception e) {
		Log.e(e, "exception");
	    }
	    
	    return null;
	}
    }
    
    private void releaseCurPlayer() {
	Log.d("");
	try {
	    if (curPlayer != null) {
		Log.d("releasing...");
		curPlayer.release();
		Log.d("releasing... ok");
		curPlayer = null;
	    }
	} catch (Exception e) {
	    Log.e(e, "exception");
	}
    }
    
    private void releaseNextPlayer() {
	Log.d("");
	try {
	    if (nextPlayer != null) {
		Log.d("releasing...");
		nextPlayer.release();
		Log.d("releasing... ok");
		nextPlayer = null;
	    }
	} catch (Exception e) {
	    Log.e(e, "exception");
	}
    }
    
    private String selectNext(String nextOf) {
	Log.d("nextOf=%s", nextOf);
	String found = null;
	if (nextOf.startsWith(topDir)) {
	    nextOf = nextOf.substring(topDir.length() + 1);	// +1: for '/'
	    String[] parts = nextOf.split("/");
	    found = lookForFile(new File(topDir), parts, 0, false);
	}
	if (found == null)
	    found = lookForFile(new File(topDir), null, 0, false);
	Log.d("found=%s", found);
	return found;
    }
    
    private String selectPrev(String prevOf) {
	Log.d("prevOf=%s", prevOf);
	String found = null;
	if (prevOf.startsWith(topDir)) {
	    prevOf = prevOf.substring(topDir.length() + 1);	// +1: for '/'
	    String[] parts = prevOf.split("/");
	    found = lookForFile(new File(topDir), parts, 0, true);
	}
	if (found == null)
	    found = lookForFile(new File(topDir), null, 0, true);
	Log.d("found=%s", found);
	return found;
    }
    
    /* 次のファイルを探す。
     *   dir: 今見ているディレクトリ
     *   parts[]: topdir からの相対 path。'/' で区切られている。
     *   parts_idx: ディレクトリの nest。
     *   backward: 逆向き検索。
     * 最初にこのメソッドに来る時、nextOf は、
     *   /dir/parts[0]/parts[1]/…/parts[N]
     * だったことになる。
     * lookForFile() の役割は、dir 内 subdir も含めて、nextOf の次のファイルを探すこと。
     * parts == null の場合、nextOf の path tree から外れた場所を探している。
     */
    private String lookForFile(File dir, String[] parts, int parts_idx, boolean backward) {
	String cur = null;
	if (parts != null) {
	    if (parts_idx < parts.length)
		cur = parts[parts_idx];
	}
	
	File[] files = ExplorerActivity.listFiles(dir, backward);
	
	for (File file: files) {
	    if (cur == null) {
		if (file.isDirectory()) {
		    String r = lookForFile(file, null, parts_idx + 1, backward);
		    if (r != null)
			return r;
		} else {
		    return file.getAbsolutePath();
		}
	    } else {
		int compare = comparePath(file.getName(), cur);
		if (compare == 0) {
		    // 今そこ。
		    if (file.isDirectory()) {
			String r = lookForFile(file, parts, parts_idx + 1, backward);
			if (r != null)
			    return r;
		    } else {
			// これは今再生中。
		    }
		} else if (!backward && compare > 0) {
		    if (file.isDirectory()) {
			// 次を探していたら dir だった
			String r = lookForFile(file, null, parts_idx + 1, backward);
			if (r != null)
			    return r;
		    } else {
			// 次のファイルを見つけた
			return file.getAbsolutePath();
		    }
		} else if (backward && compare < 0) {
		    if (file.isDirectory()) {
			// 次を探していたら dir だった
			String r = lookForFile(file, null, parts_idx + 1, backward);
			if (r != null)
			    return r;
		    } else {
			// 次のファイルを見つけた
			return file.getAbsolutePath();
		    }
		}
	    }
	}
	
	return null;
    }
    
    private int comparePath(String p1, String p2) {
	String l1 = p1.toLowerCase(Locale.getDefault());
	String l2 = p2.toLowerCase(Locale.getDefault());
	int r = l1.compareTo(l2);
	if (r == 0)
	    r = p1.compareTo(p2);
	return r;
    }
    
    /* topDir を変更する。
     *  - path != null の場合:
     *    → topDir を設定し、enqueueNext() し直す。
     *  - path == null の場合:
     *    → 何もしない。
     */
    private void setTopDir(String path) {
	Log.d("path=%s", path);
	if (path != null) {
	    topDir = path;
	    // 「次の曲」が変わる可能性があるので、enqueue しなおす。
	    if (curPlayer != null) {
		Log.d("enqueue next player.");
		enqueueNext();
	    }
	}
    }
    
    /* context を switch する。
     * 今再生中なら pause() し、context を保存する。
     * context を読み出し、再生を再開する。
     */
    private void switchContext() {
	Log.d("curPlayer=%s", curPlayer == null ? "null" : curPlayer.toString());
	stopPlay();	// saveContext() を含む。
	
	Log.d("load context.");
	loadContext();
	
	Log.d("curPlayer=%s", curPlayer == null ? "null" : curPlayer.toString());
	if (curPlayer != null) {
	    Log.d("starting");
	    startPlay();
	    
	    Log.d("set to foreground");
	    setForeground(true);
	    Log.d("enqueue next player.");
	    enqueueNext();
	}
    }
    
    private void setForeground(boolean on) {
	Log.d("on=%b", on);
	if (on) {
	    PlayContext ctxt = PlayContext.find(contextId);
	    String contextName = "noname";
	    if (ctxt != null)
		contextName = ctxt.name;
	    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	    NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
	    builder.setContentTitle(getResources().getString(R.string.app_name));
	    builder.setContentText(getResources().getString(R.string.playing, contextName));
	    builder.setSmallIcon(R.drawable.notification);
	    Intent intent = new Intent(this, MainActivity.class);
	    TaskStackBuilder tsBuilder = TaskStackBuilder.create(this);
	    tsBuilder.addParentStack(MainActivity.class);
	    tsBuilder.addNextIntent(intent);
	    PendingIntent pendingIntent = tsBuilder.getPendingIntent(
		    0, PendingIntent.FLAG_UPDATE_CURRENT);
	    builder.setContentIntent(pendingIntent);
	    startForeground(1, builder.build());
	} else {
	    stopForeground(true);
	}
    }
    
    private void saveContext() {
	Log.d("contextId=%d", contextId);
	PlayContext ctxt = PlayContext.find(contextId);
	if (ctxt != null && curPlayer != null) {
	    Log.d("Id=%d", ctxt.getId());
	    ctxt.path = playingPath;
	    Log.d("path=%s", ctxt.path);
	    MediaTimestamp stamp = curPlayer.getTimestamp();
	    ctxt.pos = stamp.getAnchorMediaTimeUs() / 1000;	// us -> ms
	    Log.d("pos=%d", ctxt.pos);
	    Log.d("ctxt saving...");
	    ctxt.save();
	}
    }
    
    private void loadContext() {
	Log.d("release nextPlayer.");
	releaseNextPlayer();
	
	stopPlay();
	
	Log.d("release curPlayer.");
	releaseCurPlayer();
	Log.d("set to non-foreground.");
	setForeground(false);
	
	Log.d("getting context_id");
	contextId = Long.parseLong(Config.findByKey("context_id").value);
	Log.d("contextId=%d.", contextId);
	PlayContext ctxt = PlayContext.find(contextId);
	if (ctxt != null) {
	    playingPath = ctxt.path;
	    topDir = ctxt.topDir;
	    Log.d("playingPath=%s", playingPath);
	    Log.d("topDir=%s", topDir);
	    
	    if (playingPath != null) {
		Log.d("creating mediaplayer.");
		Object[] ret = createMediaPlayer(playingPath, (int) ctxt.pos, false);
		if (ret == null) {
		    Log.w("No audio file found.");
		    return;
		}
		Log.d("creating mediaplayer. ok.");
		curPlayer = (MediaPlayer) ret[0];
		playingPath = (String) ret[1];
		Log.d("curPlayer=%s", curPlayer.toString());
		Log.d("playingPath=%s", playingPath);
	    } else {
		// 作られたばかりの context の場合。
		Log.d("creating mediaplayer.");
		Object[] ret = createMediaPlayer("", 0, false);
		if (ret == null) {
		    Log.w("No audio file found.");
		    return;
		}
		Log.d("creating mediaplayer. ok.");
		curPlayer = (MediaPlayer) ret[0];
		playingPath = (String) ret[1];
		Log.d("curPlayer=%s", curPlayer.toString());
		Log.d("playingPath=%s", playingPath);
	    }
	}
    }
    
    private void startBroadcast() {
	Runnable code = new Runnable () {
	    @Override
	    public void run() {
		try {
		    while (true) {
			handler.post(new Runnable() {
			    public void run() {
				broadcastStatus();
			    }
			});
			Thread.sleep(500);
		    }
		} catch (InterruptedException e) {
		    Log.d(e, "interrupted.");
		}
	    }
	};
	
	stopBroadcast();	// 念の為
	broadcaster = new Thread(code);
	broadcaster.start();
    }
    
    private void stopBroadcast() {
	if (broadcaster != null) {
	    broadcaster.interrupt();
	    try {
		broadcaster.join();
	    } catch (InterruptedException e) {
		Log.e(e, "interrupted.");
	    }
	    broadcaster = null;
	}
    }
    
    private void broadcastStatus() {
	CurrentStatus status = new CurrentStatus();
	for (OnStatusChangedListener listener: statusChangedListeners) {
	    // Log.d("listener=%s", listener);
	    listener.onStatusChanged(status);
	}
    }
    
    private CurrentStatus getCurrentStatus() {
	return new CurrentStatus();
    }
    
    @Override
    public void onDestroy() {
	Log.d("save context");
	saveContext();
	
	Log.d("release nextPlayer.");
	releaseNextPlayer();
	stopPlay();
	Log.d("release curPlayer.");
	releaseCurPlayer();
	
	unregisterReceiver(headsetReceiver);
    }
}
