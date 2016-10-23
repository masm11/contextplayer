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
    public long pos;
    
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
