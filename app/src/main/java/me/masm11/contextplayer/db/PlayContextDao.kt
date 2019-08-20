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
    // fixme: 必要?
    @Query("SELECT * FROM PlayContext ORDER BY id")
    abstract fun getAll(): List<PlayContext>
    
    // context を作成
    @Insert
    abstract fun insert(ctxt: PlayContext)
    
/*
    // updateAll から使用
    @Insert
    abstract fun insertPlayContexts(ctxt: List<PlayContext>)
*/

/*
    // 表示順序も含めて、画面に表示しているものをそのまま DB に反映
    fun updateAll(ctxts: List<PlayContext>) {
	deleteAll()
	insertPlayContexts(ctxts)
    }
*/

    // updateAll から使用。ただし、他から使いたければ、それも可?
    @Query("DELETE FROM PlayContext")
    abstract fun deleteAll()
    
    // 特定の context を得る
    @Query("SELECT * FROM PlayContext WHERE id = :id")
    abstract fun find(id: Long): PlayContext?
    
    // context を id 順に得る。fixme: 表示順に変更する。
    @Query("SELECT * FROM PlayContext ORDER BY id")
    abstract fun all(): PlayContext?

    @Update
    abstract fun update(ctxt: PlayContext)
    
    @Query("DELETE FROM PlayContext WHERE id = :id")
    abstract fun delete(id: Long)
    
    // まとめて insert や delete する方法ないかな。
}
