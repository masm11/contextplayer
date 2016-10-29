package jp.ddo.masm11.cplayer;

import android.app.Fragment;
import android.widget.Button;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.os.Bundle;
import android.content.Intent;

public class OperationFragment extends Fragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
	Log.d("");
	super.onCreate(savedInstanceState);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	View view = inflater.inflate(R.xml.operation_fragment, container, false);
	
	Button btnPlay = (Button) view.findViewById(R.id.op_play);
	assert btnPlay != null;
	btnPlay.setOnClickListener(new View.OnClickListener() {
	    public void onClick(View view) {
		Intent intent = new Intent(getContext(), PlayerService.class);
		intent.setAction("PLAY");
		getContext().startService(intent);
	    }
	});
	
	Button btnPause = (Button) view.findViewById(R.id.op_pause);
	assert btnPause != null;
	btnPause.setOnClickListener(new View.OnClickListener() {
	    public void onClick(View view) {
		Intent intent = new Intent(getContext(), PlayerService.class);
		intent.setAction("PAUSE");
		getContext().startService(intent);
	    }
	});
	
	Button btnPrev = (Button) view.findViewById(R.id.op_prev);
	assert btnPrev != null;
	btnPrev.setOnClickListener(new View.OnClickListener() {
	    public void onClick(View view) {
		Intent intent = new Intent(getContext(), PlayerService.class);
		intent.setAction("PREV");
		getContext().startService(intent);
	    }
	});
	
	Button btnNext = (Button) view.findViewById(R.id.op_next);
	assert btnNext != null;
	btnNext.setOnClickListener(new View.OnClickListener() {
	    public void onClick(View view) {
		Intent intent = new Intent(getContext(), PlayerService.class);
		intent.setAction("NEXT");
		getContext().startService(intent);
	    }
	});
	
	return view;
    }
}
