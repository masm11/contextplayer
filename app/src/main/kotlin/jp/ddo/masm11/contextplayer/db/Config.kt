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
	fun loadContextId(): Long {
	    val config: Config? = findByKey("context_id")
	    if (config == null)
		return PlayContext.all().get(0).getId()
	    val id = config.value
	    if (id == null)
		return 0
	    return id.toLong()
	}

	fun saveContextId(context_id: Long) {
	    var config: Config? = findByKey("context_id")
	    if (config == null) {
		config = Config()
		config.key = "context_id"
	    }
	    config.value = context_id.toString()
	    config.save()
	}

	fun loadVolume(): Int {
	    val config: Config? = findByKey("volume")
	    if (config == null)
		return 100
	    val vol = config.value
	    if (vol == null)
		return 100
	    return vol.toInt()
	}

	fun saveVolume(volume: Int) {
	    var config: Config? = findByKey("volume")
	    if (config == null) {
		config = Config()
		config.key = "volume"
	    }
	    config.value = volume.toString()
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
