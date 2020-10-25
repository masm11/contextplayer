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
package me.masm11.contextplayer.receiver

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.Context
import android.media.AudioManager

import me.masm11.contextplayer.service.PlayerService
import me.masm11.contextplayer.util.Log

class HeadsetReceiver : BroadcastReceiver() {
    private var isPlugged = -1

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("")
        if (intent != null) {
            val action = intent.action
            Log.d("action=${action}")
            if (action == AudioManager.ACTION_HEADSET_PLUG) {
                val state = intent.getIntExtra("state", -1)
                val name = intent.getStringExtra("name")
                val microphone = intent.getIntExtra("microphone", -1)
                Log.d("state=${state}")
                Log.d("name=${name}")
                Log.d("microphone=${microphone}")
                /* PlayerService を起動すると、必ず飛んでくる。
		 * もしかして現在の状態の通知のつもりか?
		 */
                if (isPlugged != -1) {
                    if (state == 0) {
                        Log.d("headset unplugged.")
                        val i = Intent(context, PlayerService::class.java)
                        i.action = PlayerService.ACTION_HEADSET_UNPLUGGED
                        context.startForegroundService(i)
                    }
                    isPlugged = state
                } else {
                    isPlugged = state
                }
            }
        }
    }
}
