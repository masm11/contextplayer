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

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Update
import android.arch.persistence.room.Delete

@Dao
abstract class PlayContextDao {
    @Query("SELECT * FROM PlayContext ORDER BY id")
    abstract fun getAll(): List<PlayContext>
    
    @Insert
    abstract fun insert(ctxt: PlayContext)
    
    @Update
    abstract fun update(ctxt: PlayContext)
    
    @Delete
    abstract fun delete(ctxt: PlayContext)
    
    @Query("SELECT * FROM PlayContext WHERE id = :id")
    abstract fun find(id: Long): PlayContext?
}
