/* Context Player - Audio Player with Contexts
    Copyright (C) 2016, 2018 Yuuki Harano

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
package jp.ddo.masm11.contextplayer.receiver

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.Context
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothHeadset
import android.os.Parcelable

import jp.ddo.masm11.contextplayer.service.PlayerService

import jp.ddo.masm11.logger.Log

class BluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("")
        if (intent != null) {
            val action = intent.action
            Log.d("action=${action}")
	    if (action != null && action == BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED) {
		val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
		val prevstate = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1)
                val device = intent.getParcelableExtra<Parcelable>(BluetoothDevice.EXTRA_DEVICE)
                Log.d("headset: ${device}: ${prevstate} -> ${state}")
	    }
	    if (action != null && action == BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED) {
		val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
		val prevstate = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1)
                val device = intent.getParcelableExtra<Parcelable>(BluetoothDevice.EXTRA_DEVICE)
                Log.d("playing_state: ${device}: ${prevstate} -> ${state}")
	    }
            if (action != null && action == BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                val prevstate = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1)
                val device = intent.getParcelableExtra<Parcelable>(BluetoothDevice.EXTRA_DEVICE)
                Log.d("a2dp: ${device}: ${prevstate} -> ${state}")
                if (state == BluetoothProfile.STATE_DISCONNECTED && prevstate != BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("a2dp disconnected.")
                    val i = Intent(context, PlayerService::class.java)
                    i.action = PlayerService.ACTION_A2DP_DISCONNECTED
                    context.startService(i)
                }
            }
        }
    }
}
