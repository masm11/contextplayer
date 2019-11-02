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

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.app.Service
import android.app.AlertDialog
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentActivity
import android.os.IBinder
import android.os.Bundle
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.view.MenuInflater
import android.net.Uri
import android.widget.Button
import android.widget.TextView
import android.widget.SeekBar
import android.content.Intent
import android.content.Context
import android.content.ServiceConnection
import android.content.ComponentName
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.content.pm.PackageInfo
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.Manifest

import kotlinx.android.synthetic.main.activity_main.*

import java.io.IOException
import java.util.Locale

import me.masm11.contextplayer.R
import me.masm11.contextplayer.service.PlayerService
import me.masm11.contextplayer.util.Metadata
import me.masm11.contextplayer.fs.MFile
import me.masm11.contextplayer.db.AppDatabase
import me.masm11.contextplayer.db.PlayContext
import me.masm11.contextplayer.db.PlayContextList
import me.masm11.contextplayer.Application

import me.masm11.logger.Log

class MainActivity : FragmentActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private lateinit var playContexts: PlayContextList
    private val rootDir = MFile("//")
    private var curPath: String? = null
    private var curTopDir: String? = null
    private var curPos: Int = 0    // msec
    private var maxPos: Int = 0    // msec
    private var seeking: Boolean = false
    private var vol: Int = 100
    private var needSwitchContext: Boolean = false
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var localBroadcastReceiver: BroadcastReceiver
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

	playContexts = (getApplication() as Application).getPlayContextList()

        Log.d("rootDir=${rootDir.absolutePath}")
	
        context_name.setOnClickListener {
            val i = Intent(this@MainActivity, ContextActivity::class.java)
	    val pair_1 = Pair<View, String>(context_name, "transit_cat")
	    val pair_2 = Pair<View, String>(findViewById(R.id.op_frag), "transit_op")
	    val opt = ActivityOptionsCompat.makeSceneTransitionAnimation(
		this@MainActivity, pair_1, pair_2
	    )
            startActivity(i, opt.toBundle())
        }

        playing_info.setOnClickListener {
            val i = Intent(this@MainActivity, ExplorerActivity::class.java)
	    val pair_1 = Pair<View, String>(playing_info, "transit_title")
	    val pair_2 = Pair<View, String>(findViewById(R.id.op_frag), "transit_op")
	    val opt = ActivityOptionsCompat.makeSceneTransitionAnimation(
		this@MainActivity, pair_1, pair_2
	    )
            startActivity(i, opt.toBundle())
        }

        playing_pos.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser)
		    PlayerService.seek(this@MainActivity, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                seeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                seeking = false
            }
        })

        volume.max = 100 - VOLUME_BASE
        volume.progress = vol - VOLUME_BASE
        volume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(volume: SeekBar, progress: Int, fromUser: Boolean) {
                PlayerService.setVolume(this@MainActivity, VOLUME_BASE + progress)

                vol = VOLUME_BASE + progress
            }

            override fun onStartTrackingTouch(volume: SeekBar) {
                /*NOP*/
            }

            override fun onStopTrackingTouch(volume: SeekBar) {
                /*NOP*/
            }
        })

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // permission がない && 説明必要 => 説明
                val dialog = AlertDialog.Builder(this)
                        .setMessage(R.string.please_grant_permission)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                            ActivityCompat.requestPermissions(this@MainActivity, permissions, REQ_PERMISSION_ON_CREATE)
                        }
                        .create()
                dialog.show()
            } else {
                // permission がない && 説明不要 => request。
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                ActivityCompat.requestPermissions(this, permissions, REQ_PERMISSION_ON_CREATE)
            }
        } else {
            // permission がある
            // rootDir.mkdirs()
        }

        val intent = getIntent()
        if (intent != null) {
            val action = intent.getAction()
            if (action != null && action == Intent.ACTION_MAIN) {
                val uuid = intent.getStringExtra("me.masm11.contextplayer.CONTEXT_ID")

                if (uuid != null) {
		    val ctxt = playContexts.get(uuid)
		    if (ctxt != null)
			playContexts.setCurrent(ctxt)
		    
                    needSwitchContext = true
                }
            }
        }
	
	localBroadcastManager = LocalBroadcastManager.getInstance(this)
	localBroadcastReceiver = object: BroadcastReceiver() {
	    override fun onReceive(context: Context, intent: Intent) {
		val path = intent.getStringExtra(PlayerService.EXTRA_PATH)
		val topDir = intent.getStringExtra(PlayerService.EXTRA_TOPDIR)
		val volume = intent.getIntExtra(PlayerService.EXTRA_VOLUME, 0)
		val pos = intent.getIntExtra(PlayerService.EXTRA_POS, 0)
		val duration = intent.getIntExtra(PlayerService.EXTRA_DURATION, 0)
		updateTrackInfo(path, topDir, pos, duration, volume)
	    }
	}
	val filter = IntentFilter(PlayerService.ACTION_CURRENT_STATUS)
	localBroadcastManager.registerReceiver(localBroadcastReceiver, filter)
	
	PlayerService.requestCurrentStatus(this)
    }
    
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == REQ_PERMISSION_ON_CREATE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED)
                finish()
            else {
                // rootDir.mkdirs()
	    }
        }
    }
    
    override fun onResume() {
        val ctxt = playContexts.getCurrent()
        context_name.text = ctxt.name
	
        super.onResume()
    }
    
    override fun onDestroy() {
	localBroadcastManager.unregisterReceiver(localBroadcastReceiver)
	
	super.onDestroy()
    }
    
    private fun updateTrackInfo(path: String?, topDir: String, pos: Int, duration: Int, vol1: Int) {
	var p = path
	if (p == null)
	    p = "//"
        if (curPath != p) {
            curPath = p
	    
            playing_filename.rootDir = rootDir.absolutePath
            playing_filename.path = p

            val meta = Metadata(MFile(p).file.absolutePath)
            var title: String? = null
            var artist: String? = null
            if (meta.extract()) {
                title = meta.title
                artist = meta.artist
            }

            if (title == null)
                title = resources.getString(R.string.unknown_title)
            if (artist == null)
                artist = resources.getString(R.string.unknown_artist)

            playing_title.text = title
            playing_artist.text = artist
        }

        if (curTopDir != topDir) {
            curTopDir = topDir
	    val dir = curTopDir
            playing_filename.topDir = if (dir != null) dir else "//"
        }

        if (maxPos != duration) {
            maxPos = duration

            playing_pos.max = maxPos

            val sec = maxPos / 1000
            val maxTime = String.format(Locale.US, "%d:%02d", sec / 60, sec % 60)
            playing_maxtime.text = maxTime
        }

        if (curPos != pos) {
            curPos = pos

            playing_pos.progress = curPos

            val sec = curPos / 1000
            val curTime = String.format(Locale.US, "%d:%02d", sec / 60, sec % 60)
            playing_curtime.text = curTime
        }

	if (vol != vol1) {
	    vol = vol1
	    volume.progress = vol - VOLUME_BASE
	}
    }

    companion object {
        private val REQ_PERMISSION_ON_CREATE = 1
	private val VOLUME_BASE = 50
    }
}
