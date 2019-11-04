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
package me.masm11.contextplayer.db

import java.util.concurrent.locks.ReentrantLock

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

import me.masm11.contextplayer.util.MutableWeakSet

class PlayContextList {
    private val db = AppDatabase.getDB()
    private val dao = db.playContextDao()
    private val hash = HashMap<String, PlayContext>()	// key: uuid
    private val updaterChannel = Channel<() -> Unit>(Channel.UNLIMITED)
    private val onContextSwitchListeners = MutableWeakSet<(PlayContext) -> Unit>()
    
    init {
	/* 起動時の処理なので、
	*  ブロックしてでも全部読む。
	*/
	runBlocking {
	    GlobalScope.launch(context=Dispatchers.IO) {
		val list = dao.getAll()
		for (ctxt in list)
		    hash.put(ctxt.uuid, ctxt)
	    }.join()
	}
	
	GlobalScope.launch(context=Dispatchers.IO) {
	    while (true) {
		val block = updaterChannel.receive()
		block()
	    }
	}
	
	/* 一つも存在しない場合は作成しておく */
	if (hash.size == 0)
	    new()
    }
    
    fun uuids(): Set<String> {
	return hash.keys
    }
    
    fun get(uuid: String): PlayContext? {
	return hash.get(uuid)
    }
    
    private fun enqueue_job(block: () -> Unit) {
	/* unlimited なので block はしないはず。
	* いや queue へのアクセスが競合したら block することもあるか。
	*/
	runBlocking {
	    updaterChannel.send(block)
	}
    }
    
    fun getCurrent(): PlayContext {
	for (ctxt in hash.values) {
	    if (ctxt.current == CURRENT)
		return ctxt
	}
	for (ctxt in hash.values)
	    return ctxt
	return new()
    }
    
    fun setCurrent(ctxt: PlayContext) {
	if (ctxt.current == CURRENT)
	    return
	val cur = getCurrent()
	cur.current = null
	enqueue_job {
	    dao.update(cur)
	}
	ctxt.current = CURRENT
	enqueue_job {
	    dao.update(ctxt)
	}
	for (listener in onContextSwitchListeners)
	    listener(ctxt)
    }
    
    fun put(uuid: String) {
	val ctxt = hash.get(uuid)
	if (ctxt != null) {
	    enqueue_job {
		dao.update(ctxt)
	    }
	}
    }
    
    fun delete(uuid: String) {
	val ctxt = hash.remove(uuid)
	if (ctxt != null) {
	    enqueue_job {
		dao.delete(ctxt.uuid)
	    }
	}
    }
    
    fun new(): PlayContext {
	val ctxt = PlayContext()
	ctxt.displayOrder = nextDisplayOrder()
	hash.put(ctxt.uuid, ctxt)
	enqueue_job {
	    dao.insert(ctxt)
	}
	return ctxt
    }
    
    private fun nextDisplayOrder(): Int {
	var lastDisplayOrder = -1
	for (ctxt in hash.values) {
	    if (lastDisplayOrder < ctxt.displayOrder)
		lastDisplayOrder = ctxt.displayOrder
	}
	return lastDisplayOrder + 1
    }

    fun addOnContextSwitchListener(listener: (PlayContext) -> Unit) {
	onContextSwitchListeners.add(listener)
    }
    
    fun removeOnContextSwitchListener(listener: (PlayContext) -> Unit) {
	onContextSwitchListeners.remove(listener)
    }

    companion object {
	val CURRENT = 1
    }
}
