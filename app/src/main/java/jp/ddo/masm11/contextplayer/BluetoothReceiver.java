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
		Log.d("state=%d", state);
		Log.d("prevstate=%d", prevstate);
		Log.d("device=%s", intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE).toString());
		if (state == BluetoothProfile.STATE_DISCONNECTED && prevstate != BluetoothProfile.STATE_DISCONNECTED) {
		    Log.d("a2dp disconnected.");
		    Intent i = new Intent(context, PlayerService.class);
		    i.setAction(PlayerService.ACTION_A2DP_DISCONNECTED);
		    context.startService(i);
		}
	    }
	}
    }
}
