/* Context Player - Audio Player with Contexts
    Copyright (C) 2016, 2017 Yuuki Harano

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
package jp.ddo.masm11.contextplayer.db

import com.activeandroid.Model
import com.activeandroid.query.Select
import com.activeandroid.annotation.Column
import com.activeandroid.annotation.Table

@Table(name = "PlayContexts")
class PlayContext: Model() {
    
    @Column(name = "name", notNull = true)
    var name: String = ""
    
    @Column(name = "topdir", notNull = true)
    var topDir: String = ""
    
    @Column(name = "path")
    var path: String? = null
    
    @Column(name = "pos")
    var pos: Long = 0		// msec
    
    @Column(name = "volume")
    var volume: Int = 100

    companion object {
	fun find(id: Long): PlayContext? {
	    return Select()
		    .from(PlayContext::class.java)
		    .where("id = ?", id)
		    .executeSingle()
	}

	fun all(): List<PlayContext> {
	    val ctxts = Select()
		    .from(PlayContext::class.java)
		    .orderBy("id")
		    .execute<PlayContext>()
	    return ctxts.toList()
	}
    }
    
    override fun toString(): String {
	return name
    }
}
