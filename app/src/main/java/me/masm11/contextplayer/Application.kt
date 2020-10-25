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
package me.masm11.contextplayer

import androidx.room.Room

import me.masm11.contextplayer.util.Log
import me.masm11.contextplayer.db.AppDatabase
import me.masm11.contextplayer.db.PlayContextList

class Application : android.app.Application() {
    private lateinit var playContexts: PlayContextList
    fun getPlayContextList(): PlayContextList {
	return playContexts
    }
    override fun onCreate() {
        super.onCreate()
	
        Log.init()
	
	AppDatabase.setApplication(this)
	playContexts = PlayContextList()
    }
}
