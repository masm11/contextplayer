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

@Dao
abstract class ConfigDao {
    private val db: AppDatabase
    constructor(db: AppDatabase) {
	this.db = db
    }
    
    @Query("SELECT * FROM Config")
    abstract fun getAll(): List<Config>
    
    @Query("SELECT * FROM Config WHERE key = :key")
    abstract fun findByKey(key: String): Config?
    
    @Insert
    abstract fun insert(config: Config)
    
    @Update
    abstract fun update(config: Config)
    
    fun getContextId(): String {
	val config: Config? = findByKey("context_id")
	if (config == null) {
	    // 設定がない場合。
	    // context があるなら、どれか 1つの id を返す。
	    val ctxts = db.playContextDao().getAll()
	    if (ctxts.size >= 1)
		return ctxts.get(0).uuid
	    
	    // context が一つもないなら、新規に作って id を返す。
	    val ctxt = PlayContext()
	    db.playContextDao().insert(ctxt)
	    return ctxt.uuid
	}
	return config.value
    }
    
    fun setContextId(value: String) {
	var config: Config? = findByKey("context_id")
	if (config == null) {
	    config = Config()
	    config.key = "context_id"
	    config.value = value
	    insert(config)
	} else {
	    config.value = value
	    update(config)
	}
    }
}
