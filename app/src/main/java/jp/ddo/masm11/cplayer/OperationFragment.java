package jp.ddo.masm11.cplayer;

import android.app.Fragment;
import android.app.Service;
import android.widget.ImageButton;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.os.Bundle;
import android.os.IBinder;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;

public class OperationFragment extends Fragment {
    private class OpFragmentServiceConnection implements ServiceConnection {
	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
	    svc = ((PlayerService.PlayerServiceBinder) service).getService();
	}
	
	@Override
	public void onServiceDisconnected(ComponentName name) {
	    svc = null;
	}
    }
    
    private PlayerService svc;
    private OpFragmentServiceConnection conn;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
	Log.d("");
	super.onCreate(savedInstanceState);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	View view = inflater.inflate(R.xml.operation_fragment, container, false);
	
	ImageButton btnPlay = (ImageButton) view.findViewById(R.id.op_play);
	assert btnPlay != null;
	btnPlay.setOnClickListener(new View.OnClickListener() {
	    public void onClick(View view) {
		if (svc != null)
		    svc.play(null);
	    }
	});
	
	ImageButton btnPause = (ImageButton) view.findViewById(R.id.op_pause);
	assert btnPause != null;
	btnPause.setOnClickListener(new View.OnClickListener() {
	    public void onClick(View view) {
		if (svc != null)
		    svc.pause();
	    }
	});
	
	ImageButton btnPrev = (ImageButton) view.findViewById(R.id.op_prev);
	assert btnPrev != null;
	btnPrev.setOnClickListener(new View.OnClickListener() {
	    public void onClick(View view) {
		if (svc != null)
		    svc.prevTrack();
	    }
	});
	
	ImageButton btnNext = (ImageButton) view.findViewById(R.id.op_next);
	assert btnNext != null;
	btnNext.setOnClickListener(new View.OnClickListener() {
	    public void onClick(View view) {
		if (svc != null)
		    svc.nextTrack();
	    }
	});
	
	return view;
    }
    
    @Override
    public void onStart() {
	super.onStart();
	
	Intent intent = new Intent(getContext(), PlayerService.class);
	conn = new OpFragmentServiceConnection();
	getContext().bindService(intent, conn, Service.BIND_AUTO_CREATE);
    }
    
    @Override
    public void onStop() {
	getContext().unbindService(conn);
	
	super.onStop();
    }
}
