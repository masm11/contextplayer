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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
	
	Button btn = (Button) findViewById(R.id.btn);
	btn.setOnClickListener(new View.OnClickListener() {
	    public void onClick(View view) {
/*
		try {
		    MediaPlayer mp1, mp2;
		    mp1 = MediaPlayer.create(MainActivity.this, Uri.parse("file:///sdcard/Music/nana/impact_exciter/nana_ie_01.ogg"));
		    mp2 = MediaPlayer.create(MainActivity.this, Uri.parse("file:///sdcard/Music/nana/impact_exciter/nana_ie_02.ogg"));
		    
		    // mp1.prepare();
		    mp1.start();
		    // mp2.prepare();
		    mp1.setNextMediaPlayer(mp2);
		} catch (Exception e) {
		    android.util.Log.e("cplayer", "exception", e);
		}
*/
		Intent i = new Intent(MainActivity.this, ExplorerActivity.class);
		startActivity(i);
	    }
	});
    }
}
