package jp.ddo.masm11.cplayer;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothA2dp;

public class BluetoothReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
	Log.d("");
	if (intent != null) {
	    String action = intent.getAction();
	    Log.d("action=%s", action);
	    if (action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
		int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
		int prevstate = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1);
		Log.d("state=%s", state);
		Log.d("prevstate=%s", prevstate);
		Log.d("device=%s", intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE).toString());
		if (state == BluetoothProfile.STATE_DISCONNECTED) {
		    Log.d("a2dp disconnected.");
		    Intent i = new Intent(context, PlayerService.class);
		    i.setAction("A2DP_DISCONNECTED");
		    context.startService(i);
		}
	    }
	}
    }
}
