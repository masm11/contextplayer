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

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete

@Dao
abstract class PlayContextDao {
    // 一覧を取得
    @Query("SELECT * FROM PlayContext_2 ORDER BY uuid")
    abstract fun getAll(): List<PlayContext>
    
    // context を作成
    @Insert
    abstract fun insert(ctxt: PlayContext)
    
    @Update
    abstract fun update(ctxt: PlayContext)
    
    @Query("DELETE FROM PlayContext_2 WHERE uuid = :uuid")
    abstract fun delete(uuid: String)
}
