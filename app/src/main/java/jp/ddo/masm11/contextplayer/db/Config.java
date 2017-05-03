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
package jp.ddo.masm11.contextplayer.db;

import com.activeandroid.Model;
import com.activeandroid.query.Select;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name = "Configs")
public class Config extends Model {
    
    @Column(name = "key", notNull = true, unique = true)
    public String key;
    
    @Column(name = "value")
    public String value;
    
    public Config() {
	super();
    }
    
    public static long loadContextId() {
	Config config = findByKey("context_id");
	if (config == null)
	    return PlayContext.all().get(0).getId();
	return Long.parseLong(config.value);
    }
    
    public static void saveContextId(long context_id) {
	Config config = findByKey("context_id");
	if (config == null) {
	    config = new Config();
	    config.key = "context_id";
	}
	config.value = new Long(context_id).toString();
	config.save();
    }
    
    public static int loadVolume() {
	Config config = findByKey("volume");
	if (config == null)
	    return 100;
	return Integer.parseInt(config.value);
    }
    
    public static void saveVolume(int volume) {
	Config config = findByKey("volume");
	if (config == null) {
	    config = new Config();
	    config.key = "volume";
	}
	config.value = new Integer(volume).toString();
	config.save();
    }

    public static Config findByKey(String key) {
	return new Select()
		.from(Config.class)
		.where("key = ?", key)
		.executeSingle();
    }
}
