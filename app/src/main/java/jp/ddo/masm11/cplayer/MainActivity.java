package jp.ddo.masm11.cplayer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.media.MediaPlayer;
import android.net.Uri;
import android.widget.Button;
import android.content.Intent;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

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
    }
}
