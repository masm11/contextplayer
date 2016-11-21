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

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.annotation.NonNull;
import android.app.Service;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.net.Uri;
import android.widget.Button;
import android.widget.TextView;
import android.widget.SeekBar;
import android.content.Intent;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.Manifest;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
    implements ActivityCompat.OnRequestPermissionsResultCallback {
    
    private static final int REQ_PERMISSION_ON_CREATE = 1;
    
    private class PlayerServiceConnection implements ServiceConnection {
	private PlayerService.OnStatusChangedListener listener = new PlayerService.OnStatusChangedListener() {
	    @Override
	    public void onStatusChanged(PlayerService.CurrentStatus status) {
/*
		Log.d("path=%s, topDir=%s, position=%d.",
			status.path, status.topDir, status.position);
*/
		updateTrackInfo(status);
	    }
	};
	
	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
	    svc = (PlayerService.PlayerServiceBinder) service;
	    
	    svc.setOnStatusChangedListener(listener);
	    
	    updateTrackInfo(svc.getCurrentStatus());
	    
	    if (needSwitchContext) {
		svc.switchContext();
		needSwitchContext = false;
	    }
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
    private boolean needSwitchContext;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	Log.init(getExternalCacheDir());
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);
	
	FragmentManager fragMan = getFragmentManager();
	ActionBarFragment frag = (ActionBarFragment) fragMan.findFragmentById(R.id.actionbar_frag);
	setSupportActionBar(frag.getToolbar());
	
	rootDir = Environment.getExternalStoragePublicDirectory(
		Environment.DIRECTORY_MUSIC);
	Log.d("rootDir=%s", rootDir.getAbsolutePath());
	
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
	
	if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
	    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
		// permission がない && 説明必要 => 説明
		AlertDialog dialog = new AlertDialog.Builder(this)
			.setMessage(R.string.please_grant_permission)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			    @Override
			    public void onClick(DialogInterface dialog, int id) {
				String[] permissions = new String[] {
				    Manifest.permission.READ_EXTERNAL_STORAGE,
				};
				ActivityCompat.requestPermissions(MainActivity.this, permissions, REQ_PERMISSION_ON_CREATE);
			    }
			})
			.create();
		dialog.show();
	    } else {
		// permission がない && 説明不要 => request。
		String[] permissions = new String[] {
		    Manifest.permission.READ_EXTERNAL_STORAGE,
		};
		ActivityCompat.requestPermissions(this, permissions, REQ_PERMISSION_ON_CREATE);
	    }
	} else {
	    // permission がある
	    rootDir.mkdirs();
	}
	
	Intent intent = getIntent();
	if (intent != null) {
	    String action = intent.getAction();
	    if (action != null && action.equals(Intent.ACTION_MAIN)) {
		long id = intent.getLongExtra("jp.ddo.masm11.contextplayer.CONTEXT_ID", -1);
		
		if (id != -1) {
		    config = Config.findByKey("context_id");
		    config.value = "" + id;
		    config.save();
		    
		    needSwitchContext = true;
		}
	    }
	}
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode,
	    @NonNull String[] permissions,
	    @NonNull int[] grantResults) {
	if (requestCode == REQ_PERMISSION_ON_CREATE) {
	    if (grantResults[0] != PackageManager.PERMISSION_GRANTED)
		finish();
	    else
		rootDir.mkdirs();
	}
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
	    
	    Metadata meta = new Metadata(curPath);
	    String title = null, artist = null;
	    if (meta.extract()) {
		title = meta.getTitle();
		artist = meta.getArtist();
	    }
	    
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
	}
	
	if (!strEq(curTopDir, status.topDir)) {
	    curTopDir = status.topDir;
	    
	    PathView pathView = (PathView) findViewById(R.id.playing_filename);
	    assert pathView != null;
	    pathView.setTopDir(curTopDir);
	}
	
	if (maxPos != status.duration) {
	    maxPos = status.duration;
	    
	    SeekBar seekBar = (SeekBar) findViewById(R.id.playing_pos);
	    assert seekBar != null;
	    seekBar.setMax(maxPos);
	    
	    int sec = maxPos / 1000;
	    String maxTime = String.format(Locale.US, "%d:%02d", sec / 60, sec % 60);
	    TextView textView = (TextView) findViewById(R.id.playing_maxtime);
	    assert textView != null;
	    textView.setText(maxTime);
	}
	
	if (curPos != status.position) {
	    curPos = status.position;
	    
	    SeekBar seekBar = (SeekBar) findViewById(R.id.playing_pos);
	    assert seekBar != null;
	    seekBar.setProgress(curPos);
	    
	    int sec = curPos / 1000;
	    String curTime = String.format(Locale.US, "%d:%02d", sec / 60, sec % 60);
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
