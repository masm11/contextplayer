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

@Entity
class PlayContext {
    constructor() {
	val uuid = UUID.randomUUID()
	val hi = uuid.getMostSignificantBits()
	val lo = uuid.getLeastSignificantBits()
	id = hi xor lo
    }
    
    fun dup(): PlayContext {
	val r = PlayContext()
	r.id = this.id
	r.name = this.name
	r.topDir = this.topDir
	r.path = this.path
	r.pos = this.pos
	r.volume = this.volume
	r.deleted = this.deleted
	return r
    }

    @PrimaryKey
    var id: Long
    
    @NonNull
    var name: String = ""
    
    @NonNull
    var topDir: String = ""
    
    var path: String? = null
    
    var pos: Long = 0		// msec
    
    var volume: Int = 100
    
    @Ignore
    var deleted: Boolean = false
    
    override fun toString(): String {
	return name
    }
}
