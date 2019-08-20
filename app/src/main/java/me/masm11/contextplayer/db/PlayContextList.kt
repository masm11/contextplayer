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
    private val dat = HashMap<String, PlayContext>()
    private val updater = Updater()
    private val thread = Thread(updater)
    
    init {
	val list = dao.getAll()
	for (ctxt in list)
	    dat.put(ctxt.uuid, ctxt)
	
	thread.setDaemon(true)
	thread.start()
    }
    
    fun uuids(): Set<String> {
	return dat.keys
    }
    
    fun get(uuid: String): PlayContext? {
	return dat.get(uuid)
    }
    
    fun put(uuid: String) {
	val ctxt = dat.get(uuid)
	if (ctxt != null) {
	    val ct = ctxt.dup()
	    updater.enqueue(ct)
	}
    }
    
    fun delete(uuid: String) {
	val ctxt = dat.remove(uuid)
	if (ctxt != null) {
	    val ct = ctxt.dup()
	    ct.deleted = true
	    updater.enqueue(ct)
	}
    }
    
    fun new(): PlayContext {
	val ctxt = PlayContext()
	dat.put(ctxt.uuid, ctxt)
	val ct = ctxt.dup()
	ct.created = true
	updater.enqueue(ct)
	return ctxt
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
		    if (ctxt.deleted)
			dao.delete(ctxt.uuid)
		    else if (ctxt.created)
			dao.insert(ctxt)
		    else
			dao.update(ctxt)
		}
	    } catch (e: InterruptedException) {
	    }
	}
    }
}
