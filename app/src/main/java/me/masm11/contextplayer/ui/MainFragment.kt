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
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
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
import android.view.ViewGroup
import android.view.LayoutInflater
import android.net.Uri
import android.widget.Button
import android.widget.TextView
import android.widget.SeekBar
import android.widget.LinearLayout
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

class MainFragment : Fragment() {
    private lateinit var playContexts: PlayContextList
    private lateinit var curContext: PlayContext
    private lateinit var onContextChangedListener: (PlayContext) -> Unit
    private lateinit var onContextSwitchListener: (PlayContext) -> Unit
    private val rootDir = MFile("//")
    private var curPath: String? = null
    private var curTopDir: String? = null
    private var needSwitchContext: Boolean = false
    
    private lateinit var playingFilenameView: PathView
    private lateinit var playingTitleView: TextView
    private lateinit var playingArtistView: TextView
    private lateinit var contextNameView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.main_fragment, container, false)

	playContexts = (activity!!.getApplication() as Application).getPlayContextList()

        Log.d("rootDir=${rootDir.absolutePath}")
	
	playingFilenameView = view.findViewById<PathView>(R.id.playing_filename)
	playingTitleView = view.findViewById<TextView>(R.id.playing_title)
	playingArtistView = view.findViewById<TextView>(R.id.playing_artist)
	contextNameView = view.findViewById<TextView>(R.id.context_name)
	
        view.findViewById<TextView>(R.id.context_name).setOnClickListener {
            val i = Intent(activity, ContextActivity::class.java)
            startActivity(i)
        }

        view.findViewById<LinearLayout>(R.id.playing_info).setOnClickListener {
            val i = Intent(activity, ExplorerActivity::class.java)
            startActivity(i)
        }

        if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity as FragmentActivity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // permission がない && 説明必要 => 説明
                val dialog = AlertDialog.Builder(context)
                        .setMessage(R.string.please_grant_permission)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                            ActivityCompat.requestPermissions(activity as FragmentActivity, permissions, REQ_PERMISSION_ON_CREATE)
                        }
                        .create()
                dialog.show()
            } else {
                // permission がない && 説明不要 => request。
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                ActivityCompat.requestPermissions(activity as FragmentActivity, permissions, REQ_PERMISSION_ON_CREATE)
            }
        } else {
            // permission がある
            // rootDir.mkdirs()
        }
	
	return view;
    }
    
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == REQ_PERMISSION_ON_CREATE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED)
                (activity as FragmentActivity).finish()		// fixme: 終わらない?
            else {
                // rootDir.mkdirs()
	    }
        }
    }
    
    fun setContextListener() {
	onContextSwitchListener = { ctxt ->
	    curContext.removeOnChangedListener(onContextChangedListener)
	    curContext = ctxt
	    curContext.addOnChangedListener(onContextChangedListener)

	    updateTrackInfo(ctxt)
	    updateContextName(ctxt)
	}
	onContextChangedListener = { ctxt ->
	    updateTrackInfo(ctxt)
	}
	
	curContext = playContexts.getCurrent()
	
	curContext.addOnChangedListener(onContextChangedListener)
	playContexts.addOnContextSwitchListener(onContextSwitchListener)
	
	updateContextName(curContext)
	updateTrackInfo(curContext)
    }
    
    fun unsetContextListener() {
	playContexts.removeOnContextSwitchListener(onContextSwitchListener)
	curContext.removeOnChangedListener(onContextChangedListener)
    }
    
    override fun onResume() {
	setContextListener()
	
        super.onResume()
    }
    
    override fun onPause() {
	unsetContextListener()
	
	super.onPause()
    }
    
    override fun onDestroy() {
	super.onDestroy()
    }
    
    private fun updateContextName(ctxt: PlayContext) {
	contextNameView.text = ctxt.name
	Log.d("Id=${ctxt.uuid}")
	Log.d("name=${ctxt.name}")
    }

    private fun updateTrackInfo(ctxt: PlayContext) {
	val path = ctxt.path
	val topDir = ctxt.topDir
	Log.d("path=${path}")

	var p = path
	if (p == null)
	    p = "//"
        if (curPath != p) {
            curPath = p
	    
            playingFilenameView.rootDir = rootDir.absolutePath
            playingFilenameView.path = p

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

            playingTitleView.text = title
            playingArtistView.text = artist
        }

        if (curTopDir != topDir) {
            curTopDir = topDir
	    val dir = curTopDir
            playingFilenameView.topDir = if (dir != null) dir else "//"
        }
    }

    companion object {
        private val REQ_PERMISSION_ON_CREATE = 1
    }
}
