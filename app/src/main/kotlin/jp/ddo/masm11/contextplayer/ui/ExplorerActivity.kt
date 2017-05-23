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

import android.support.v7.app.AppCompatActivity
import android.app.Service
import android.app.FragmentManager
import android.os.Bundle
import android.os.Handler
import android.os.Environment
import android.os.IBinder
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.AdapterView
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.KeyEvent
import android.webkit.MimeTypeMap
import android.content.Intent
import android.content.ServiceConnection
import android.content.ComponentName

import kotlinx.android.synthetic.main.activity_explorer.*
import kotlinx.android.synthetic.main.list_explorer.view.*

import java.io.File
import java.io.FileFilter
import java.util.ArrayList
import java.util.LinkedList
import java.util.Arrays
import java.util.Collections
import java.util.Comparator
import java.util.Locale
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

import jp.ddo.masm11.contextplayer.R
import jp.ddo.masm11.contextplayer.db.PlayContext
import jp.ddo.masm11.contextplayer.db.Config
import jp.ddo.masm11.contextplayer.util.Metadata
import jp.ddo.masm11.contextplayer.service.PlayerService

import jp.ddo.masm11.logger.Log

class ExplorerActivity : AppCompatActivity() {
    private var conn: PlayerServiceConnection? = null
    private var svc: PlayerService.PlayerServiceBinder? = null
    private var backKeyShortPress: Boolean = false

    private inner class PlayerServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            svc = service as PlayerService.PlayerServiceBinder
        }

        override fun onServiceDisconnected(name: ComponentName) {
            svc = null
        }
    }

    private class FileItem(val file: File) {
        var title: String? = null
            private set
        var artist: String? = null
            private set
        var mimeType: String? = null
            private set

        init {
            if (!file.isDirectory) {
                Log.d("uri=%s", file.toURI().toASCIIString())

                val absPath = file.absolutePath
                val extPos = absPath.lastIndexOf('.')
                var ext = ""
                if (extPos >= 0) {
                    ext = absPath.substring(extPos + 1)
                    if (ext.contains("/"))
                        ext = ""
                }
                Log.d("ext=%s", ext)
                mimeType = mimeTypeMap.getMimeTypeFromExtension(ext)
                Log.d("mimeType=%s", mimeType)
                if (mimeType == null) {
                    ext = ext.toLowerCase()
                    mimeType = mimeTypeMap.getMimeTypeFromExtension(ext)
                    if (mimeType == null) {
                        ext = ext.toUpperCase()
                        mimeType = mimeTypeMap.getMimeTypeFromExtension(ext)
                    }
                }
            }
        }

        fun retrieveMetadata() {
            if (isAudioType(mimeType)) {
                val meta = Metadata(file.absolutePath)
                if (meta.extract()) {
                    title = meta.title
                    artist = meta.artist
                }
            }
        }

        val isDir: Boolean
            get() = file.isDirectory
        val filename: String
            get() = file.name

        override fun hashCode(): Int {
            return StringBuilder()
                    .append(file.toString())
                    .append("\t")
                    .append(if (title != null) title else "null")
                    .append("\t")
                    .append(if (artist != null) artist else "null")
                    .append("\t")
                    .append(if (mimeType != null) mimeType else "null")
                    .hashCode()
        }
    }

    private class FileAdapter(context: Context, items: ArrayList<FileItem>) : ArrayAdapter<FileItem>(context, R.layout.list_explorer, items) {
        private val inflater: LayoutInflater

        init {
            inflater = LayoutInflater.from(context)
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        override fun getItemId(position: Int): Long {
            val item = getItem(position)!!
            return item.hashCode().toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = convertView
            if (view == null)
                view = inflater.inflate(R.layout.list_explorer, parent, false)!!

            val item = getItem(position)!!

            if (!item.isDir) {
                view.filename.text = item.filename
                view.mime_type.text = item.mimeType
                view.title.text = item.title ?: view.context.resources.getString(R.string.unknown_title)
                view.artist.text = item.artist ?: view.context.resources.getString(R.string.unknown_artist)

                view.for_file.visibility = View.VISIBLE
                view.for_dir.visibility = View.GONE
            } else {
                view.dirname.text = item.filename + "/"

                view.for_file.visibility = View.GONE
                view.for_dir.visibility = View.VISIBLE
            }

            return view
        }
    }

    private inner class BackgroundRetriever(private val adapter: FileAdapter) : Runnable {
        private val list: LinkedList<FileItem> = LinkedList<FileItem>()
	private val mutex: Lock = ReentrantLock()
	private val cond: Condition = mutex.newCondition()

        override fun run() {
            try {
                while (true) {
                    var item: FileItem

		    mutex.lock()
		    try {
			while (list.isEmpty())
                            cond.await()
			item = list.removeFirst()
		    } finally {
			mutex.unlock()
		    }

                    item.retrieveMetadata()
                    handler!!.post { adapter.notifyDataSetChanged() }
                }
            } catch (e: InterruptedException) {
            }

        }

        fun setNewItems(newList: ArrayList<FileItem>) {
	    mutex.lock()
	    try {
		list.clear()
		list.addAll(newList)
		cond.signal()
	    } finally {
		mutex.unlock()
	    }
        }
    }

    private var rootDir: File = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC)    // これより上には戻れない
    private var topDir: File = File(".")
    private var curDir: File = File(".")
    private var adapter: FileAdapter? = null
    private var ctxt: PlayContext? = null
    private var bretr: BackgroundRetriever? = null
    private var thread: Thread? = null
    private var handler: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_explorer)

        val fragMan = getFragmentManager()
	val frag = fragMan.findFragmentById(R.id.actionbar_frag) as ActionBarFragment
        setSupportActionBar(frag.toolbar)

        handler = Handler()

        adapter = FileAdapter(this, ArrayList<FileItem>())

        val ctxtId = Config.loadContextId()
        var c = PlayContext.find(ctxtId)
        if (c == null)
            c = PlayContext()
	ctxt = c

        bretr = BackgroundRetriever(adapter!!)
	val t = Thread(bretr)
        thread = t
        t.priority = Thread.MIN_PRIORITY
        t.start()

        var dir = File(c.topDir)
        topDir = dir
	val path = c.path
        if (path != null && path.startsWith(c.topDir)) {
            val slash = path.lastIndexOf('/')
            if (slash != -1)
                dir = File(path.substring(0, slash))
        }
        if (savedInstanceState != null) {
            val str = savedInstanceState.getString(STATE_CUR_DIR)
            if (str != null)
                dir = File(str)
        }
        renewAdapter(dir)

        list.setOnItemClickListener(object : AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val listView = parent as ListView
                val item = listView.getItemAtPosition(position) as FileItem
                Log.d("clicked=%s", item.filename)

                if (item.isDir) {
                    if (item.filename != ".")
                        renewAdapter(item.file)
                } else {
                    play(item.file)
                }
            }
        })
        list.setOnItemLongClickListener(object : AdapterView.OnItemLongClickListener {
            override fun onItemLongClick(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
                val listView = parent as ListView
                val item = listView.getItemAtPosition(position) as FileItem
                Log.d("longclicked=%s", item.filename)

                if (item.isDir) {
		    setTopDir(if (item.filename == ".") curDir else item.file)
                    return true
                } else
                    return false
            }
        })
    }

    /* 参考:
     *   http://stackoverflow.com/questions/12950215/onkeydown-and-onkeylongpress
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
            event.startTracking()
            if (event.repeatCount == 0)
                backKeyShortPress = true
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            backKeyShortPress = false
            finish()
            return true
        }
        return super.onKeyLongPress(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (backKeyShortPress) {
                if (curDir == rootDir || curDir.absolutePath == "/")
                    finish()
                else
                    renewAdapter(curDir.parentFile)
            }
            backKeyShortPress = false
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun setTopDir(newDir: File) {
        topDir = newDir

        // topDir からの相対で curDir を表示
        val topPath = topDir.toString()
        val curPath = curDir.toString()
        path.rootDir = rootDir.toString()
        path.topDir = topPath
        path.path = curPath + "/"

        svc?.setTopDir(newDir.absolutePath)

	val c = ctxt
	if (c != null) {
	    c.topDir = newDir.absolutePath
	    c.path = null
	    c.pos = 0
	    c.save()
	}
    }

    private fun renewAdapter(newDir: File) {
        val files = listFiles(newDir, false)
        val items = ArrayList<FileItem>()
        for (i in files.indices) {
            Log.d("%s", files[i].toString())
            items.add(FileItem(files[i]))
        }

        Log.d("newDir=%s", newDir.toString())
        Log.d("rootDir=%s", rootDir.toString())
        items.add(0, FileItem(File(newDir, ".")))

        adapter!!.clear()
        adapter!!.addAll(items)

        bretr!!.setNewItems(items)

        list.adapter = adapter

        // topDir からの相対で newDir を表示
        val topPath = topDir.toString()
        val newPath = newDir.toString()
        path.rootDir = rootDir.toString()
        path.path = newPath + "/"
        path.topDir = topPath

        curDir = newDir
    }

    private fun play(file: File) {
        svc?.play(file.absolutePath)
    }

    public override fun onStart() {
        super.onStart()

        // started service にする。
        startService(Intent(this, PlayerService::class.java))

        val intent = Intent(this, PlayerService::class.java)
        conn = PlayerServiceConnection()
        bindService(intent, conn, Service.BIND_AUTO_CREATE)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(STATE_CUR_DIR, curDir.absolutePath)
    }

    public override fun onStop() {
        unbindService(conn)

        super.onStop()
    }

    public override fun onDestroy() {
	val t = thread
        if (t != null) {
            t.interrupt()
            try {
                t.join()
            } catch (e: InterruptedException) {
            }

            thread = null
        }

        super.onDestroy()
    }

    companion object {
        private val STATE_CUR_DIR = "jp.ddo.masm11.contextplayer.CUR_DIR"

        private val mimeTypeMap = MimeTypeMap.getSingleton()

        fun isAudioType(mimeType: String?): Boolean {
            if (mimeType == null)
                return false
            if (mimeType.startsWith("audio/"))
                return true
            if (mimeType == "application/ogg")
                return true
            return false
        }

        /* dir に含まれるファイル名をリストアップする。
	 * '.' で始まるものは含まない。
	 * ソートされている。
	 */
        fun listFiles(dir: File, reverse: Boolean): Array<File> {
            val files = dir.listFiles { pathname -> !pathname.name.startsWith(".") }
	    if (files == null)
		return emptyArray<File>()

            var comparator: Comparator<File> = Comparator { o1, o2 ->
                val name1 = o1.name.toLowerCase(Locale.getDefault())
                val name2 = o2.name.toLowerCase(Locale.getDefault())
                // まず、大文字小文字を無視して比較
                var r = name1.compareTo(name2)
                // もしそれで同じだったら、区別して比較
                if (r == 0)
                    r = o1.compareTo(o2)
                r
            }
            if (reverse)
                comparator = Collections.reverseOrder(comparator)
            Arrays.sort(files, comparator)

            return files
        }
    }
}