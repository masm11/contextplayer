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

import androidx.appcompat.widget.Toolbar
import android.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MenuInflater
import android.os.Bundle
import android.content.Intent

import kotlinx.android.synthetic.main.actionbar_fragment.view.*

import me.masm11.contextplayer.R

import me.masm11.logger.Log

class ActionBarFragment : Fragment() {
    var toolbar: Toolbar? = null
        private set
    
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("")
        super.onCreate(savedInstanceState)

	// xml ファイルに attribute を書きたい…
	if (context::class != AboutActivity::class)
            setHasOptionsMenu(true)
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.actionbar_fragment, container, false)
	
        // toolbar = view.toolbar		// fixme: ??
	
        return view
    }
    
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.actionbar, menu)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_about -> {
                val context = context
		val i = Intent(context, AboutActivity::class.java)
		context.startActivity(i)
                return true
            }
	    
            else -> return super.onOptionsItemSelected(item)
        }
    }
}
