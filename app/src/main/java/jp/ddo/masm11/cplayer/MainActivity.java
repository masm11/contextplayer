package jp.ddo.masm11.cplayer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.media.MediaPlayer;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.widget.Button;
import android.widget.TextView;
import android.widget.SeekBar;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private class StatusReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
	    Log.d("");
	    if (intent != null) {
		String action = intent.getAction();
		Log.d("action=%s", action);
		if (action.equals("jp.ddo.masm11.cplayer.STATUS")) {
		    String path = intent.getStringExtra("jp.ddo.masm11.cplayer.FILE");
		    int pos = intent.getIntExtra("jp.ddo.masm11.cplayer.POSITION", -1);
		    String topDir = intent.getStringExtra("jp.ddo.masm11.cplayer.TOPDIR");
		    if (path != null && topDir != null && pos != -1) {
			if (!path.equals(curPath)) {
			    PathView pathView = (PathView) findViewById(R.id.playing_filename);
			    assert pathView != null;
			    pathView.setRootDir("/sdcard/Music");
			    pathView.setTopDir(topDir);
			    pathView.setPath(path);
			    curPath = path;
			    
			    MediaMetadataRetriever retr = new MediaMetadataRetriever();
			    String title = null, artist = null;
			    String duration = null;
			    try {
				retr.setDataSource(path);
				title = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
				artist = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
				duration = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
			    } catch (Exception e) {
				Log.i(e, "exception");
			    }
			    retr.release();
			    
			    if (title == null)
				title = "unknown title";
			    if (artist == null)
				artist = "unknown artist";
			    
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
			
			curPos = pos;
			SeekBar seekBar = (SeekBar) findViewById(R.id.playing_pos);
			assert seekBar != null;
			if (!seeking)
			    seekBar.setProgress(pos);
			
			int sec = curPos / 1000;
			String curTime = String.format("%d:%02d", sec / 60, sec % 60);
			TextView textView = (TextView) findViewById(R.id.playing_curtime);
			assert textView != null;
			textView.setText(curTime);
		    }
		}
	    }
	}
    }
    
    private String curPath;
    private int curPos;	// msec
    private int maxPos;	// msec
    private boolean seeking;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	Log.init(getExternalCacheDir());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
	
	if (PlayContext.all().size() == 0) {
	    PlayContext ctxt = new PlayContext();
	    ctxt.name = "Default Context";
	    ctxt.topDir = "/sdcard/Music";
	    ctxt.save();
	}
	
	Config config = Config.findByKey("context_id");
	if (config == null) {
	    config = new Config();
	    config.key = "context_id";
	    config.value = PlayContext.all().get(0).getId().toString();
	    config.save();
	}
	
	Button btn;
	btn = (Button) findViewById(R.id.contexts);
	btn.setOnClickListener(new View.OnClickListener() {
	    public void onClick(View view) {
		Intent i = new Intent(MainActivity.this, ContextActivity.class);
		startActivity(i);
	    }
	});
	btn = (Button) findViewById(R.id.explorer);
	btn.setOnClickListener(new View.OnClickListener() {
	    public void onClick(View view) {
		
		Intent i = new Intent(MainActivity.this, ExplorerActivity.class);
		PlayContext ctxt = PlayContext.all().get(0);
		i.putExtra("CONTEXT_ID", ctxt.getId());
		startActivity(i);
	    }
	});
	btn = (Button) findViewById(R.id.op_pause);
	btn.setOnClickListener(new View.OnClickListener() {
	    public void onClick(View view) {
		Intent i = new Intent(MainActivity.this, PlayerService.class);
		i.setAction("PAUSE");
		startService(i);
	    }
	});
	btn = (Button) findViewById(R.id.test);
	btn.setOnClickListener(new View.OnClickListener() {
	    public void onClick(View view) {
		Intent i = new Intent(MainActivity.this, PlayerService.class);
		i.setAction("TEST");
		startService(i);
	    }
	});
	
	SeekBar seekBar = (SeekBar) findViewById(R.id.playing_pos);
	assert seekBar != null;
	seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
	    @Override
	    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (fromUser) {
		    Intent intent = new Intent(MainActivity.this, PlayerService.class);
		    intent.setAction("SEEK");
		    intent.putExtra("POS", progress);
		    startService(intent);
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
	
	IntentFilter filter = new IntentFilter("jp.ddo.masm11.cplayer.STATUS");
	registerReceiver(new StatusReceiver(), filter);
    }
}
