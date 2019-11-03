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

import java.util.UUID

import androidx.annotation.NonNull
import androidx.room.Ignore
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index

import me.masm11.contextplayer.util.MutableWeakSet

@Entity(
    tableName = "PlayContext_2",
    indices = arrayOf(
	Index(
	    value = [ "current" ],
	    unique = true
	)
    )
)
class PlayContext {
    constructor() {
	uuid = UUID.randomUUID().toString()
    }
    
    @PrimaryKey
    var uuid: String
    
    @NonNull
    var name: String = "既定のコンテキスト"
    
    @NonNull
    var topDir: String = "//"
    
    var path: String? = null
    
    var pos: Long = 0		// msec
    
    var volume: Int = 100
    
    @ColumnInfo(name = "current") var current: Int? = null
    
    @Ignore var realtimePos: Long = 0
    @Ignore var realtimeDuration: Long = 0
    
    @Ignore val onChangedListener = MutableWeakSet<(PlayContext) -> Unit>()
    fun addOnChangedListener(listener: (PlayContext) -> Unit) {
	onChangedListener.add(listener)
    }
    fun removeOnChangedListener(listener: (PlayContext) -> Unit) {
	onChangedListener.remove(listener)
    }
    
    inner class PlayContextTransaction: AutoCloseable {
	val owner = this@PlayContext
	var topDir: String = owner.topDir
	set(topDir) {
	    if (field != topDir) {
		field = topDir
		topDirChanged = true
	    }
	}
	var path: String? = owner.path
	set(path) {
	    if (field != path) {
		field = path
		pathChanged = true
	    }
	}
	var pos: Long = owner.pos
	set(pos) {
	    if (field != pos) {
		field = pos
		posChanged = true
	    }
	}
	var volume: Int = owner.volume
	set(volume) {
	    if (field != volume) {
		field = volume
		volumeChanged = true
	    }
	}
	var realtimePos: Long = owner.realtimePos
	set(realtimePos) {
	    if (field != realtimePos) {
		field = realtimePos
		realtimePosChanged = true
	    }
	}
	var realtimeDuration: Long = owner.realtimeDuration
	set(realtimeDuration) {
	    if (field != realtimeDuration) {
		field = realtimeDuration
		realtimeDurationChanged = true
	    }
	}
	
	var topDirChanged = false
	var pathChanged = false
	var posChanged = false
	var volumeChanged = false
	var realtimePosChanged = false
	var realtimeDurationChanged = false
	
	override fun close() {
	    val owner = this@PlayContext
	    var changed = false
	    if (topDirChanged) {
		owner.topDir = topDir
		changed = true
	    }
	    if (pathChanged) {
		owner.path = path
		changed = true
	    }
	    if (posChanged) {
		owner.pos = pos
		changed = true
	    }
	    if (volumeChanged) {
		owner.volume = volume
		changed = true
	    }
	    if (realtimePosChanged) {
		owner.realtimePos = realtimePos
		changed = true
	    }
	    if (realtimeDurationChanged) {
		owner.realtimeDuration = realtimeDuration
		changed = true
	    }
	    if (changed) {
		for (listener in onChangedListener)
		    listener(owner)
	    }
	}
    }

    fun withTransaction(): PlayContextTransaction {
	return PlayContextTransaction()
    }

    override fun toString(): String {
	return name
    }
}
