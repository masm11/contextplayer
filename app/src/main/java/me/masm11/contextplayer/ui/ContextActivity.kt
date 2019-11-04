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
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import android.widget.LinearLayout
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.TextView
import android.widget.EditText
import android.widget.Button
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.app.AlertDialog
import android.app.Service
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentActivity
import android.text.InputType
import android.graphics.Color

import me.masm11.contextplayer.R
import me.masm11.contextplayer.service.PlayerService
import me.masm11.contextplayer.db.AppDatabase
import me.masm11.contextplayer.db.PlayContext
import me.masm11.contextplayer.db.PlayContextList
import me.masm11.contextplayer.util.emptyMutableListOf
import me.masm11.contextplayer.fs.MFile
import me.masm11.contextplayer.Application

import me.masm11.logger.Log

class ContextActivity : FragmentActivity() {

    private lateinit var db: AppDatabase
    private lateinit var playContexts: PlayContextList
    private val rootDir = MFile("//")
    private lateinit var items: MutableList<Item>
    private lateinit var itemAdapter: ItemAdapter
    
    private val bg_current = Color.parseColor("#002244")

    private inner class Item(val ctxt: PlayContext) {
	val uuid = ctxt.uuid
	var name = ctxt.name
	var topDir = ctxt.topDir
	var path = ctxt.path
	var current = ctxt.current != null
	private var onChangedListener: (PlayContext) -> Unit
	init {
	    onChangedListener = { c ->
		Log.d("uuid=${c.uuid}")
		Log.d("name=${c.name}")
		Log.d("topdir=${c.topDir}")
		Log.d("path=${c.path}")
		name = c.name
		topDir = c.topDir
		path = c.path
		current = c.current != null
		itemAdapter.notifyDataSetChanged()
	    }
	    ctxt.addOnChangedListener(onChangedListener)
	}
	fun stopListen() {
	    ctxt.removeOnChangedListener(onChangedListener)
	}
    }
    
    private inner class ItemAdapter(val items: List<Item>) : RecyclerView.Adapter<ItemViewHolder>() {
	private var onClickListener = { _: Item -> }

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val inflate = LayoutInflater.from(parent.context).inflate(R.layout.list_context, parent, false);
            return ItemViewHolder(inflate);
	}

	override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
	    Log.d("position=${position}")
	    val item = items.get(position)
	    Log.d("name=${item.name}")
	    holder.textView.text = item.name
	    holder.pathView.rootDir = rootDir.absolutePath
	    holder.pathView.topDir = item.topDir
	    holder.pathView.path = item.path
	    if (item.current)
		holder.layoutView.setBackgroundColor(bg_current)
	    else
		holder.layoutView.setBackground(null)
	    holder.layoutView.setOnClickListener {
		onClickListener(items.get(position))
	    }
	}

	override fun getItemCount(): Int {
	    Log.d("count=${items.size}")
	    return items.size
	}

	fun setOnClickListener(listener: (Item) -> Unit) {
	    onClickListener = listener
	}
    }
    
    private inner class ItemViewHolder(view: View): RecyclerView.ViewHolder(view) {
	public val layoutView: LinearLayout = view.findViewById<LinearLayout>(R.id.context_item)
	public val textView: TextView = view.findViewById<TextView>(R.id.context_name)
	public val pathView: PathView = view.findViewById<PathView>(R.id.context_topdir)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_context)
	
	db = AppDatabase.getDB()
	playContexts = (getApplication() as Application).getPlayContextList()

        items = emptyMutableListOf<Item>()
        for (uuid in playContexts.uuids()) {
	    val ctxt = playContexts.get(uuid)
	    if (ctxt != null) {
		val item = Item(ctxt)
		items.add(item)
	    }
        }
	items.sortWith(object: Comparator<Item> {		// lambda だけで書けない…?
	    override fun compare(p0: Item, p1: Item): Int {
		return p0.ctxt.displayOrder - p1.ctxt.displayOrder
	    }
	})

        itemAdapter = ItemAdapter(items)

        val llm = LinearLayoutManager(this)
	
        findViewById<RecyclerView>(R.id.context_list).apply {
	    setHasFixedSize(false)
            setLayoutManager(llm)
	    setAdapter(itemAdapter)
	}

	val itemDecor = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.RIGHT) {
		override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
		    val fromPos = viewHolder.getAdapterPosition()
		    val toPos = target.getAdapterPosition()
		    Log.d("fromPos=${fromPos}")
		    Log.d("toPos=${toPos}")
		    val item = items.removeAt(fromPos)
		    items.add(toPos, item)
		    for (i in 0 .. items.size-1)
			items.get(i).ctxt.displayOrder = i
		    for (i in 0 .. items.size-1)
			playContexts.put(items.get(i).ctxt.uuid)
		    itemAdapter.notifyItemMoved(fromPos, toPos)
		    return true
		}
		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
		    val fromPos = viewHolder.getAdapterPosition()
		    Log.d("fromPos=${fromPos}")
		    val item = items.removeAt(fromPos)
		    item.stopListen()
		    playContexts.delete(item.ctxt.uuid)
		    itemAdapter.notifyItemRemoved(fromPos)
		}
            }
	)
	itemDecor.attachToRecyclerView(findViewById<RecyclerView>(R.id.context_list))

	itemAdapter.setOnClickListener { item ->
	    playContexts.setCurrent(item.ctxt)
            PlayerService.switchContext(this)
	}

/*
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
                    val ctxt = playContexts.get(item.uuid)
		    if (ctxt != null) {
			ctxt.name = newName
			playContexts.put(item.uuid)
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
                        val ctxt = playContexts.get(item.uuid)
			if (ctxt != null)
                            playContexts.delete(item.uuid)
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
*/

        findViewById<Button>(R.id.context_add).setOnClickListener {
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
                val ctxt = playContexts.new()
                ctxt.name = newName
                ctxt.topDir = rootDir.absolutePath
		playContexts.put(ctxt.uuid)

                val item = Item(ctxt)
		items.add(item)
		itemAdapter.notifyDataSetChanged()
            }
            builder.show()
        }
    }
}
