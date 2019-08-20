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

class PlayContextList {
    private val db = AppDatabase.getDB()
    private val dao = db.playContextDao()
    private val dat = HashMap<Long, PlayContext>()
    private val updater = Updater()
    private val thread = Thread(updater)
    
    init {
	val list = dao.getAll()
	for (ctxt in list)
	    dat.put(ctxt.id, ctxt)
	
	thread.setDaemon(true)
	thread.start()
    }
    
    fun ids(): LongArray {
	return dat.keys.toLongArray()
    }
    
    fun get(id: Long): PlayContext? {
	return dat.get(id)
    }
    
    fun put(id: Long) {
	val ctxt = dat.get(id)
	if (ctxt != null) {
	    val ct = ctxt.dup()
	    updater.enqueue(ct)
	}
    }
    
    fun delete(id: Long) {
	val ctxt = dat.remove(id)
	if (ctxt != null) {
	    val ct = ctxt.dup()
	    ct.deleted = true
	    updater.enqueue(ct)
	}
    }
    
    fun new(): PlayContext {
	return PlayContext()
    }
    
    private inner class Updater: Runnable {
	private val mutex = ReentrantLock()
	private val cond = mutex.newCondition()
	private val jobs: MutableList<PlayContext> = ArrayList()
	
	fun enqueue(ctxt: PlayContext) {
	    mutex.lock()
	    jobs.add(ctxt)
	    cond.signal()
	    mutex.unlock()
	}
	
	override fun run() {
	    try {
		while (true) {
		    mutex.lock()
		    while (jobs.size == 0)
			cond.await()
		    val ctxt = jobs.removeAt(0)
		    mutex.unlock()
		    if (ctxt != null)
			dao.update(ctxt)
		    else
			dao.delete(ctxt.id)
		}
	    } catch (e: InterruptedException) {
	    }
	}
    }
}
