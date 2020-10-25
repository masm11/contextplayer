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

import androidx.fragment.app.Fragment
import android.app.Service
import android.widget.ImageButton
import android.widget.TextView
import android.widget.SeekBar
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.os.Bundle
import android.os.IBinder
import android.content.Intent
import android.content.ServiceConnection
import android.content.ComponentName

import java.util.Locale

import me.masm11.contextplayer.R
import me.masm11.contextplayer.service.PlayerService
import me.masm11.contextplayer.db.PlayContextList
import me.masm11.contextplayer.db.PlayContext
import me.masm11.contextplayer.Application

import me.masm11.contextplayer.util.Log

class ControlPanelFragment : Fragment() {
    private lateinit var playContexts: PlayContextList
    private lateinit var curContext: PlayContext
    private lateinit var onContextChangedListener: (PlayContext) -> Unit
    private lateinit var onContextSwitchListener: (PlayContext) -> Unit
    
    private var curPos: Int = 0    // msec
    private var maxPos: Int = 0    // msec
    private var volume: Int = 100
    
    private lateinit var playingPosView: SeekBar
    private lateinit var playingCurTimeView: TextView
    private lateinit var playingMaxTimeView: TextView
    private lateinit var volumeView: SeekBar
    
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("")
        super.onCreate(savedInstanceState)
    }
    
    private fun setContextListener() {
	onContextSwitchListener = { ctxt ->
	    curContext.removeOnChangedListener(onContextChangedListener)
	    curContext = ctxt
	    curContext.addOnChangedListener(onContextChangedListener)
	    
	    updateVolume(ctxt)
	    updatePos(ctxt)
	}
	onContextChangedListener = { ctxt ->
	    updateVolume(ctxt)
	    updatePos(ctxt)
	}
	
	curContext = playContexts.getCurrent()
	
	curContext.addOnChangedListener(onContextChangedListener)
	playContexts.addOnContextSwitchListener(onContextSwitchListener)
	
	updateVolume(curContext)
	updatePos(curContext)
    }
    
    private fun unsetContextListener() {
	playContexts.removeOnContextSwitchListener(onContextSwitchListener)
	curContext.removeOnChangedListener(onContextChangedListener)
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.control_panel_fragment, container, false)
	
	playContexts = (activity!!.getApplication() as Application).getPlayContextList()
	
	playingPosView = view.findViewById<SeekBar>(R.id.playing_pos)
	playingCurTimeView = view.findViewById<TextView>(R.id.playing_curtime)
	playingMaxTimeView = view.findViewById<TextView>(R.id.playing_maxtime)
	volumeView = view.findViewById<SeekBar>(R.id.volume)
	
        playingPosView.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
		    val c = context
		    if (c != null)
			PlayerService.seek(c, progress)
		}
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                /*NOP*/
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                /*NOP*/
            }
        })
	
        volumeView.max = 100 - VOLUME_BASE
        volumeView.progress = volume - VOLUME_BASE
        volumeView.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(view: SeekBar, progress: Int, fromUser: Boolean) {
                volume = VOLUME_BASE + progress
		val c = context
		if (c != null)
		    PlayerService.setVolume(c, volume)
            }

            override fun onStartTrackingTouch(volume: SeekBar) {
                /*NOP*/
            }

            override fun onStopTrackingTouch(volume: SeekBar) {
                /*NOP*/
            }
        })
	
        view.findViewById<ImageButton>(R.id.op_play).setOnClickListener {
	    val ctxt = context
	    if (ctxt != null)
		PlayerService.play(ctxt, null)
        }

        view.findViewById<ImageButton>(R.id.op_pause).setOnClickListener {
	    val ctxt = context
	    if (ctxt != null)
		PlayerService.pause(ctxt)
        }

        view.findViewById<ImageButton>(R.id.op_prev).setOnClickListener {
	    val ctxt = context
	    if (ctxt != null)
		PlayerService.prevTrack(ctxt)
        }

        view.findViewById<ImageButton>(R.id.op_next).setOnClickListener {
	    val ctxt = context
	    if (ctxt != null)
		PlayerService.nextTrack(ctxt)
        }
	
	setContextListener()
	
        return view
    }
    
    private fun updatePos(ctxt: PlayContext) {
        if (maxPos != ctxt.realtimeDuration.toInt()) {
            maxPos = ctxt.realtimeDuration.toInt()

            playingPosView.max = maxPos

            val sec = maxPos / 1000
            val maxTime = String.format(Locale.US, "%d:%02d", sec / 60, sec % 60)
            playingMaxTimeView.text = maxTime
        }

        if (curPos != ctxt.realtimePos.toInt()) {
            curPos = ctxt.realtimePos.toInt()

            playingPosView.progress = curPos

            val sec = curPos / 1000
            val curTime = String.format(Locale.US, "%d:%02d", sec / 60, sec % 60)
            playingCurTimeView.text = curTime
        }
    }

    private fun updateVolume(ctxt: PlayContext) {
	if (volume != ctxt.volume) {
	    volume = ctxt.volume
	    volumeView.progress = volume - VOLUME_BASE
	}
    }
    
    companion object {
	private val VOLUME_BASE = 50
    }
}
