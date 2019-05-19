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
package me.masm11.contextplayer.ui

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

import kotlinx.android.synthetic.main.operation_fragment.view.*

import me.masm11.contextplayer.R
import me.masm11.contextplayer.service.PlayerService

import me.masm11.logger.Log

class OperationFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("")
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.operation_fragment, container, false)

        view.op_play.setOnClickListener {
            PlayerService.play(context, null)
        }

        view.op_pause.setOnClickListener {
            PlayerService.pause(context)
        }

        view.op_prev.setOnClickListener {
            PlayerService.prevTrack(context)
        }

        view.op_next.setOnClickListener {
            PlayerService.nextTrack(context)
        }

        return view
    }
}
