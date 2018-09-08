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
package jp.ddo.masm11.contextplayer.service

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
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.os.IBinder
import android.os.Binder
import android.os.Handler
import android.appwidget.AppWidgetManager
import android.widget.RemoteViews
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset

import java.util.Locale

import jp.ddo.masm11.contextplayer.R
import jp.ddo.masm11.contextplayer.ui.MainActivity
import jp.ddo.masm11.contextplayer.ui.ExplorerActivity
import jp.ddo.masm11.contextplayer.util.WeakSet
import jp.ddo.masm11.contextplayer.util.MutableWeakSet
import jp.ddo.masm11.contextplayer.fs.MFile
import jp.ddo.masm11.contextplayer.receiver.HeadsetReceiver
import jp.ddo.masm11.contextplayer.db.AppDatabase
import jp.ddo.masm11.contextplayer.db.PlayContext
import jp.ddo.masm11.contextplayer.db.Config
import jp.ddo.masm11.contextplayer.widget.WidgetProvider
import jp.ddo.masm11.contextplayer.player.Player

import jp.ddo.masm11.logger.Log

class PlayerService : Service() {

    class CurrentStatus(
            val contextId: Long,
            val path: String?,
            val topDir: String,
            val position: Int,
            val duration: Int,
	    val volume: Int)
    
    private fun buildCurrentStatus(): CurrentStatus {
	val player: MediaPlayer? = curPlayer
	return CurrentStatus(
		contextId,
		playingPath,
		topDir,
		if (player == null) 0 else player.currentPosition,
		if (player == null) 0 else player.duration,
		volume)
    }
    
    class CreatedMediaPlayer (val mediaPlayer: MediaPlayer, val path: String)
    
    interface OnStatusChangedListener {
        fun onStatusChanged(status: CurrentStatus)
    }
    
    private lateinit var db: AppDatabase
    private var topDir: String = "/"
    private var playingPath: String? = null
    private var nextPath: String? = null
    private var curPlayer: MediaPlayer? = null
    private var nextPlayer: MediaPlayer? = null
    private var contextId: Long = 0
    private lateinit var audioManager: AudioManager
    private lateinit var audioAttributes: AudioAttributes
    private var audioSessionId: Int = 0
    private lateinit var audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener
    private lateinit var audioFocusRequest: AudioFocusRequest
    private var broadcaster: Thread? = null
    private lateinit var handler: Handler
    private lateinit var statusChangedListeners: MutableSet<OnStatusChangedListener>
    private lateinit var headsetReceiver: BroadcastReceiver
    private var volume: Int = 0
    private var volumeDuck: Int = 0
    private var volumeOnOff: Int = 0
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothHeadset: BluetoothHeadset? = null
    private lateinit var headsetMonitor: Thread
    private lateinit var notificationManager: NotificationManager
    private lateinit var player: Player

    fun setOnStatusChangedListener(listener: OnStatusChangedListener) {
        Log.d("listener=${listener}")
        statusChangedListeners.add(listener)
    }

    override fun onCreate() {
	db = AppDatabase.getDB()
	
	notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
	val channel = NotificationChannel("notify_channel_1", getString(R.string.notification), NotificationManager.IMPORTANCE_LOW)
	notificationManager.createNotificationChannel(channel)

        statusChangedListeners = MutableWeakSet<OnStatusChangedListener>()

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
			    handler.post { player.stop() }
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
	player = Player.create(this, audioAttributes, audioSessionId)

        val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange -> handleAudioFocusChangeEvent(focusChange) }

	val builder = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
	builder.setOnAudioFocusChangeListener(audioFocusChangeListener)
	builder.setAudioAttributes(audioAttributes)
	audioFocusRequest = builder.build()

        handler = Handler()

        volumeDuck = 100

        loadContext()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d("action=${action}")
        when (action) {
            ACTION_A2DP_DISCONNECTED -> player.stop()
            ACTION_HEADSET_UNPLUGGED -> player.stop()
            ACTION_TOGGLE -> player.toggle()
            ACTION_UPDATE_APPWIDGET -> updateAppWidget()
        }
        return Service.START_NOT_STICKY
    }

    inner class PlayerServiceBinder : Binder() {
        fun setOnStatusChangedListener(listener: OnStatusChangedListener) {
            this@PlayerService.setOnStatusChangedListener(listener)
        }

        val currentStatus: CurrentStatus
            get() = this@PlayerService.currentStatus

        fun seek(pos: Int) {
            player.seek(pos)
        }

        fun play(path: String?) {
            player.play(path)
        }

        fun pause() {
            player.stop()
        }

        fun prevTrack() {
            player.prev()
        }

        fun nextTrack() {
            player.next()
        }
	
        fun switchContext() {
            this@PlayerService.switchContext()
        }
	
        fun setTopDir(topDir: String) {
            this@PlayerService.setTopDir(topDir)
        }
	
        fun setVolume(volume: Int) {
            this@PlayerService.setVolume(volume)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return PlayerServiceBinder()
    }

    private fun setMediaPlayerVolume() {
        val vol = volume * volumeDuck * volumeOnOff / 100 / 100
	player.setVolume(vol)
    }

    private fun handleAudioFocusChangeEvent(focusChange: Int) {
        Log.d("focusChange=${focusChange}")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                volumeDuck = 100
                setMediaPlayerVolume()
            }
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> player.stop()
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
        Log.d("curPlayer=${curPlayer}")
        stopPlay()    // saveContext() を含む。

        Log.d("load context.")
        loadContext()

        Log.d("curPlayer=${curPlayer}")
        if (curPlayer != null) {
            Log.d("starting")
            startPlay()

            Log.d("set to foreground")
            setForeground(true)
            Log.d("enqueue next player.")
            enqueueNext()
        }
    }

    private fun setForeground(on: Boolean) {
        Log.d("on=${on}")
        if (on) {
            val ctxt = db.playContextDao().find(contextId)
            var contextName = "noname"
            if (ctxt != null)
                contextName = ctxt.name
            val builder = Notification.Builder(this, "notify_channel_1")
            builder.setStyle(Notification.BigTextStyle()
		    .bigText(resources.getString(R.string.playing, contextName)))
            builder.setSmallIcon(R.drawable.notification)
            val intent = Intent(this, MainActivity::class.java)
            val tsBuilder = TaskStackBuilder.create(this)
            tsBuilder.addParentStack(MainActivity::class.java)
            tsBuilder.addNextIntent(intent)
            val pendingIntent = tsBuilder.getPendingIntent(
                    0, PendingIntent.FLAG_UPDATE_CURRENT)
            builder.setContentIntent(pendingIntent)
            startForeground(1, builder.build())
        } else {
            stopForeground(true)
        }
    }

    private fun saveContext() {
        Log.d("contextId=${contextId}")
        val ctxt = db.playContextDao().find(contextId)
        if (ctxt != null && curPlayer != null) {
            Log.d("Id=${ctxt.id}")
            ctxt.path = playingPath
            Log.d("path=${ctxt.path}")
            val stamp = curPlayer!!.timestamp
            ctxt.pos = stamp.anchorMediaTimeUs / 1000    // us -> ms
            Log.d("pos=${ctxt.pos}")
	    ctxt.volume = volume
	    Log.d("volume=${volume}")
            Log.d("ctxt saving...")
            db.playContextDao().update(ctxt)
        }
    }

    private fun loadContext() {
        Log.d("release nextPlayer.")
        releaseNextPlayer()

        stopPlay()

        Log.d("release curPlayer.")
        releaseCurPlayer()
        Log.d("set to non-foreground.")
        setForeground(false)

        Log.d("getting context_id")
        contextId = db.configDao().getContextId()
        Log.d("contextId=${contextId}")
        val ctxt = db.playContextDao().find(contextId)
        if (ctxt != null) {
            playingPath = ctxt.path
            topDir = ctxt.topDir
	    volume = ctxt.volume
            Log.d("playingPath=${playingPath}")
            Log.d("topDir=${topDir}")

            if (playingPath != null) {
                Log.d("creating mediaplayer.")
                val ret = createMediaPlayer(playingPath, ctxt.pos.toInt(), false)
                if (ret == null) {
                    Log.w("No audio file found.")
                    return
                }
                Log.d("creating mediaplayer. ok.")
                curPlayer = ret.mediaPlayer
                playingPath = ret.path
                Log.d("curPlayer=${curPlayer}")
                Log.d("playingPath=${playingPath}")
                setMediaPlayerVolume()
            } else {
                // 作られたばかりの context の場合。
                Log.d("creating mediaplayer.")
                val ret = createMediaPlayer("", 0, false)
                if (ret == null) {
                    Log.w("No audio file found.")
                    return
                }
                Log.d("creating mediaplayer. ok.")
                curPlayer = ret.mediaPlayer
                playingPath = ret.path
                Log.d("curPlayer=${curPlayer}")
                Log.d("playingPath=${playingPath}")
                setMediaPlayerVolume()
            }
        }
    }

    private fun startBroadcast() {
        val code = Runnable {
            try {
                while (true) {
                    handler.post { broadcastStatus() }
                    Thread.sleep(500)
                }
            } catch (e: InterruptedException) {
                Log.d("interrupted.", e)
            }
        }

        stopBroadcast()    // 念の為
	val thr = Thread(code)
        broadcaster = thr
        thr.start()
    }

    private fun stopBroadcast() {
	val thr = broadcaster
        if (thr != null) {
            thr.interrupt()
            try {
                thr.join()
            } catch (e: InterruptedException) {
                Log.e("interrupted.", e)
            }

            broadcaster = null
        }
    }

    private fun broadcastStatus() {
        val status = buildCurrentStatus()
        for (listener in statusChangedListeners) {
            // Log.d("listener=${listener}")
            listener.onStatusChanged(status)
        }
    }

    private val currentStatus: CurrentStatus
        get() = buildCurrentStatus()

    override fun onDestroy() {
        Log.d("save context")
        saveContext()

        Log.d("release nextPlayer.")
        releaseNextPlayer()
        stopPlay()
        Log.d("release curPlayer.")
        releaseCurPlayer()

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
        val ctxt = db.playContextDao().find(contextId)
        var contextName: String? = null
        if (ctxt != null)
            contextName = ctxt.name

        var icon = android.R.drawable.ic_media_play
        if (curPlayer != null && curPlayer!!.isPlaying)
            icon = android.R.drawable.ic_media_pause

        WidgetProvider.updateAppWidget(this, null, icon, contextName)
    }

    private fun setVolume(volume: Int) {
        this.volume = volume
        setMediaPlayerVolume()
    }

    companion object {
        val ACTION_A2DP_DISCONNECTED = "jp.ddo.masm11.contextplayer.A2DP_DISCONNECTED"
        val ACTION_HEADSET_UNPLUGGED = "jp.ddo.masm11.contextplayer.HEADSET_UNPLUGGED"
        val ACTION_TOGGLE = "jp.ddo.masm11.contextplayer.TOGGLE"
        val ACTION_UPDATE_APPWIDGET = "jp.ddo.masm11.contextplayer.UPDATE_APP_WIDGET"
    }
}
