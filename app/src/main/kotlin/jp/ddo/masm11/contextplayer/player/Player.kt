/* Context Player - Audio Player with Contexts
    Copyright (C) 2016, 2017 Yuuki Harano

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
package jp.ddo.masm11.contextplayer.player

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.media.MediaPlayer
import android.media.MediaTimestamp
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioFocusRequest
import android.net.Uri
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message

import jp.ddo.masm11.contextplayer.R
import jp.ddo.masm11.contextplayer.ui.MainActivity
import jp.ddo.masm11.contextplayer.ui.ExplorerActivity
import jp.ddo.masm11.contextplayer.fs.MFile

import jp.ddo.masm11.logger.Log

import java.util.Locale

class Player : Runnable {
    
    class CreatedMediaPlayer (val mediaPlayer: MediaPlayer, val path: String)
    
    private val OP_PLAY = 1
    private val OP_STOP = 2
    private val OP_SEEK = 3
    private val OP_SET_VOLUME = 4
    private val OP_SET_FILE = 5
    private val OP_SET_TOPDIR = 6
    private val OP_PREV = 7
    private val OP_NEXT = 8
    private val OP_TOGGLE = 9
    
    private lateinit var thr: Thread
    
    private lateinit var ctxt: Context
    private lateinit var attr: AudioAttributes
    private var aid: Int = 0
    
    private var topDir = "/"
    private var playingPath: String? = null
    private var nextPath: String? = null
    private var curPlayer: MediaPlayer? = null
    private var nextPlayer: MediaPlayer? = null
    private var volume: Int = 0
    private var volumeOnOff: Int = 0
    
    private var new_topDir = "/"
    private var new_playingPath: String? = null
    private var new_volume: Int = 0
    
    private var handler: Handler? = null
    
    inner class MyHandler: Handler() {
	override fun handleMessage(msg: Message) {
	    Log.d("handleMessage: ${msg.what}")
	    
	    when (msg.what) {
		OP_PLAY -> {
		    if (curPlayer == null) {
			val plr = MediaPlayer.create(ctxt, Uri.parse("file:///storage/7FFA-1D1A/Music/claris/best1/claris_best1_01.ogg"), null, attr, aid)
			plr.start()
			curPlayer = plr
		    }
		}
		OP_STOP -> {
		    val plr = curPlayer
		    curPlayer = null
		    if (plr != null)
			plr.pause()
		}
		OP_SEEK -> {
		    val plr = curPlayer
		    Log.d("pos=${msg.arg1}")
		    if (msg.arg1 != -1 && plr != null)
		        plr.seekTo(msg.arg1)
		}
		OP_SET_VOLUME -> {
		    val plr = curPlayer
		    if (plr != null)
			plr.setVolume(msg.arg1.toFloat() / 100.0f, msg.arg1.toFloat() / 100.0f)
		    volume = msg.arg1
		}
		OP_SET_FILE -> {
		    playingPath = msg.obj as String
		}
		OP_SET_TOPDIR -> {
		    topDir = msg.obj as String
		}
	    }
	}
    }
    
    override fun run() {
	while (true) {
	    Looper.prepare()
	    handler = MyHandler()
	    Looper.loop()
	}
    }
    
    fun play(path: String?) {
	val h = handler
	if (h != null) {
	    val msg = Message.obtain(h, OP_PLAY, path)
	    h.sendMessage(msg)
	}
    }
    
    fun stop() {
	val h = handler
	if (h != null) {
	    val msg = Message.obtain(h, OP_STOP)
	    h.sendMessage(msg)
	}
    }
    
    fun next() {
	val h = handler
	if (h != null) {
	    val msg = Message.obtain(h, OP_NEXT)
	    h.sendMessage(msg)
	}
    }
    
    fun prev() {
	val h = handler
	if (h != null) {
	    val msg = Message.obtain(h, OP_PREV)
	    h.sendMessage(msg)
	}
    }
    
    fun seek(pos: Int) {
	val h = handler
	if (h != null) {
	    val msg = Message.obtain(h, OP_SEEK, pos, 0)
	    h.sendMessage(msg)
	}
    }
    
    val currentPosition : Int
    get() {
	val plr = curPlayer
	if (plr != null)
	    return plr.currentPosition
	return 0
    }
    
    val duration: Int
    get() {
	val plr = curPlayer
	if (plr != null)
	    return plr.duration
	return 0
    }
    
    val isPlaying: Boolean
    get() {
	val plr = curPlayer
	if (plr != null)
	    return plr.isPlaying
	return false
    }
    
    fun setVolume(vol: Int) {
	val h = handler
	if (h != null) {
	    val msg = Message.obtain(h, OP_SET_VOLUME, vol, 0)
	    h.sendMessage(msg)
	}
    }
    
    fun setFile(path: String) {
	val h = handler
	if (h != null) {
	    val msg = Message.obtain(h, OP_SET_FILE, path)
	    h.sendMessage(msg)
	}
    }
    
    fun setTopDir(path: String) {
	val h = handler
	if (h != null) {
	    val msg = Message.obtain(h, OP_SET_TOPDIR, path)
	    h.sendMessage(msg)
	}
    }
    
    companion object {
	fun create(ctxt: Context, attr: AudioAttributes, aid: Int) : Player {
	    val p = Player()
	    p.thr = Thread(p)
	    p.ctxt = ctxt
	    p.attr = attr
	    p.aid = aid
	    p.thr.start()
	    while (p.handler == null)
	        Thread.sleep(100)
	    return p
	}
    }
}
