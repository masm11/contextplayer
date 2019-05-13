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

import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.app.Service
import android.app.AlertDialog
import android.app.FragmentManager
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
import me.masm11.contextplayer.db.Config

import me.masm11.logger.Log

class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {
/*
    private inner class PlayerServiceConnection : ServiceConnection {
	// 参照を保持しておかないと、GC に回収されてしまう。
        private val listener = { status: PlayerService.CurrentStatus -> updateTrackInfo(status) }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val s = service as PlayerService.PlayerServiceBinder
	    svc = s

	    s.setOnStatusChangedListener(listener)
	    
	    updateTrackInfo(s.currentStatus)
	    
            if (needSwitchContext) {
		s.switchContext()
                needSwitchContext = false
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            svc = null
        }
    }
*/

    private lateinit var db: AppDatabase
/*
    private var svc: PlayerService.PlayerServiceBinder? = null
    private var conn: ServiceConnection? = null
*/
    private val rootDir = MFile("//")
    private var curPath: String? = null
    private var curTopDir: String? = null
    private var curPos: Int = 0    // msec
    private var maxPos: Int = 0    // msec
    private var seeking: Boolean = false
    private var vol: Int = 100
    private var needSwitchContext: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

	db = AppDatabase.getDB()

        val fragMan = getFragmentManager()
	val frag = fragMan.findFragmentById(R.id.actionbar_frag) as ActionBarFragment
        setSupportActionBar(frag.toolbar)

        Log.d("rootDir=${rootDir.absolutePath}")
	
        if (db.playContextDao().getAll().size == 0) {
            val ctxt = PlayContext()
            ctxt.name = resources.getString(R.string.default_context)
            ctxt.topDir = rootDir.absolutePath
            db.playContextDao().insert(ctxt)
        }

        context_name.setOnClickListener {
            val i = Intent(this@MainActivity, ContextActivity::class.java)
            startActivity(i)
        }

        playing_info.setOnClickListener {
            val i = Intent(this@MainActivity, ExplorerActivity::class.java)
            startActivity(i)
        }

        playing_pos.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
/*
                    svc?.seek(progress)
*/
                }
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
/*
                svc?.setVolume(VOLUME_BASE + progress)
*/

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
                val id = intent.getLongExtra("me.masm11.contextplayer.CONTEXT_ID", -1)

                if (id != -1L) {
                    db.configDao().setContextId(id)

                    needSwitchContext = true
                }
            }
        }
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

    override fun onStart() {
        super.onStart()

        // started service にする。
        // startService(Intent(this, PlayerService::class.java))

        // val intent = Intent(this, PlayerService::class.java)
/*
        conn = PlayerServiceConnection()
        bindService(intent, conn, Service.BIND_AUTO_CREATE)
*/
    }

    override fun onResume() {
        val ctxt = db.playContextDao().find(db.configDao().getContextId())
        if (ctxt != null)
            context_name.text = ctxt.name

        super.onResume()
    }

    override fun onStop() {
/*
        unbindService(conn)
*/

        super.onStop()
    }

    private fun updateTrackInfo(status: PlayerService.CurrentStatus) {
	var p = status.path
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

        if (curTopDir != status.topDir) {
            curTopDir = status.topDir
	    val dir = curTopDir
            playing_filename.topDir = if (dir != null) dir else "//"
        }

        if (maxPos != status.duration) {
            maxPos = status.duration

            playing_pos.max = maxPos

            val sec = maxPos / 1000
            val maxTime = String.format(Locale.US, "%d:%02d", sec / 60, sec % 60)
            playing_maxtime.text = maxTime
        }

        if (curPos != status.position) {
            curPos = status.position

            playing_pos.progress = curPos

            val sec = curPos / 1000
            val curTime = String.format(Locale.US, "%d:%02d", sec / 60, sec % 60)
            playing_curtime.text = curTime
        }

	if (vol != status.volume) {
	    vol = status.volume
	    volume.progress = vol - VOLUME_BASE
	}
    }

    companion object {
        private val REQ_PERMISSION_ON_CREATE = 1
	private val VOLUME_BASE = 50
    }
}
