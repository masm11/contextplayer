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
import android.app.Service
import android.app.AlertDialog
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.Fragment
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

class MainActivity : FragmentActivity() {

    private lateinit var fragmentManager: FragmentManager
    private lateinit var curFragment: Fragment
    private val dummyOnBackPressedListener = { -> false }
    private var onBackPressedListener: () -> Boolean = dummyOnBackPressedListener

    private val moveToOtherFragmentListener: (Int) -> Unit = { type: Int ->
	var nextFragment: Fragment?
	when (type) {
	    1 -> nextFragment = ContextFragment()
	    2 -> nextFragment = ExplorerFragment()
	    else -> throw RuntimeException("Internal Error: Bad fragment type: ${type}")
	}
	
	fragmentManager.beginTransaction()
	    .replace(R.id.top_half, nextFragment)
	    .addToBackStack(null)
	    .commit()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
	
	val frag = MainFragment()
	frag.setMoveToOtherFragmentListener(moveToOtherFragmentListener)
	curFragment = frag
	
	fragmentManager = getSupportFragmentManager()
	fragmentManager.beginTransaction()
	    .add(R.id.top_half, curFragment)
	    .commit()
    }
    
    fun setOnBackPressedListener(listener: () -> Boolean) {
	onBackPressedListener = listener
    }
    
    override fun onBackPressed() {
	/* true: fragment 内で処理した。無視してくれ。→そのまま return
	*  false: fragment 終了した。戻してくれ。→ super へ移譲
	*         main の場合だったらアプリ終了。→ super へ移譲
	*/
	if (onBackPressedListener())
	    return
	setOnBackPressedListener(dummyOnBackPressedListener)
	super.onBackPressed()
    }
}
