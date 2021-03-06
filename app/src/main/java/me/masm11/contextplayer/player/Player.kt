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
package me.masm11.contextplayer.player

import android.media.MediaPlayer
import android.media.AudioAttributes
import android.net.Uri
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message

import me.masm11.contextplayer.ui.ExplorerActivity
import me.masm11.contextplayer.fs.MFile

import me.masm11.logger.Log

import java.util.Locale

class Player : Runnable {
    
    class CreatedMediaPlayer (val mediaPlayer: MediaPlayer, val path: String)
    
    data class PlayArgs(val path: String?, val pos: Int, val start: Boolean)

    private val OP_PLAY = 1
    private val OP_STOP = 2
    private val OP_SEEK = 3
    private val OP_SET_VOLUME = 4
    private val OP_SET_TOPDIR = 6
    private val OP_PREV = 7
    private val OP_NEXT = 8
    private val OP_TOGGLE = 9
    private val OP_FINISH = 10
    
    private lateinit var thr: Thread
    
    private lateinit var ctxt: Context
    private lateinit var attr: AudioAttributes
    private var aid: Int = 0
    
    private var topDir = "/"
    var playingPath: String? = null
    get() = field
    private set(path) {
	field = path
    }
    private var nextPath: String? = null
    private var curPlayer: MediaPlayer? = null
    private var nextPlayer: MediaPlayer? = null
    private var volume: Int = 0
    
    private var handler: Handler? = null
    private lateinit var mainHandler: Handler
    
    inner class HandlerCallback: Handler.Callback {
	override fun handleMessage(msg: Message): Boolean {
	    Log.d("handleMessage: ${msg.what}")
	    
	    when (msg.what) {
		OP_PLAY ->		handle_play(msg)
		OP_STOP ->		handle_stop(msg)
		OP_SEEK ->		handle_seek(msg)
		OP_SET_VOLUME ->	handle_set_volume(msg)
		OP_SET_TOPDIR ->	handle_set_topdir(msg)
		OP_PREV ->		handle_prev(msg)
		OP_NEXT ->		handle_next(msg)
		OP_TOGGLE ->		handle_toggle(msg)
		OP_FINISH ->		handle_finish(msg)
		else -> return false
	    }
	    return true
	}
	
	private fun handle_play(msg: Message) {
	    /* 再生を開始する。
	    *  - path が指定されている場合:
	    *    → その path から開始し、再生できる曲を曲先頭から再生する。
	    *  - path が指定されていない場合:
	    *    - curPlayer != null の場合:
	    *      → curPlayer.play() する。
	    *    - curPlayer == null の場合:
	    *      - playingPath != null の場合:
	    *        → その path から開始し、再生できる曲を曲先頭から再生する。
	    *      - playingPath == null の場合:
	    *        → topDir 内で最初に再生できる曲を再生する。
	    */
	    val args = msg.obj as PlayArgs
	    val path = args.path
	    val pos = args.pos
	    val start = args.start
	    Log.i("path=${path}")
	    
	    Log.d("release nextPlayer")
	    releaseNextPlayer()
	    
	    if (path != null) {
		// path が指定された。
		// その path から開始し、再生できる曲を曲先頭から再生する。
		Log.d("path=${path}")
		
		Log.d("release curPlayer")
		releaseCurPlayer()
		
		Log.d("createMediaPlayer")
		val ret = createMediaPlayer(path, pos, false)
		if (ret == null) {
		    Log.w("No audio file found.")
		    return
		}
		Log.d("createMediaPlayer OK.")
		curPlayer = ret.mediaPlayer
		playingPath = ret.path
		Log.d("curPlayer=${curPlayer}")
		Log.d("playingPath=${playingPath}")
	    } else if (curPlayer != null) {
		// path が指定されてない && 再生途中だった
		// 再生再開
		Log.d("curPlayer exists. starting it.")
	    } else if (playingPath != null) {
		// path が指定されてない && 再生途中でない && context に playingPath がある
		// その path から開始し、再生できる曲を曲先頭から再生する。
		Log.d("playingPath=${playingPath}")
		
		Log.d("release nextPlayer")
		releaseCurPlayer()
		
		Log.d("creating mediaplayer.")
		val ret = createMediaPlayer(playingPath, pos, false)
		if (ret == null) {
		    Log.w("No audio file found.")
		    return
		}
		Log.d("creating mediaplayer OK.")
		curPlayer = ret.mediaPlayer
		playingPath = ret.path
		Log.d("curPlayer=${curPlayer}")
		Log.d("playingPath=${playingPath}")
	    } else {
		// 何もない
		// topDir 内から再生できる曲を探し、曲先頭から再生する。
		Log.d("none.")
		
		Log.d("release curPlayer.")
		releaseCurPlayer()
		
		Log.d("creating mediaplayer.")
		val ret = createMediaPlayer("", 0, false)
		if (ret == null) {
		    Log.w("No audio file found.")
		    return
		}
		Log.d("creating mediaplayer OK.")
		curPlayer = ret.mediaPlayer
		playingPath = ret.path
		Log.d("curPlayer=${curPlayer}")
		Log.d("playingPath=${playingPath}")
	    }
	    
	    if (start) {
		Log.d("starting.")
		setMediaPlayerVolume()
		startPlay()
		Log.d("enqueue next player.")
		enqueueNext()
	    }
	    callOneshotBroadcastListener()
	}
	
	private fun handle_stop(@Suppress("UNUSED_PARAMETER") msg: Message) {
	    /* 再生を一時停止する。
	    *  - curPlayer != null の場合
	    *    → pause() し、context を保存する
	    *  - curPlayer == null の場合
	    *    → 何もしない
	    */
	    Log.d("")
	    stopPlay()
	}
	
	private fun handle_seek(msg: Message) {
	    val plr = curPlayer
	    Log.d("pos=${msg.arg1}")
	    if (msg.arg1 != -1 && plr != null)
		plr.seekTo(msg.arg1)
	}
	
	private fun handle_set_volume(msg: Message) {
	    volume = msg.arg1
	    val vol = volume.toFloat() / 100.0f
	    curPlayer?.setVolume(vol, vol)
	    nextPlayer?.setVolume(vol, vol)
	}
	
	private fun handle_set_topdir(msg: Message) {
	    topDir = msg.obj as String
	    // 「次の曲」が変わる可能性があるので、enqueue しなおす。
	    if (curPlayer != null) {
		Log.d("enqueue next player.")
		enqueueNext()
	    }
	}
	
	private fun handle_prev(@Suppress("UNUSED_PARAMETER") msg: Message) {
	    val player: MediaPlayer? = curPlayer
	    if (player != null) {
		val pos = player.currentPosition
		if (pos >= 3 * 1000)
		    player.seekTo(0)
		else {
		    releaseNextPlayer()
		    releaseCurPlayer()
		    
		    val ret = createMediaPlayer(selectPrev(playingPath), 0, true)
		    if (ret == null) {
			Log.w("No audio file.")
			stopPlay()
		    } else {
			val mp = ret.mediaPlayer
			curPlayer = mp
			playingPath = ret.path
			setMediaPlayerVolume()
			mp.start()
			enqueueNext()
		    }
		}
	    }
	}
	
	private fun handle_next(@Suppress("UNUSED_PARAMETER") msg: Message) {
	    if (curPlayer != null) {
		releaseCurPlayer()
		
		playingPath = nextPath
		curPlayer = nextPlayer
		nextPath = null
		nextPlayer = null
		
		val plr = curPlayer
		if (plr != null) {
		    plr.start()
		    enqueueNext()
		}
	    }
	}
	
	private fun handle_toggle(@Suppress("UNUSED_PARAMETER") msg: Message) {
	    Log.d("")
	    val plr = curPlayer
	    if (plr != null && plr.isPlaying)
		stopPlay()
	    else
		play(null)
	}
	
	private fun handle_finish(@Suppress("UNUSED_PARAMETER") msg: Message) {
	    Looper.myLooper()?.quitSafely()
	    stopPlay()
	}
    }
    
    override fun run() {
	Looper.prepare()
	handler = Handler(HandlerCallback())
	Looper.loop()
	Log.d("thread exiting.")
    }
    
    fun finish() {
	val h = handler
	if (h != null) {
	    val msg = Message.obtain(h, OP_FINISH)
	    h.sendMessage(msg)
	}
	thr.join()
    }
    
    fun play(path: String?) {
	val h = handler
	if (h != null) {
	    val args = PlayArgs(path, 0, true)
	    val msg = Message.obtain(h, OP_PLAY, args)
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
    
    fun toggle() {
	val h = handler
	if (h != null) {
	    val msg = Message.obtain(h, OP_TOGGLE)
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
    
    val currentPosition: Int
    get() {
	val plr = curPlayer
	if (plr != null) {
	    try {
		return plr.currentPosition
	    } catch (e: IllegalStateException) {
		// return 0
	    }
	}
	return 0
    }
    
    val duration: Int
    get() {
	val plr = curPlayer
	if (plr != null) {
	    try {
		return plr.duration
	    } catch (e: IllegalStateException) {
		// return 0
	    }
	}
	return 0
    }
    
    val isPlaying: Boolean
    get() {
	val plr = curPlayer
	if (plr != null) {
	    try {
		return plr.isPlaying
	    } catch (e: IllegalStateException) {
		// return false
	    }
	}
	return false
    }
    
    fun setVolume(vol: Int) {
	val h = handler
	if (h != null) {
	    val msg = Message.obtain(h, OP_SET_VOLUME, vol, 0)
	    h.sendMessage(msg)
	}
    }
    
    fun setFile(path: String?, pos: Int) {
	val h = handler
	if (h != null) {
	    val args = PlayArgs(path, pos, false)
	    val msg = Message.obtain(h, OP_PLAY, args)
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

    // curPlayer がセットされた状態で呼ばれ、
    // 再生を start する。
    private fun startPlay() {
        if (curPlayer != null) {
            Log.d("request audio focus.")
            callAudioFocusRequestListener(true)

            setMediaPlayerVolume()

            try {
                Log.d("starting.")
                curPlayer?.start()
            } catch (e: Exception) {
                Log.e("exception", e)
            }

            Log.d("set to foreground")
            callSetForegroundListener(true)

            callStartBroadcastListener(true)

            callUpdateAppWidgetListener()

            callSaveContextListener()
        }
    }

    private fun stopPlay() {
        try {
            callStartBroadcastListener(false)

            Log.d("set to non-foreground")
            callSetForegroundListener(false)

            setMediaPlayerVolume()

	    val plr = curPlayer
            if (plr != null) {
                /* paused から pause() は問題ないが、
		 * prepared から pause() は正しくないみたい。
		 */
                if (plr.isPlaying) {
                    Log.d("pause ${plr}")
                    plr.pause()
                } else
                    Log.d("already paused ${plr}")
            }

            callUpdateAppWidgetListener()

            Log.d("abandon audio focus.")
            callAudioFocusRequestListener(false)

            Log.d("save context")
            callSaveContextListener()
        } catch (e: Exception) {
            Log.e("exception", e)
        }
    }
    
    private fun setMediaPlayerVolume() {
	val vol = volume.toFloat() / 100.0f
        curPlayer?.setVolume(vol, vol)
        nextPlayer?.setVolume(vol, vol)
    }
    
    private fun enqueueNext() {
        Log.d("release nextPlayer")
        releaseNextPlayer()

        Log.d("creating mediaplayer")
        val ret = createMediaPlayer(selectNext(playingPath), 0, false)
        if (ret == null) {
            Log.w("No audio file found.")
            return
        }
        Log.d("creating mediaplayer OK.")
        nextPlayer = ret.mediaPlayer
        nextPath = ret.path
        setMediaPlayerVolume()
        Log.d("nextPlayer=${nextPlayer}")
        Log.d("nextPath=${nextPath}")
        try {
            Log.d("setting it as nextmediaplayer.")
            curPlayer?.setNextMediaPlayer(nextPlayer)
        } catch (e: Exception) {
            Log.e("exception", e)
        }

    }

    private fun createMediaPlayer(startPath: String?, startPos: Int, back: Boolean): CreatedMediaPlayer? {
        var path = startPath
        var pos = startPos
        Log.d("path=${path}")
        Log.d("pos=${pos}")
        var tested = emptySet<String>()
        while (true) {
            Log.d("iter")
            try {
                Log.d("path=${path}")
                if (path == null || tested.contains(path)) {
                    // 再生できるものがない…
                    Log.d("No audio file.")
                    return null
                }
                tested = tested + path

                Log.d("try create mediaplayer.")
                val player = MediaPlayer.create(ctxt, Uri.parse("file://${MFile(path).file.absolutePath}"), null, attr, aid)
                if (player == null) {
                    Log.w("MediaPlayer.create() failed: ${path}")
                    path = if (back) selectPrev(path) else selectNext(path)
                    pos = 0    // お目当てのファイルが見つからなかった。次のファイルの先頭からとする。
                    continue
                }
                Log.d("create mediaplayer ok.")
                if (pos > 0) {    // 0 の場合に seekTo() すると、曲の頭が切れるみたい?
                    Log.d("seek to ${pos}")
                    player.seekTo(pos)
                }
                player.setOnCompletionListener { mp ->
                    Log.d("shifting")
                    playingPath = nextPath
                    curPlayer = nextPlayer
                    Log.d("now playingPath=${playingPath}")
                    Log.d("now curPlayer=${curPlayer}")
                    Log.d("releasing ${mp}")
                    mp.release()
                    Log.d("clearing nextPath/nextPlayer")
                    nextPath = null
                    nextPlayer = null

                    callSaveContextListener()

                    if (curPlayer != null) {
                        Log.d("enqueue next mediaplayer.")
                        enqueueNext()
                    } else
                        stopPlay()
                }
                player.setOnErrorListener(MediaPlayer.OnErrorListener { _, what, extra ->
                    Log.d("error reported. ${what}, ${extra}.")

                    // 両方 release して新たに作り直す。
                    releaseNextPlayer()
                    releaseCurPlayer()

                    Log.d("creating mediaplayer.")
                    val ret = createMediaPlayer(nextPath, 0, false)
                    if (ret == null) {
                        Log.w("No audio file found.")
                        stopPlay()
                        return@OnErrorListener true
                    }

                    curPlayer = ret.mediaPlayer
                    playingPath = ret.path
                    setMediaPlayerVolume()

                    Log.d("starting it.")
                    curPlayer?.start()

                    Log.d("enqueuing next.")
                    enqueueNext()
                    setMediaPlayerVolume()

                    callSaveContextListener()

                    true
                })

                Log.d("done. player=${player}, path=${path}")
                return CreatedMediaPlayer(player, path)
            } catch (e: Exception) {
                Log.e("exception", e)
            }

            return null
        }
    }

    private fun releaseCurPlayer() {
        Log.d("")
        try {
            Log.d("releasing...")
            curPlayer?.release()
            Log.d("releasing... ok")
            curPlayer = null
        } catch (e: Exception) {
            Log.e("exception", e)
        }

    }

    private fun releaseNextPlayer() {
        Log.d("")
        try {
            Log.d("releasing...")
            nextPlayer?.release()
            Log.d("releasing... ok")
            nextPlayer = null
        } catch (e: Exception) {
            Log.e("exception", e)
        }

    }

    private fun selectNext(nextOf: String?): String? {
	if (nextOf == null)
	    return null;
        Log.d("nextOf=${nextOf}")
        var found: String? = null
        if (nextOf.startsWith(topDir)) {
	    if (topDir != "//") {
		//                           +1: for '/'   ↓
		val parts = nextOf.substring(topDir.length + 1).split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		found = lookForFile(MFile(topDir), parts, 0, false)
	    } else {
		val parts = nextOf.substring(2).split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		found = lookForFile(MFile(topDir), parts, 0, false)
	    }
        }
        if (found == null)
            found = lookForFile(MFile(topDir), null, 0, false)
        Log.d("found=${found}")
        return found
    }

    private fun selectPrev(prevOf: String?): String? {
	if (prevOf == null)
	    return null;
        Log.d("prevOf=${prevOf}")
        var found: String? = null
        if (prevOf.startsWith(topDir)) {
	    if (topDir != "//") {
		//                            +1: for '/'  ↓
		val parts = prevOf.substring(topDir.length + 1).split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		found = lookForFile(MFile(topDir), parts, 0, true)
	    } else {
		val parts = prevOf.substring(2).split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		found = lookForFile(MFile(topDir), parts, 0, true)
	    }
        }
        if (found == null)
            found = lookForFile(MFile(topDir), null, 0, true)
        Log.d("found=${found}")
        return found
    }

    /* 次のファイルを探す。
     *   dir: 今見ているディレクトリ
     *   parts[]: topdir からの相対 path。'/' で区切られている。
     *   parts_idx: ディレクトリの nest。
     *   backward: 逆向き検索。
     * 最初にこのメソッドに来る時、nextOf は、
     *   /dir/parts[0]/parts[1]/…/parts[N]
     * だったことになる。
     * lookForFile() の役割は、dir 内 subdir も含めて、nextOf の次のファイルを探すこと。
     * parts == null の場合、nextOf の path tree から外れた場所を探している。
     */
    private fun lookForFile(dir: MFile, parts: Array<String>?, parts_idx: Int, backward: Boolean): String? {
        var cur: String? = null
        if (parts != null) {
            if (parts_idx < parts.size)
                cur = parts[parts_idx]
        }

        val files = ExplorerActivity.listFiles(dir, backward)

        for (file in files) {
            if (cur == null) {
                if (file.isDirectory) {
                    val r = lookForFile(file, null, parts_idx + 1, backward)
                    if (r != null)
                        return r
                } else {
                    return file.absolutePath
                }
            } else {
                val compare = comparePath(file.name, cur)
                if (compare == 0) {
                    // 今そこ。
                    if (file.isDirectory) {
                        val r = lookForFile(file, parts, parts_idx + 1, backward)
                        if (r != null)
                            return r
                    } else {
                        // これは今再生中。
                    }
                } else if (!backward && compare > 0) {
                    if (file.isDirectory) {
                        // 次を探していたら dir だった
                        val r = lookForFile(file, null, parts_idx + 1, backward)
                        if (r != null)
                            return r
                    } else {
                        // 次のファイルを見つけた
                        return file.absolutePath
                    }
                } else if (backward && compare < 0) {
                    if (file.isDirectory) {
                        // 次を探していたら dir だった
                        val r = lookForFile(file, null, parts_idx + 1, backward)
                        if (r != null)
                            return r
                    } else {
                        // 次のファイルを見つけた
                        return file.absolutePath
                    }
                }
            }
        }
	
        return null
    }
    
    private fun comparePath(p1: String, p2: String): Int {
        val l1 = p1.toLowerCase(Locale.getDefault())
        val l2 = p2.toLowerCase(Locale.getDefault())
        var r = l1.compareTo(l2)
        if (r == 0)
            r = p1.compareTo(p2)
        return r
    }
    
    private fun callSaveContextListener() {
	mainHandler.post {
	    saveContextListener()
	}
    }
    
    private fun callSetForegroundListener(onoff: Boolean) {
	mainHandler.post {
	    setForegroundListener(onoff)
	}
    }
    
    private fun callStartBroadcastListener(onoff: Boolean) {
	mainHandler.post {
	    startBroadcastListener(onoff)
	}
    }
    
    private fun callUpdateAppWidgetListener() {
	mainHandler.post {
	    updateAppWidgetListener()
	}
    }
    
    private fun callAudioFocusRequestListener(onoff: Boolean) {
	mainHandler.post {
	    audioFocusRequestListener(onoff)
	}
    }
    
    private fun callOneshotBroadcastListener() {
	mainHandler.post {
	    oneshotBroadcastListener()
	}
    }
    
    private lateinit var saveContextListener: () -> Unit
    private lateinit var setForegroundListener: (Boolean) -> Unit
    private lateinit var startBroadcastListener: (Boolean) -> Unit
    private lateinit var updateAppWidgetListener: () -> Unit
    private lateinit var audioFocusRequestListener: (Boolean) -> Unit
    private lateinit var oneshotBroadcastListener: () -> Unit
    
    companion object {
	fun create(ctxt: Context, attr: AudioAttributes, aid: Int,
		   saveContextListener: () -> Unit,
		   setForegroundListener: (Boolean) -> Unit,
		   startBroadcastListener: (Boolean) -> Unit,
		   updateAppWidgetListener: () -> Unit,
		   audioFocusRequestListener: (Boolean) -> Unit,
		   oneshotBroadcastListener: () -> Unit) : Player {
	    val p = Player()
	    p.mainHandler = Handler()
	    p.ctxt = ctxt
	    p.attr = attr
	    p.aid = aid
	    p.saveContextListener = saveContextListener
	    p.setForegroundListener = setForegroundListener
	    p.startBroadcastListener = startBroadcastListener
	    p.updateAppWidgetListener = updateAppWidgetListener
	    p.audioFocusRequestListener = audioFocusRequestListener
	    p.oneshotBroadcastListener = oneshotBroadcastListener
	    p.thr = Thread(p)
	    p.thr.start()
	    while (p.handler == null)
	        Thread.sleep(100)
	    return p
	}
    }
}
