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
import android.support.v4.content.LocalBroadcastManager
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.content.Intent
import android.content.IntentFilter
import android.content.Context
import android.content.DialogInterface
import android.content.ServiceConnection
import android.content.ComponentName
import android.content.BroadcastReceiver
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView
import android.widget.EditText
import android.widget.Button
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.app.AlertDialog
import android.app.Service
import android.app.FragmentManager
import android.text.InputType

import kotlinx.android.synthetic.main.activity_context.*
import kotlinx.android.synthetic.main.list_context.view.*

import me.masm11.contextplayer.R
import me.masm11.contextplayer.service.PlayerService
import me.masm11.contextplayer.db.AppDatabase
import me.masm11.contextplayer.db.PlayContext
import me.masm11.contextplayer.db.Config
import me.masm11.contextplayer.util.emptyMutableListOf
import me.masm11.contextplayer.fs.MFile

import me.masm11.logger.Log

class ContextActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private val rootDir = MFile("//")
    private lateinit var items: MutableList<Item>
    private lateinit var adapter: ItemAdapter
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var localBroadcastReceiver: BroadcastReceiver
    
    private inner class Item(val id: Long, var name: String?, val topDir: String, var path: String?)
    
    private inner class ItemAdapter(context: Context, items: List<Item>) : ArrayAdapter<Item>(context, R.layout.list_context, items) {
        private val inflater = LayoutInflater.from(context)

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = convertView
            if (view == null)
                view = inflater.inflate(R.layout.list_context, parent, false)!!

            val item = getItem(position)

	    view.context_name.text = item?.name ?: "(?)"

	    view.context_topdir.rootDir = rootDir.absolutePath
	    view.context_topdir.topDir = item.topDir
	    view.context_topdir.path = item.path

            return view
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_context)
	
	db = AppDatabase.getDB()
	
        val fragMan = getFragmentManager()
	val frag = fragMan.findFragmentById(R.id.actionbar_frag) as ActionBarFragment
        setSupportActionBar(frag.toolbar)

        items = emptyMutableListOf<Item>()
        for (ctxt in db.playContextDao().getAll()) {
            val item = Item(ctxt.id, ctxt.name, ctxt.topDir, ctxt.path)
            items.add(item)
        }

        adapter = ItemAdapter(this, items)

        context_list.adapter = adapter

        context_list.setOnItemClickListener { parent, _, position, _ ->
            val listView = parent as ListView
            val item = listView.getItemAtPosition(position) as Item
	    
            db.configDao().setContextId(item.id)
	    
            PlayerService.switchContext(this)
        }

        context_list.setOnItemLongClickListener(object : AdapterView.OnItemLongClickListener {
            override fun onItemLongClick(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
                val listView = parent as ListView
                val item = listView.getItemAtPosition(position) as Item

                val builder = AlertDialog.Builder(this@ContextActivity)
                builder.setItems(R.array.context_list_menu) { _, which ->
                    when (which) {
                        0    // edit
                        -> editContextName(item)

                        1    // delete
                        -> deleteContext(item)
                    }
                }
                builder.show()

                return true
            }

            private fun editContextName(item: Item) {
                val editText = EditText(this@ContextActivity)
                editText.inputType = InputType.TYPE_CLASS_TEXT
                editText.setText(item.name)
                val builder = AlertDialog.Builder(this@ContextActivity)
                builder.setTitle(R.string.edit_the_context_name)
                builder.setView(editText)
                builder.setNegativeButton(android.R.string.cancel) { _, _ ->
                    // NOP.
                }
                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    val newName = editText.text.toString()
                    val ctxt = db.playContextDao().find(item.id)
		    if (ctxt != null) {
			ctxt.name = newName
			db.playContextDao().update(ctxt)
		    }
		    
                    item.name = newName
                    adapter.notifyDataSetChanged()
                }
                builder.show()
            }

            private fun deleteContext(item: Item) {
                if (adapter.count >= 2) {
                    val builder = AlertDialog.Builder(this@ContextActivity)
                    builder.setMessage(R.string.are_you_sure_to_delete_it)
                    builder.setNegativeButton(android.R.string.cancel) { _, _ ->
                        // NOP.
                    }
                    builder.setPositiveButton(android.R.string.ok) { _, _ ->
                        val ctxt = db.playContextDao().find(item.id)
			if (ctxt != null)
                            db.playContextDao().delete(ctxt)
                        adapter.remove(item)
                    }
                    builder.show()
                } else {
                    val builder = AlertDialog.Builder(this@ContextActivity)
                    builder.setMessage(R.string.cant_delete_the_last_context)
                    builder.setPositiveButton(android.R.string.ok) { _, _ ->
                        // NOP.
                    }
                    builder.show()
                }
            }
        })

        context_add.setOnClickListener {
            val editText = EditText(this@ContextActivity)
            editText.inputType = InputType.TYPE_CLASS_TEXT
            val builder = AlertDialog.Builder(this@ContextActivity)
            builder.setTitle(R.string.name_the_new_context)
            builder.setView(editText)
            builder.setNegativeButton(android.R.string.cancel) { _, _ ->
                // NOP.
            }
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                val newName = editText.text.toString()
                val ctxt = PlayContext()
                ctxt.name = newName
                ctxt.topDir = rootDir.absolutePath
		db.playContextDao().insert(ctxt)

                val item = Item(ctxt.id, newName, ctxt.topDir, null)
                adapter.add(item)
            }
            builder.show()
        }
	
	localBroadcastManager = LocalBroadcastManager.getInstance(this)
	localBroadcastReceiver = object: BroadcastReceiver() {
	    override fun onReceive(context: Context, intent: Intent) {
		Log.d("received.")
		val contextId = intent.getLongExtra(PlayerService.EXTRA_CONTEXT_ID, 0)
		val path = intent.getStringExtra(PlayerService.EXTRA_PATH)
		
		var changed = false
		for (item in items) {
		    if (item.id == contextId) {
			if (item.path != path) {
			    item.path = path
			    changed = true
			}
		    }
		}
		if (changed)
		    adapter.notifyDataSetChanged()
	    }
	}
	val filter = IntentFilter(PlayerService.ACTION_CURRENT_STATUS)
	localBroadcastManager.registerReceiver(localBroadcastReceiver, filter)
    }
}