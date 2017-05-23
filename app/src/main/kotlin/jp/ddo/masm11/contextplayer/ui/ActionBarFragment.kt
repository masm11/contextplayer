/* Context Player - Audio Player with Contexts
    Copyright (C) 2016 Yuuki Harano

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

import android.support.v7.widget.Toolbar
import android.app.Fragment
import android.app.Service
import android.app.AlertDialog
import android.webkit.WebView
import android.webkit.WebSettings
import android.webkit.JavascriptInterface
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MenuInflater
import android.os.Bundle
import android.os.IBinder
import android.content.Context
import android.content.Intent
import android.content.DialogInterface
import android.content.ServiceConnection
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.pm.PackageInfo

import kotlinx.android.synthetic.main.actionbar_fragment.view.*

import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.io.IOException

import jp.ddo.masm11.contextplayer.R

import jp.ddo.masm11.logger.Log

class ActionBarFragment : Fragment() {
    private inner class WebAppInterface(private val context: Context) {
        val appVersion: String
            @JavascriptInterface
            get() {
                try {
                    val pm = context.packageManager
                    val pi = pm.getPackageInfo("jp.ddo.masm11.contextplayer", 0)
                    return pi.versionName
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.e("namenotfoundexception", e)
                }

                return "???"
            }
    }

    var toolbar: Toolbar? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("")
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.actionbar_fragment, container, false)

        toolbar = view.toolbar

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.actionbar, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_about -> {
                val context = context
                val builder = AlertDialog.Builder(context)
                val inflater = activity.layoutInflater

                val webView = inflater.inflate(R.layout.about_dialog, null) as WebView
                val settings = webView.settings
                settings.javaScriptEnabled = true
                webView.addJavascriptInterface(WebAppInterface(context), "android")
                webView.loadUrl(resources.getString(R.string.about_url))

                builder.setView(webView)

                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    // NOP
                }
                builder.show()
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }
}
