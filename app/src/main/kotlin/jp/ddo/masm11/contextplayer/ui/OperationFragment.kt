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
package jp.ddo.masm11.contextplayer.ui

import android.app.Fragment
import android.app.Service
import android.widget.ImageButton
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.os.Bundle
import android.os.IBinder
import android.content.Intent
import android.content.ServiceConnection
import android.content.ComponentName

import jp.ddo.masm11.contextplayer.R
import jp.ddo.masm11.contextplayer.service.PlayerService

import jp.ddo.masm11.logger.Log

class OperationFragment : Fragment() {
    private inner class PlayerServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            svc = service as PlayerService.PlayerServiceBinder
        }

        override fun onServiceDisconnected(name: ComponentName) {
            svc = null
        }
    }

    private var svc: PlayerService.PlayerServiceBinder? = null
    private var conn: PlayerServiceConnection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("")
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.operation_fragment, container, false)

        val btnPlay = view.findViewById(R.id.op_play) as ImageButton
        btnPlay.setOnClickListener {
            svc?.play(null)
        }

        val btnPause = view.findViewById(R.id.op_pause) as ImageButton
        btnPause.setOnClickListener {
            svc?.pause()
        }

        val btnPrev = view.findViewById(R.id.op_prev) as ImageButton
        btnPrev.setOnClickListener {
            svc?.prevTrack()
        }

        val btnNext = view.findViewById(R.id.op_next) as ImageButton
        btnNext.setOnClickListener {
            svc?.nextTrack()
        }

        return view
    }

    override fun onStart() {
        super.onStart()

        // started service にする。
        context.startService(Intent(context, PlayerService::class.java))

        val intent = Intent(context, PlayerService::class.java)
        conn = PlayerServiceConnection()
        context.bindService(intent, conn!!, Service.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        context.unbindService(conn!!)

        super.onStop()
    }
}
