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
	
	Button btn = (Button) findViewById(R.id.btn);
	btn.setOnClickListener(new View.OnClickListener() {
	    public void onClick(View view) {
		
//		Intent i = new Intent(MainActivity.this, ExplorerActivity.class);
		Intent i = new Intent(MainActivity.this, ContextActivity.class);
//		i.putExtra("CONTEXT_ID", ctxt.getId());
		startActivity(i);
	    }
	});
    }
}
