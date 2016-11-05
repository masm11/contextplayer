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
package jp.ddo.masm11.cplayer;

import com.activeandroid.Model;
import com.activeandroid.query.Select;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import java.util.List;

@Table(name = "PlayContexts")
public class PlayContext extends Model {
    
    @Column(name = "name", notNull = true)
    public String name;
    
    @Column(name = "topdir", notNull = true)
    public String topDir;
    
    @Column(name = "path")
    public String path;
    
    @Column(name = "pos")
    public long pos;	// msec
    
    public PlayContext() {
	super();
    }
    
    public static PlayContext find(long id) {
	return new Select()
		.from(PlayContext.class)
		.where("id = ?", id)
		.executeSingle();
    }
    
    public static List<PlayContext> all() {
	return new Select()
		.from(PlayContext.class)
		.orderBy("id")
		.execute();
    }
    
    @Override
    public String toString() {
	return name;
    }
}
