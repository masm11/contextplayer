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

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ Config::class, PlayContext::class ], version = 2)
abstract class AppDatabase: RoomDatabase() {
    abstract fun configDao(): ConfigDao
    abstract fun playContextDao(): PlayContextDao

    companion object {
	private lateinit var app: Context
	fun setApplication(app: Context) {
	    this.app = app
	}
	
	private val migration_1_2 = object: Migration(1, 2) {
	    override fun migrate(database: SupportSQLiteDatabase) {
		database.execSQL(
		    "CREATE TABLE PlayContext_2 (" +
		    "uuid TEXT NOT NULL PRIMARY KEY," +
		    "name TEXT NOT NULL," +
		    "topDir TEXT NOT NULL," +
		    "path TEXT," +
		    "pos INTEGER NOT NULL," +
		    "volume INTEGER NOT NULL" +
		    ")"
		)
		database.execSQL("INSERT INTO PlayContext_2 SELECT CAST(id AS TEXT), name, topDir, path, pos, volume FROM PlayContext")

		database.execSQL(
		    "CREATE TABLE Config_2 (" +
		    "uuid TEXT NOT NULL PRIMARY KEY," +
		    "key TEXT NOT NULL," +
		    "value TEXT NOT NULL" +
		    ")"
		)
		database.execSQL("INSERT INTO Config_2 SELECT CAST(id AS TEXT), key, value FROM Config")
		database.execSQL("CREATE UNIQUE INDEX index_Config_2_key ON Config_2 (key)")
	    }
	}

	private var appdb: AppDatabase? = null
	fun getDB(): AppDatabase {
	    var db = appdb
	    if (db == null) {
	        db = Room.databaseBuilder(app, AppDatabase::class.java, "ContextPlayer.db").addMigrations(migration_1_2).build()
		appdb = db
	    }
	    return db
	}
    }
}
