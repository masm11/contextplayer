/* Context Player - Audio Player with Contexts
    Copyright (C) 2016, 2019 Yuuki Harano

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
import android.app.IntentService
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri

import me.masm11.contextplayer.db.AppDatabase
import me.masm11.contextplayer.fs.MFile
import me.masm11.logger.Log

class PlayerService : IntentService("PlayerService") {
    private lateinit var db: AppDatabase
    
    private var curPlayer: MediaPlayer? = null
    private var nextPlayer: MediaPlayer? = null
    
    /*
    基本、IntentService として、
    Service 外からのリクエストは Intent として受け付けて、
    別スレッドで処理する。
    これで、画面が一瞬フリーズするのは避けられるはず。
    
    IntentService での Intent の処理は直列に行われるので、
    基本的には排他制御は不要。
    
    IntentService は、通常は、処理を終えたらすぐに終了する。
    onStartCommand をオーバーライドして、再生開始の場合は終了しないようにする。
    
    再生中に勝手に落ちないようにするのは、普通に、消えない通知で。
    
    status の broadcast はどうする? 再生時刻とか。
    
    各種 listener は、どのスレッドで呼ばれる?
    */
    
    inner class CurrentStatus {
	val contextId: Long = 0
	val path: String? = null
	val topDir: String? = null
	val duration: Int = 0
	val position: Int = 0
	val volume: Int = 0
    }
    
    override fun onCreate() {
	super.onCreate()

	db = AppDatabase.getDB()
	
/*
	notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
	val channel = NotificationChannel("notify_channel_1", getString(R.string.notification), NotificationManager.IMPORTANCE_LOW)
	notificationManager.createNotificationChannel(channel)
*/
	
/*
        statusChangedListeners = MutableWeakSet<(CurrentStatus) -> Unit>()
*/
	
/*
        headsetReceiver = HeadsetReceiver()
        registerReceiver(headsetReceiver, IntentFilter(AudioManager.ACTION_HEADSET_PLUG))
*/
	
/*
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
*/

	/* bluetooth headset への接続が切れたら、再生を停止する。
	* intent だとかなり遅延することがあるので、
	* 自前で BluetoothHeadset class で接続状況を監視する。
	* log がかなりうざい…
	*/
/*
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
*/

/*
        audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
*/
	curPlayer = null
	nextPlayer = null
	
/*
        val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange -> handleAudioFocusChangeEvent(focusChange) }
	
	val builder = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
	builder.setOnAudioFocusChangeListener(audioFocusChangeListener)
	builder.setAudioAttributes(audioAttributes)
	audioFocusRequest = builder.build()
*/
	
/*
        handler = Handler()
*/

/*
        volumeDuck = 100
*/
	
/*
        loadContext()
*/
    }
    
    /* 返り値を上書きしたい */
/*
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
	super.onStartCommand(intent, flags, startId)
        return Service.START_NOT_STICKY
    }
*/
    
    override fun onHandleIntent(intent: Intent) {
        val action = intent?.action
        Log.d("action=${action}")
        when (action) {
            ACTION_A2DP_DISCONNECTED -> {
/*
		saveContext()
		player.stop()
*/
	    }
            ACTION_HEADSET_UNPLUGGED -> {
/*
		saveContext()
		player.stop()
*/
	    }
            ACTION_TOGGLE -> {
/*
		saveContext()
		player.toggle()
*/
	    }
            ACTION_UPDATE_APPWIDGET -> {
/*
		updateAppWidget()
*/
	    }
	    ACTION_CURRENT_STATUS -> {
	    }
	    ACTION_SEEK -> {
	    }
	    ACTION_PLAY -> {
		val path = intent.getStringExtra(EXTRA_PATH)
		val local_path = MFile(path).file.absolutePath
		Log.i("local_path=${local_path}")
                val player = MediaPlayer.create(this, Uri.parse("file://${local_path}"))
		if (player == null) {
		    Log.i("player is null.")
		} else {
		    player.start()
		}
		curPlayer = player
	    }
	    ACTION_PAUSE -> {
	    }
	    ACTION_PREV -> {
	    }
	    ACTION_NEXT -> {
	    }
	    ACTION_SWITCH -> {
	    }
	    ACTION_SET_TOPDIR -> {
	    }
	    ACTION_SET_VOLUME -> {
	    }
        }
    }
    
    
    companion object {
        val ACTION_A2DP_DISCONNECTED = "me.masm11.contextplayer.A2DP_DISCONNECTED"
        val ACTION_HEADSET_UNPLUGGED = "me.masm11.contextplayer.HEADSET_UNPLUGGED"
        val ACTION_TOGGLE = "me.masm11.contextplayer.TOGGLE"
        val ACTION_UPDATE_APPWIDGET = "me.masm11.contextplayer.UPDATE_APPWIDGET"
	val ACTION_CURRENT_STATUS = "me.masm11.contextplayer.CURRENT_STATUS"
	val ACTION_SEEK = "me.masm11.contextplayer.SEEK"
	val ACTION_PLAY = "me.masm11.contextplayer.PLAY"
	val ACTION_PAUSE = "me.masm11.contextplayer.PAUSE"
	val ACTION_PREV = "me.masm11.contextplayer.PREV"
	val ACTION_NEXT = "me.masm11.contextplayer.NEXT"
	val ACTION_SWITCH = "me.masm11.contextplayer.SWITCH"
	val ACTION_SET_TOPDIR = "me.masm11.contextplayer.SET_TOPDIR"
	val ACTION_SET_VOLUME = "me.masm11.contextplayer.SET_VOLUME"

	val EXTRA_PATH = "me.masm11.contextplayer.PATH"

    }
}
