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
package jp.ddo.masm11.contextplayer.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Update

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
    
    fun getContextId(): Long {
	val config: Config? = findByKey("context_id")
	if (config == null) {
	    // 設定がない場合。
	    // context があるなら、どれか 1つの id を返す。
	    val ctxts = db.playContextDao().getAll()
	    if (ctxts.size >= 1)
		return ctxts.get(0).id
	    
	    // context が一つもないなら、新規に作って id を返す。
	    val ctxt = PlayContext()
	    db.playContextDao().insert(ctxt)
	    return ctxt.id
	}
	return config.value.toLong()
    }
    
    fun setContextId(value: Long) {
	var config: Config? = findByKey("context_id")
	if (config == null) {
	    config = Config()
	    config.key = "context_id"
	    config.value = value.toString()
	    insert(config)
	} else {
	    config.value = value.toString()
	    update(config)
	}
    }
}
