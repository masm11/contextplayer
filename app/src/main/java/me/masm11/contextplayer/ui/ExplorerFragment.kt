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
import android.app.Service
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.KeyEvent
import android.view.ViewConfiguration
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.AccelerateInterpolator
import android.view.animation.TranslateAnimation
import android.webkit.MimeTypeMap
import android.content.Intent
import android.content.ServiceConnection
import android.content.ComponentName

import kotlinx.coroutines.*

import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Comparator
import java.util.Locale
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

import me.masm11.contextplayer.R
import me.masm11.contextplayer.db.AppDatabase
import me.masm11.contextplayer.db.PlayContext
import me.masm11.contextplayer.db.PlayContextList
import me.masm11.contextplayer.util.Metadata
import me.masm11.contextplayer.fs.MFile
import me.masm11.contextplayer.service.PlayerService
import me.masm11.contextplayer.Application

import me.masm11.logger.Log

class ExplorerFragment: Fragment() {
    private var backKeyShortPress: Boolean = false
    
    private var supervisorJob = SupervisorJob()
    private var supervisorScope = CoroutineScope(supervisorJob)
    
    private lateinit var pathView: PathView
    private lateinit var listViewportView: RelativeLayout
    
    private class FileItem(val file: MFile) {
        var title: String? = null
            private set
        var artist: String? = null
            private set
        var mimeType: String? = null
            private set

        init {
            if (!file.isDirectory) {
                // Log.d("uri=${file.toURI().toASCIIString()}")
                Log.d("uri=${file}")

                val absPath = file.absolutePath
                val extPos = absPath.lastIndexOf('.')
                var ext = ""
                if (extPos >= 0) {
                    ext = absPath.substring(extPos + 1)
                    if (ext.contains("/"))
                        ext = ""
                }
                Log.d("ext=${ext}")
                mimeType = mimeTypeMap.getMimeTypeFromExtension(ext)
                Log.d("mimeType=${mimeType}")
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

        suspend fun retrieveMetadata() {
            if (isAudioType(mimeType)) {
                val meta = Metadata(file.file.absolutePath)
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
        private val inflater: LayoutInflater = LayoutInflater.from(context)

        override fun hasStableIds(): Boolean {
            return true
        }

        override fun getItemId(position: Int): Long {
            val item = getItem(position)
	    if (item != null)
		return item.hashCode().toLong()
	    return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = convertView
            if (view == null)
                view = inflater.inflate(R.layout.list_explorer, parent, false)!!

            val item = getItem(position)

	    if (item != null) {
		if (!item.isDir) {
		    view.findViewById<TextView>(R.id.filename).text = item.filename
		    view.findViewById<TextView>(R.id.mime_type).text = item.mimeType
		    view.findViewById<TextView>(R.id.title).text = item.title ?: view.context.resources.getString(R.string.unknown_title)
		    view.findViewById<TextView>(R.id.artist).text = item.artist ?: view.context.resources.getString(R.string.unknown_artist)

		    view.findViewById<LinearLayout>(R.id.for_file).visibility = View.VISIBLE
		    view.findViewById<LinearLayout>(R.id.for_dir).visibility = View.GONE
		} else {
		    view.findViewById<TextView>(R.id.dirname).text = "${item.filename}/"

		    view.findViewById<LinearLayout>(R.id.for_file).visibility = View.GONE
		    view.findViewById<LinearLayout>(R.id.for_dir).visibility = View.VISIBLE
		}
	    }

            return view
        }

	companion object {
	    fun create(dir: MFile, explorer: ExplorerFragment): FileAdapter {
		val files = listFiles(dir, false)
		val items = MutableList<FileItem>(files.size, { i -> FileItem(files[i]) })
		
		Log.d("dir=${dir}")
		if (dir.absolutePath != "//")
		    items.add(0, FileItem(MFile(dir.absolutePath + "/.")))  // リストの一番上に "." を表示
		else
		    items.add(0, FileItem(MFile(dir.absolutePath + ".")))
		
		val adapter = FileAdapter(explorer.context!!, ArrayList<FileItem>())
		adapter.addAll(items)
		explorer.invokeNewItemsUpdater(items, adapter)
		
		return adapter
	    }
	}
    }
    
    private fun invokeNewItemsUpdater(newList: List<FileItem>, adapter: FileAdapter) {
	for (item in newList) {
	    supervisorScope.launch {
		withContext(Dispatchers.Default) {
		    item.retrieveMetadata()
		}
		withContext(Dispatchers.Main) {
		    adapter.notifyDataSetChanged()
		}
	    }
	}
    }
    
    private fun createListView(adapter: FileAdapter): ListView {
	val list = ListView(context)
        list.adapter = adapter
	
        list.setOnItemClickListener { parent, _, position, _ ->
	    val listView = parent as ListView
	    val item = listView.getItemAtPosition(position) as FileItem
	    Log.d("clicked=${item.filename}")

	    if (dirStack[dirStack.size - 1].listView == listView) {
		if (item.isDir) {
		    if (item.filename != ".")
			enterDir(item.file, true)
		} else {
		    play(item.file)
		}
	    }
        }
        list.setOnItemLongClickListener { parent, _, position, _ ->
	    val listView = parent as ListView
	    val item = listView.getItemAtPosition(position) as FileItem
	    Log.d("longclicked=${item.filename}")

	    var ret = false
	    if (item.isDir) {
		setTopDir(if (item.filename == ".") curDir else item.file)
		ret = true
	    }
	    ret
	}
	
	return list
    }
    
    private fun createDirFrame(path: MFile): DirFrame {
	val adapter = FileAdapter.create(path, this)
	val list = createListView(adapter)
	return DirFrame(path, adapter, list)
    }
    
    private fun enterDir(path: MFile, anime: Boolean) {
	val frame = createDirFrame(path)
	renewAdapter(frame.path)
	listViewportView.addView(frame.listView)
	val anim1 = TranslateAnimation(
	    Animation.RELATIVE_TO_PARENT, 1.5f,
	    Animation.RELATIVE_TO_PARENT, 0f,
	    Animation.RELATIVE_TO_PARENT, 0f,
	    Animation.RELATIVE_TO_PARENT, 0f
	)
	anim1.setDuration(if (anime) 300 else 0)
	anim1.setRepeatCount(0)
	anim1.setFillAfter(true)
	frame.listView.startAnimation(anim1)
	
	if (!dirStack.isEmpty()) {
	    val last = dirStack[dirStack.size - 1]
	    val anim0 = TranslateAnimation(
		Animation.RELATIVE_TO_PARENT, 0f,
		Animation.RELATIVE_TO_PARENT, -1.5f,
		Animation.RELATIVE_TO_PARENT, 0f,
		Animation.RELATIVE_TO_PARENT, 0f
	    )
	    anim0.setDuration(if (anime) 300 else 0)
	    anim0.setRepeatCount(0)
	    anim0.setFillAfter(true)
	    last.listView.startAnimation(anim0)
	}
	
	dirStack.add(frame)
    }
    
    private fun leaveDir(anime: Boolean): Boolean {
	if (dirStack.size < 2)
	    return false
	
	run {
	    val frame = dirStack[dirStack.size - 1]
	    val anim0 = TranslateAnimation(
		Animation.RELATIVE_TO_PARENT, 0f,
		Animation.RELATIVE_TO_PARENT, 1.5f,
		Animation.RELATIVE_TO_PARENT, 0f,
		Animation.RELATIVE_TO_PARENT, 0f
	    )
	    anim0.setDuration(if (anime) 300 else 0)
	    anim0.setRepeatCount(0)
	    anim0.setFillAfter(true)
	    frame.listView.startAnimation(anim0)
	    listViewportView.removeView(frame.listView)
	}
	
	run {
	    val frame = dirStack[dirStack.size - 2]
	    val anim0 = TranslateAnimation(
		Animation.RELATIVE_TO_PARENT, -1.5f,
		Animation.RELATIVE_TO_PARENT, 0f,
		Animation.RELATIVE_TO_PARENT, 0f,
		Animation.RELATIVE_TO_PARENT, 0f
	    )
	    anim0.setDuration(if (anime) 300 else 0)
	    anim0.setRepeatCount(0)
	    anim0.setFillAfter(true)
	    frame.listView.startAnimation(anim0)
	    
	    renewAdapter(frame.path)
	}
	
	dirStack.removeAt(dirStack.size - 1)
	
	return true
    }
    
    private data class DirFrame(val path: MFile, val adapter: FileAdapter, val listView: View)
    
    private lateinit var db: AppDatabase
    private lateinit var playContexts: PlayContextList
    private val rootDir: MFile = MFile("//")
    private var topDir: MFile = MFile("//")
    private var curDir: MFile = MFile("//")
    private lateinit var ctxt: PlayContext
    private lateinit var handler: Handler
    private val dirStack = mutableListOf<DirFrame>()    // [0]: //,  [last]: current
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.explorer_fragment, container, false)
	
	db = AppDatabase.getDB()
	playContexts = (activity!!.getApplication() as Application).getPlayContextList()

        handler = Handler()

	ctxt = playContexts.getCurrent()
	
	pathView = view.findViewById<PathView>(R.id.path)
	listViewportView = view.findViewById<RelativeLayout>(R.id.list_viewport)
	
	/* 再生中のファイルがある場所に移動 */
        var dir = MFile(ctxt.topDir)
        topDir = dir
	val path = ctxt.path
        if (path != null && path.startsWith(ctxt.topDir)) {
            val slash = path.lastIndexOf('/')
            if (slash != -1)
                dir = MFile(path.substring(0, slash))
        }
	/* 画面を回転させた場合は、その時いた場所に移動 */
        if (savedInstanceState != null) {
            val str = savedInstanceState.getString(STATE_CUR_DIR)
            if (str != null)
                dir = MFile(str)
        }

	val dirs = mutableListOf(MFile("//"))
	var slpos = 1
	while (true) {
	    slpos = dir.absolutePath.indexOf('/', slpos + 1)
	    if (slpos != -1) {
		dirs.add(MFile(dir.absolutePath.substring(0, slpos)))
	    } else {
		dirs.add(dir)
		break
	    }
	}
	for (mf in dirs)
	    enterDir(mf, false)
	
	(activity as MainActivity).setOnBackPressedListener { ->
            if (curDir == rootDir || curDir.absolutePath == "/") {
                false
	    } else {
		leaveDir(true)
		true
	    }
	}

	return view
    }

    /* 参考:
     *   http://stackoverflow.com/questions/12950215/onkeydown-and-onkeylongpress
     *
     * もうひとつ:
     *   Android N から onKeyLongPress が効かないので、自前で timeout を設けて処理する。
     *   https://issuetracker.google.com/issues/37106088
     */
    val backHandler = Handler()
    val backButtonRunnable = object : Runnable {
	override fun run() {
            backKeyShortPress = false
	    Log.d("finishing...")
            activity!!.finish()
	}
    }

/*
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
	Log.d("keyCode=${keyCode}, action=${event.action}")
        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
	    Log.d("start tracking.")
            event.startTracking()
	    Log.d("repeat count=${event.repeatCount}.")
            if (event.repeatCount == 0) {
                backKeyShortPress = true
		Log.d("longpress timeout=${ViewConfiguration.getLongPressTimeout().toLong()}.")
		backHandler.postDelayed(backButtonRunnable, ViewConfiguration.getLongPressTimeout().toLong())
	    }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
*/

/*
    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
	Log.d("keyCode=${keyCode}")
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            backKeyShortPress = false
	    Log.d("finishing...")
            finish()
            return true
        }
        return super.onKeyLongPress(keyCode, event)
    }
*/

/*
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
	Log.d("keyCode=${keyCode}")
        if (keyCode == KeyEvent.KEYCODE_BACK) {
	    backHandler.removeCallbacks(backButtonRunnable)
	    Log.d("backKeyShortPress=${backKeyShortPress}")
            if (backKeyShortPress) {
                if (curDir == rootDir || curDir.absolutePath == "/")
                    finish()
                else
		    leaveDir(true)
            }
            backKeyShortPress = false
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
*/

    private fun setTopDir(newDir: MFile) {
        topDir = newDir

        // topDir からの相対で curDir を表示
        pathView.rootDir = rootDir.toString()
        pathView.topDir = topDir.toString()
	var cur: String = "//"
	if (curDir.toString() != "//")
	    cur = curDir.toString() + "/"
        pathView.path = cur

	val c = context
	if (c != null)
            PlayerService.setTopDir(c, newDir.absolutePath)

	ctxt.topDir = newDir.absolutePath
	ctxt.path = null
	ctxt.pos = 0
	playContexts.put(ctxt.uuid)
    }

    private fun renewAdapter(newDir: MFile) {

        // topDir からの相対で newDir を表示
        pathView.rootDir = rootDir.toString()
	var cur: String = "//"
	if (newDir.toString() != "//")
	    cur = newDir.toString() + "/"
        pathView.path = cur
        pathView.topDir = topDir.toString()

        curDir = newDir
    }
    
    private fun play(file: MFile) {
	val c = context
	if (c != null)
            PlayerService.play(c, file.absolutePath)
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(STATE_CUR_DIR, curDir.absolutePath)
    }

    public override fun onDestroy() {
	supervisorJob.cancel()
	runBlocking {
	    supervisorJob.join()
	}

        super.onDestroy()
    }

    companion object {
        private val STATE_CUR_DIR = "me.masm11.contextplayer.CUR_DIR"

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
        fun listFiles(dir: MFile, reverse: Boolean): Array<MFile> {
	    Log.d("listFiles: dir: ${dir}")
            val files = dir.listFiles { f -> !f.name.startsWith(".") }
	    Log.d("listFiles: files: ${files}")
	    if (files == null)
		return emptyArray<MFile>()
	    Log.d("listFiles: files: not empty.")

            var comparator = Comparator<MFile> { o1, o2 ->
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
