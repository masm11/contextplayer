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
package me.masm11.contextplayer.service

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.media.MediaPlayer
import android.media.MediaTimestamp
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioFocusRequest
import android.net.Uri
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.os.IBinder
import android.os.Handler
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import androidx.localbroadcastmanager.content.LocalBroadcastManager

import java.util.Locale

// https://kotlinlang.org/docs/reference/coroutines/basics.html
import kotlinx.coroutines.*

import me.masm11.contextplayer.R
import me.masm11.contextplayer.ui.MainActivity
import me.masm11.contextplayer.ui.ExplorerActivity
import me.masm11.contextplayer.fs.MFile
import me.masm11.contextplayer.receiver.HeadsetReceiver
import me.masm11.contextplayer.db.AppDatabase
import me.masm11.contextplayer.db.PlayContext
import me.masm11.contextplayer.db.PlayContextList
import me.masm11.contextplayer.widget.WidgetProvider
import me.masm11.contextplayer.player.Player
import me.masm11.contextplayer.Application

import me.masm11.logger.Log

class PlayerService : Service(), CoroutineScope by MainScope() {

    class CreatedMediaPlayer (val mediaPlayer: MediaPlayer, val path: String)
    
    private lateinit var playContexts: PlayContextList
    private lateinit var curContext: PlayContext
    private var topDir: String = "//"
    private lateinit var player: Player
    private lateinit var audioManager: AudioManager
    private lateinit var audioAttributes: AudioAttributes
    private var audioSessionId: Int = 0
    private lateinit var audioFocusRequest: AudioFocusRequest
    private var broadcaster: Job? = null
    private lateinit var headsetReceiver: BroadcastReceiver
    private var volume: Int = 0
    private var volumeDuck: Int = 0
    private var volumeOnOff: Int = 0
    private lateinit var handler: Handler
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothHeadset: BluetoothHeadset? = null
    private lateinit var headsetMonitor: Thread
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private var isForeground = false
    private var promotor: Job? = null
    private var prevJob: Job? = null

    override fun onCreate() {
	handler = Handler()

	playContexts = (getApplication() as Application).getPlayContextList()
	curContext = playContexts.getCurrent()
	
	localBroadcastManager = LocalBroadcastManager.getInstance(this)
	
	val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
	val channel_1 = NotificationChannel("notify_channel_1", getString(R.string.notification), NotificationManager.IMPORTANCE_LOW)
	notificationManager.createNotificationChannel(channel_1)
	val channel_2 = NotificationChannel("notify_channel_2", getString(R.string.notification), NotificationManager.IMPORTANCE_LOW)
	notificationManager.createNotificationChannel(channel_2)
	
	headsetReceiver = HeadsetReceiver()
	registerReceiver(headsetReceiver, IntentFilter(AudioManager.ACTION_HEADSET_PLUG))
	
	val ba = BluetoothAdapter.getDefaultAdapter()
	bluetoothAdapter = ba
	if (ba != null) {
	    ba.getProfileProxy(this, object: BluetoothProfile.ServiceListener {
		override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
		    if (profile == BluetoothProfile.HEADSET) {
			Log.i("Connected to bluetooth headset proxy.")
			bluetoothHeadset = proxy as BluetoothHeadset
		    }
		}
		override fun onServiceDisconnected(profile: Int) {
		    if (profile == BluetoothProfile.HEADSET) {
			Log.i("Disconnected from bluetooth headset proxy.")
			bluetoothHeadset = null
		    }
		}
	    }, BluetoothProfile.HEADSET)
	}
	
	/* bluetooth headset への接続が切れたら、再生を停止する。
	* intent だとかなり遅延することがあるので、
	* 自前で BluetoothHeadset class で接続状況を監視する。
	* log がかなりうざい…
	*/
	val code = Runnable {
	    var connected = false
	    try {
		while (true) {
		    val headset = bluetoothHeadset
		    var newConnected = false
		    if (headset != null) {
			val devices = headset.getConnectedDevices()
			if (devices.size >= 1)
			    newConnected = true
		    }
		    if (connected != newConnected) {
			Log.d("bluetooth headset: ${connected} -> ${newConnected}")
			connected = newConnected
			if (!connected)
			    handler.post { saveContext(); player.stop() }
		    }
		    Thread.sleep(500)
		}
	    } catch (e: InterruptedException) {
		Log.d("interrupted.", e)
	    }
	}
	headsetMonitor = Thread(code)
	headsetMonitor.start()


	audioAttributes = AudioAttributes.Builder()
	    .setUsage(AudioAttributes.USAGE_MEDIA)
	    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
	    .build()
	audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
	audioSessionId = audioManager.generateAudioSessionId()
	player = Player.create(
	    this, audioAttributes, audioSessionId, {
		-> saveContext()
	    }, {
		onoff -> setForeground(onoff)
	    }, {
		onoff -> if (onoff) startBroadcast() else stopBroadcast()
	    }, {
		-> updateAppWidget()
	    }, {
		onoff -> if (onoff) audioManager.requestAudioFocus(audioFocusRequest) else audioManager.abandonAudioFocusRequest(audioFocusRequest)
	    }, {
		-> broadcastStatus()
	    }
	)
	
	val builder = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
	builder.setOnAudioFocusChangeListener { focusChange -> handleAudioFocusChangeEvent(focusChange) }
	builder.setAudioAttributes(audioAttributes)
	audioFocusRequest = builder.build()
	
	volumeDuck = 100
	
	loadContext()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
	val action = intent?.action
	Log.d("action=${action}")
	
	when (action) {
	    ACTION_A2DP_DISCONNECTED -> {
		saveContext()
		player.stop()
	    }
	    ACTION_HEADSET_UNPLUGGED -> {
		saveContext()
		player.stop()
	    }
	    ACTION_TOGGLE -> {
		saveContext()
		player.toggle()
	    }
	    ACTION_UPDATE_APPWIDGET -> updateAppWidget()

	    ACTION_PLAY -> {
		val path: String? = intent.getStringExtra(EXTRA_PATH)
		player.play(path)
	    }
	    ACTION_PAUSE -> {
		saveContext()
		player.stop()
	    }
	    ACTION_SET_TOPDIR -> {
		val topDir = intent.getStringExtra(EXTRA_TOPDIR)
		setTopDir(topDir)
	    }
	    ACTION_SET_VOLUME -> {
		val volume = intent.getIntExtra(EXTRA_VOLUME, 0)
		setVolume(volume)
	    }
	    ACTION_SWITCH_CONTEXT -> {
		switchContext()
	    }
	    ACTION_SEEK ->  {
		val pos = intent.getIntExtra(EXTRA_POS, 0)
		player.seek(pos)
	    }
	    ACTION_PREV_TRACK -> {
		player.prev()
	    }
	    ACTION_NEXT_TRACK -> {
		player.next()
	    }
	    ACTION_REQUEST_CURRENT_STATUS -> {
		broadcastStatus()
	    }
	}

	/*
	* background から background service を起動することはできない。
	* なので、startForegroundService() を使う。
	* しかしこれを使うと startForeground() を呼ばないといけなくなる。
	* 呼べばいいのだけど、volume の変更とか連続した処理に毎回呼ぶのは
	* 無駄なので、1秒おきとする。
	*/
	val p = promotor
	if (p != null && !p.isActive) {
	    Log.d("promotor.join")
	    runBlocking {
		p.join()
	    }
	    Log.d("promotor.join done.")
	    promotor = null
	}
	if (promotor == null) {
	    Log.d("enqueue promotor")
	    promotor = launch {
		delay(1000)
		setForegroundJustInstant()
	    }
	}

	return Service.START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent): IBinder? {
	return null
    }
    
    private fun setMediaPlayerVolume() {
	val vol = ((volume * volumeDuck * volumeOnOff).toFloat() / 100.0f / 100.0f).toInt()
	player.setVolume(vol)
    }
    
    private fun handleAudioFocusChangeEvent(focusChange: Int) {
	Log.d("focusChange=${focusChange}")
	when (focusChange) {
	    AudioManager.AUDIOFOCUS_GAIN -> {
		volumeDuck = 100
		setMediaPlayerVolume()
	    }
	    AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
		saveContext()
		player.stop()
	    }
	    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
		volumeDuck = 25
		setMediaPlayerVolume()
	    }
	}
    }
    
    /* topDir を変更する。
    *    topDir を設定し、enqueueNext() し直す。
    */
    private fun setTopDir(path: String) {
	Log.d("path=${path}")
	topDir = path
	player.setTopDir(path)
    }
    
    /* context を switch する。
    * 今再生中なら pause() し、context を保存する。
    * context を読み出し、再生を再開する。
    */
    private fun switchContext() {
	Log.d("save context.")
	saveContext()

        Log.d("player stopping.")
        player.stop()

        Log.d("load context.")
        loadContext()

        Log.d("start playing.")
        player.play(null)
    }
    
    private fun setForeground(on: Boolean) {
	Log.d("on=${on}")
	if (on) {
	    volumeOnOff = 100		// fixme: 仮でこの場所に
	    setMediaPlayerVolume()
	    
	    val ctxt = curContext
	    var contextName = ctxt.name
	    val builder = Notification.Builder(this, "notify_channel_1")
	    builder.setStyle(Notification.BigTextStyle()
		.bigText(resources.getString(R.string.playing, contextName)))
	    builder.setSmallIcon(R.drawable.notification)
	    val intent = Intent(this, MainActivity::class.java)
	    val tsBuilder = TaskStackBuilder.create(this)
	    tsBuilder.addParentStack(MainActivity::class.java)
	    tsBuilder.addNextIntent(intent)
	    val pendingIntent = tsBuilder.getPendingIntent(
		0, PendingIntent.FLAG_UPDATE_CURRENT
	    )
	    builder.setContentIntent(pendingIntent)
	    startForeground(1, builder.build())
	} else {
	    volumeOnOff = 0
	    setMediaPlayerVolume()
	    
	    stopForeground(true)
	}
	isForeground = on
    }
    
    private fun setForegroundJustInstant() {
	Log.d("")
	
	/* 既に foreground になってる場合、
	* stopForeground すると本当に foreground でなくなってしまう。
	* ので、その場合はこの処理をしないことにする。
	*/
	if (isForeground)
	    return
	
	val builder = Notification.Builder(this, "notify_channel_2")
	builder.setSmallIcon(R.drawable.notification)
	val intent = Intent(this, MainActivity::class.java)
	val tsBuilder = TaskStackBuilder.create(this)
	tsBuilder.addParentStack(MainActivity::class.java)
	tsBuilder.addNextIntent(intent)
	val pendingIntent = tsBuilder.getPendingIntent(
	    0, PendingIntent.FLAG_UPDATE_CURRENT
	)
	builder.setContentIntent(pendingIntent)
	startForeground(1, builder.build())
	
	stopForeground(true)
    }

    private fun saveContext() {
	Log.d("contextId=----")
	val ctxt = curContext
	Log.d("Id=${ctxt.uuid}")
	ctxt.path = player.playingPath
	Log.d("path=${ctxt.path}")
	ctxt.pos = player.currentPosition.toLong()
	Log.d("pos=${ctxt.pos}")
	ctxt.volume = volume
	Log.d("volume=${volume}")
	Log.d("ctxt saving...")
	playContexts.put(ctxt.uuid)
    }
    
    private fun loadContext() {
        Log.d("getting context")
        val ctxt = playContexts.getCurrent()
        val path = ctxt.path
        topDir = ctxt.topDir
        volume = ctxt.volume
        Log.d("path=${path}")
        Log.d("topDir=${topDir}")
        player.setTopDir(topDir)
        player.setFile(path, ctxt.pos.toInt())
        setMediaPlayerVolume()
    }
    
    private fun startBroadcast() {
	Log.d("enter.")
	
	Log.d("stop broadcast.")
	stopBroadcast()    // 念の為
	
	Log.d("starting thread.")
	broadcaster = GlobalScope.launch {
	    while (true) {
		Log.d("broadcast.")
		broadcastStatus()
		Log.d("delay.")
		delay(500)
		Log.d("delay. done.")
	    }
	}
	
	Log.d("leave.")
    }
    
    private fun stopBroadcast() {
	val job = broadcaster
	if (job != null) {
	    broadcaster = null
	    
	    Log.d("interrupt&joining...")
	    runBlocking {
		job.cancelAndJoin()
	    }
	    Log.d("joining... done")
	}
    }
    
    private fun broadcastStatus() {
	val ctxt = curContext
	Log.d("${player.playingPath}")
	val intent = Intent(ACTION_CURRENT_STATUS)
	    .putExtra(EXTRA_CONTEXT_ID, ctxt.uuid)
	    .putExtra(EXTRA_PATH, player.playingPath)
	    .putExtra(EXTRA_TOPDIR, topDir)
	    .putExtra(EXTRA_VOLUME, volume)
	intent.putExtra(EXTRA_POS, player.currentPosition)
	intent.putExtra(EXTRA_DURATION, player.duration)
	localBroadcastManager.sendBroadcast(intent)
    }
    
    override fun onDestroy() {
	Log.d("save context")
	saveContext()
	
	Log.d("stopping player.")
	player.stop()

	headsetMonitor.interrupt()
	headsetMonitor.join()

	if (bluetoothHeadset != null) {
	    val ba = bluetoothAdapter
	    if (ba != null)
		ba.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset)
	}
	
	unregisterReceiver(headsetReceiver)
    }
    
    private fun updateAppWidget() {
	val ctxt = curContext
	val contextName = ctxt.name
	
	var icon = android.R.drawable.ic_media_play
	if (player.isPlaying)
	    icon = android.R.drawable.ic_media_pause
	
	WidgetProvider.updateAppWidget(this, null, icon, contextName)
    }
    
    private fun setVolume(volume: Int) {
	this.volume = volume
	setMediaPlayerVolume()
    }
    
    companion object {
	
	private fun selectNext(nextOf: String?, topDir: String): String? {
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
	
	private fun selectPrev(prevOf: String?, topDir: String): String? {
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
	
	val ACTION_A2DP_DISCONNECTED = "me.masm11.contextplayer.A2DP_DISCONNECTED"
	val ACTION_HEADSET_UNPLUGGED = "me.masm11.contextplayer.HEADSET_UNPLUGGED"
	val ACTION_TOGGLE = "me.masm11.contextplayer.TOGGLE"
	val ACTION_UPDATE_APPWIDGET = "me.masm11.contextplayer.UPDATE_APP_WIDGET"
	
	val ACTION_PLAY = "me.masm11.contextplayer.PLAY"
	val ACTION_PAUSE = "me.masm11.contextplayer.PAUSE"
	val ACTION_SET_TOPDIR = "me.masm11.contextplayer.SET_TOPDIR"
	val ACTION_SET_VOLUME = "me.masm11.contextplayer.SET_VOLUME"
	val ACTION_SWITCH_CONTEXT = "me.masm11.contextplayer.SWICH_CONTEXT"
	val ACTION_SEEK = "me.masm11.contextplayer.SEEK"
	val ACTION_PREV_TRACK = "me.masm11.contextplayer.PREV_TRACK"
	val ACTION_NEXT_TRACK = "me.masm11.contextplayer.NEXT_TRACK"
	val ACTION_REQUEST_CURRENT_STATUS = "me.masm11.contextplayer.REQUEST_CURRENT_STATUS"
	val ACTION_CURRENT_STATUS = "me.masm11.contextplayer.CURRENT_STATUS"
	
	val EXTRA_POS = "me.masm11.contextplayer.POS"
	val EXTRA_PATH = "me.masm11.contextplayer.PATH"
	val EXTRA_VOLUME = "me.masm11.contextplayer.VOLUME"
	val EXTRA_TOPDIR = "me.masm11.contextplayer.TOPDIR"
	val EXTRA_CONTEXT_ID = "me.masm11.contextplayer.CONTEXT_ID"
	val EXTRA_DURATION = "me.masm11.contextplayer.DURATION"
	
	fun play(ctxt: Context, path: String?) {
	    val intent = Intent(ctxt, PlayerService::class.java)
	    intent.action = ACTION_PLAY
	    intent.putExtra(EXTRA_PATH, path)
	    ctxt.startForegroundService(intent)
	}
	
	fun pause(ctxt: Context) {
	    val intent = Intent(ctxt, PlayerService::class.java)
	    intent.action = ACTION_PAUSE
	    ctxt.startForegroundService(intent)
	}
	
	fun prevTrack(ctxt: Context) {
	    val intent = Intent(ctxt, PlayerService::class.java)
	    intent.action = ACTION_PREV_TRACK
	    ctxt.startForegroundService(intent)
	}
	
	fun nextTrack(ctxt: Context) {
	    val intent = Intent(ctxt, PlayerService::class.java)
	    intent.action = ACTION_NEXT_TRACK
	    ctxt.startForegroundService(intent)
	}
	
	fun seek(ctxt: Context, pos: Int) {
	    val intent = Intent(ctxt, PlayerService::class.java)
	    intent.action = ACTION_SEEK
	    intent.putExtra(EXTRA_POS, pos)
	    ctxt.startForegroundService(intent)
	}
	
	fun setVolume(ctxt: Context, volume: Int) {
	    val intent = Intent(ctxt, PlayerService::class.java)
	    intent.action = ACTION_SET_VOLUME
	    intent.putExtra(EXTRA_VOLUME, volume)
	    ctxt.startForegroundService(intent)
	}
	
	fun setTopDir(ctxt: Context, topDir: String) {
	    val intent = Intent(ctxt, PlayerService::class.java)
	    intent.action = ACTION_SET_TOPDIR
	    intent.putExtra(EXTRA_TOPDIR, topDir)
	    ctxt.startForegroundService(intent)
	}
	
	fun switchContext(ctxt: Context) {
	    val intent = Intent(ctxt, PlayerService::class.java)
	    intent.action = ACTION_SWITCH_CONTEXT
	    ctxt.startForegroundService(intent)
	}
	
	fun requestCurrentStatus(ctxt: Context) {
	    val intent = Intent(ctxt, PlayerService::class.java)
	    intent.action = ACTION_REQUEST_CURRENT_STATUS
	    ctxt.startForegroundService(intent)
	}
    }
}
