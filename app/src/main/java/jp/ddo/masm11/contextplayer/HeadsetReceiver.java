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
import android.media.AudioManager;

public class HeadsetReceiver extends BroadcastReceiver {
    private int isPlugged = -1;
    
    @Override
    public void onReceive(Context context, Intent intent) {
	Log.d("");
	if (intent != null) {
	    String action = intent.getAction();
	    Log.d("action=%s", action);
	    if (action.equals(AudioManager.ACTION_HEADSET_PLUG)) {
		int state = intent.getIntExtra("state", -1);
		String name = intent.getStringExtra("name");
		int microphone = intent.getIntExtra("microphone", -1);
		Log.d("state=%d", state);
		Log.d("name=%s", name);
		Log.d("microphone=%d", microphone);
		/* PlayerService を起動すると、必ず飛んでくる。
		 * もしかして現在の状態の通知のつもりか?
		 */
		if (isPlugged != -1) {
		    if (state == 0) {
			Log.d("headset unplugged.");
			Intent i = new Intent(context, PlayerService.class);
			i.setAction(PlayerService.ACTION_HEADSET_UNPLUGGED);
			context.startService(i);
		    }
		    isPlugged = state;
		} else {
		    isPlugged = state;
		}
	    }
	}
    }
}
