package jp.ddo.masm11.cplayer;

import android.support.v7.app.AppCompatActivity;
import android.app.Service;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.media.MediaPlayer;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.widget.Button;
import android.widget.TextView;
import android.widget.SeekBar;
import android.content.Intent;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.ComponentName;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private class PlayerServiceConnection implements ServiceConnection {
	private PlayerService.OnStatusChangedListener listener = new PlayerService.OnStatusChangedListener() {
	    @Override
	    public void onStatusChanged(PlayerService.CurrentStatus status) {
		Log.d("path=%s, topDir=%s, position=%d.",
			status.path, status.topDir, status.position);
		updateTrackInfo(status);
	    }
	};
	
	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
	    svc = (PlayerService.PlayerServiceBinder) service;
	    
	    svc.setOnStatusChangedListener(listener);
	    
	    updateTrackInfo(svc.getCurrentStatus());
	}
	
	@Override
	public void onServiceDisconnected(ComponentName name) {
	    svc = null;
	}
    }
    
    private PlayerService.PlayerServiceBinder svc;
    private ServiceConnection conn;
    private File rootDir;
    private String curPath;
    private String curTopDir;
    private int curPos;	// msec
    private int maxPos;	// msec
    private boolean seeking;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	Log.init(getExternalCacheDir());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
	
	rootDir = Environment.getExternalStoragePublicDirectory(
		Environment.DIRECTORY_MUSIC);
	Log.d("rootDir=%s", rootDir.getAbsolutePath());
	rootDir.mkdirs();
	
	if (PlayContext.all().size() == 0) {
	    PlayContext ctxt = new PlayContext();
	    ctxt.name = getResources().getString(R.string.default_context);
	    ctxt.topDir = rootDir.getAbsolutePath();
	    ctxt.save();
	}
	
	Config config = Config.findByKey("context_id");
	if (config == null) {
	    config = new Config();
	    config.key = "context_id";
	    config.value = PlayContext.all().get(0).getId().toString();
	    config.save();
	}
	
	TextView textView;
	textView = (TextView) findViewById(R.id.context_name);
	assert textView != null;
	textView.setOnClickListener(new View.OnClickListener() {
	    public void onClick(View view) {
		Intent i = new Intent(MainActivity.this, ContextActivity.class);
		startActivity(i);
	    }
	});
	
	View layout = findViewById(R.id.playing_info);
	assert layout != null;
	layout.setOnClickListener(new View.OnClickListener() {
	    @Override
	    public void onClick(View v) {
		Intent i = new Intent(MainActivity.this, ExplorerActivity.class);
		PlayContext ctxt = PlayContext.all().get(0);
		i.putExtra("CONTEXT_ID", ctxt.getId());
		startActivity(i);
	    }
	});
	
	SeekBar seekBar = (SeekBar) findViewById(R.id.playing_pos);
	assert seekBar != null;
	seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
	    @Override
	    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (fromUser) {
		    if (svc != null)
			svc.seek(progress);
		}
	    }
	    @Override
	    public void onStartTrackingTouch(SeekBar seekBar) {
		seeking = true;
	    }
	    @Override
	    public void onStopTrackingTouch(SeekBar seekBar) {
		seeking = false;
	    }
	});
    }
    
    @Override
    protected void onStart() {
	super.onStart();
	
	// started service にする。
	startService(new Intent(this, PlayerService.class));
	
	Intent intent = new Intent(this, PlayerService.class);
	conn = new PlayerServiceConnection();
	bindService(intent, conn, Service.BIND_AUTO_CREATE);
    }
    
    @Override
    protected void onResume() {
	PlayContext ctxt = PlayContext.find(Long.parseLong(Config.findByKey("context_id").value));
	TextView textView = (TextView) findViewById(R.id.context_name);
	assert textView != null;
	if (ctxt != null)
	    textView.setText(ctxt.name);
	
	super.onResume();
    }
    
    @Override
    protected void onStop() {
	unbindService(conn);
	
	super.onStop();
    }
    
    private void updateTrackInfo(PlayerService.CurrentStatus status) {
	if (!strEq(curPath, status.path)) {
	    curPath = status.path;
	    
	    PathView pathView = (PathView) findViewById(R.id.playing_filename);
	    assert pathView != null;
	    pathView.setRootDir(rootDir.getAbsolutePath());
	    pathView.setPath(curPath);
	    
	    MediaMetadataRetriever retr = new MediaMetadataRetriever();
	    String title = null, artist = null;
	    String duration = null;
	    try {
		retr.setDataSource(curPath);
		title = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
		artist = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
		duration = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
	    } catch (Exception e) {
		Log.i(e, "exception");
	    }
	    retr.release();
	    
	    if (title == null)
		title = getResources().getString(R.string.unknown_title);
	    if (artist == null)
		artist = getResources().getString(R.string.unknown_artist);
	    
	    TextView textView;
	    textView = (TextView) findViewById(R.id.playing_title);
	    assert textView != null;
	    textView.setText(title);
	    
	    textView = (TextView) findViewById(R.id.playing_artist);
	    assert textView != null;
	    textView.setText(artist);
	    
	    if (duration != null)
		maxPos = Integer.parseInt(duration);
	    else
		maxPos = 0;
	    SeekBar seekBar = (SeekBar) findViewById(R.id.playing_pos);
	    assert seekBar != null;
	    seekBar.setMax(maxPos);
	    
	    int sec = maxPos / 1000;
	    String maxTime = String.format("%d:%02d", sec / 60, sec % 60);
	    textView = (TextView) findViewById(R.id.playing_maxtime);
	    assert textView != null;
	    textView.setText(maxTime);
	}
	
	if (!strEq(curTopDir, status.topDir)) {
	    curTopDir = status.topDir;
	    
	    PathView pathView = (PathView) findViewById(R.id.playing_filename);
	    assert pathView != null;
	    pathView.setTopDir(curTopDir);
	}
	
	if (curPos != status.position) {
	    curPos = status.position;
	    
	    SeekBar seekBar = (SeekBar) findViewById(R.id.playing_pos);
	    assert seekBar != null;
	    seekBar.setProgress(curPos);
	    
	    int sec = curPos / 1000;
	    String curTime = String.format("%d:%02d", sec / 60, sec % 60);
	    TextView textView = (TextView) findViewById(R.id.playing_curtime);
	    assert textView != null;
	    textView.setText(curTime);
	}
    }
    
    private boolean strEq(String s1, String s2) {
	if (s1 == s2)
	    return true;
	if (s1 == null && s2 != null)
	    return false;
	if (s1 != null && s2 == null)
	    return false;
	return s1.equals(s2);
    }
}
