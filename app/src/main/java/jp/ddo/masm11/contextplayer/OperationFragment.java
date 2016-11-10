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
    private class PlayerServiceConnection implements ServiceConnection {
	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
	    svc = (PlayerService.PlayerServiceBinder) service;
	}
	
	@Override
	public void onServiceDisconnected(ComponentName name) {
	    svc = null;
	}
    }
    
    private PlayerService.PlayerServiceBinder svc;
    private PlayerServiceConnection conn;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
	Log.d("");
	super.onCreate(savedInstanceState);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	View view = inflater.inflate(R.layout.operation_fragment, container, false);
	
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
	
	// started service にする。
	getContext().startService(new Intent(getContext(), PlayerService.class));
	
	Intent intent = new Intent(getContext(), PlayerService.class);
	conn = new PlayerServiceConnection();
	getContext().bindService(intent, conn, Service.BIND_AUTO_CREATE);
    }
    
    @Override
    public void onStop() {
	getContext().unbindService(conn);
	
	super.onStop();
    }
}
