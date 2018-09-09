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
package jp.ddo.masm11.contextplayer.ui

import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
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
import android.webkit.WebView
import android.webkit.WebSettings
import android.webkit.JavascriptInterface
import android.content.Intent
import android.content.Context
import android.content.ServiceConnection
import android.content.ComponentName
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.content.pm.PackageInfo
import android.os.Bundle
import android.Manifest

import kotlinx.android.synthetic.main.activity_about.*

import java.io.IOException
import java.util.Locale

import jp.ddo.masm11.contextplayer.R
import jp.ddo.masm11.contextplayer.service.PlayerService
import jp.ddo.masm11.contextplayer.util.Metadata
import jp.ddo.masm11.contextplayer.fs.MFile
import jp.ddo.masm11.contextplayer.db.AppDatabase
import jp.ddo.masm11.contextplayer.db.PlayContext
import jp.ddo.masm11.contextplayer.db.Config

import jp.ddo.masm11.logger.Log

class AboutActivity : AppCompatActivity() {
    private inner class WebAppInterface(private val context: Context) {
        val appVersion: String
            @JavascriptInterface
            get() {
                val pm = context.packageManager
                val pi = pm.getPackageInfo("jp.ddo.masm11.contextplayer", 0)
                return pi.versionName
            }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
	
        val fragMan = getFragmentManager()
	val frag = fragMan.findFragmentById(R.id.actionbar_frag) as ActionBarFragment
        setSupportActionBar(frag.toolbar)
	
        val settings = web_view.settings
        settings.javaScriptEnabled = true
	settings.textZoom = 50
        web_view.addJavascriptInterface(WebAppInterface(this), "android")
        web_view.loadUrl(resources.getString(R.string.about_url))
    }
}
