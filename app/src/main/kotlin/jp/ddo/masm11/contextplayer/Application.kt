/* Context Player - Audio Player with Contexts
    Copyright (C) 2016, 2017 Yuuki Harano

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
package jp.ddo.masm11.contextplayer

import android.arch.persistence.room.Room

import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric

import jp.ddo.masm11.logger.Log
import jp.ddo.masm11.contextplayer.db.AppDatabase

class Application : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
	
        Fabric.with(this, Crashlytics())
        Log.init(getExternalFilesDir(null), BuildConfig.DEBUG)
	
	AppDatabase.setApplication(this)
    }
}
