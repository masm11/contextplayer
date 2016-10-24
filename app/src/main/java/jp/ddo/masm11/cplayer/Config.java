package jp.ddo.masm11.cplayer;

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
    
    public static Config findByKey(String key) {
	return new Select()
		.from(Config.class)
		.where("key = ?", key)
		.executeSingle();
    }
}
