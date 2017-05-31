/* Context Player - Audio Player with Contexts
    Copyright (C) 2016 Yuuki Harano

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

@Table(name = "Configs")
class Config: Model() {
    
    @Column(name = "key", notNull = true, unique = true)
    var key: String = ""
    
    @Column(name = "value")
    var value: String = ""
    
    companion object {
	var context_id: Long
	    get () {
		val config: Config? = findByKey("context_id")
		if (config == null) {
		    // 設定がない場合。
		    // context があるなら、どれか 1つの id を返す。
		    val ctxts = PlayContext.all()
		    if (ctxts.size >= 1)
			return ctxts.get(0).id

		    // context が一つもないなら、新規に作って id を返す。
		    val ctxt = PlayContext()
		    ctxt.save()
		    return ctxt.id
		}
		return config.value.toLong()
	    }
	    set(value) {
		var config: Config? = findByKey("context_id")
		if (config == null) {
		    config = Config()
		    config.key = "context_id"
		}
		config.value = value.toString()
		config.save()
	    }

	var volume: Int
	    get() {
		val config: Config? = findByKey("volume")
		if (config == null)
		    return 100
		return config.value.toInt()
	    }
	    set(value) {
		var config: Config? = findByKey("volume")
		if (config == null) {
		    config = Config()
		    config.key = "volume"
		}
		config.value = value.toString()
		config.save()
	    }

	fun findByKey(key: String): Config? {
	    return Select()
		    .from(Config::class.java)
		    .where("key = ?", key)
		    .executeSingle()
	}
    }
}
