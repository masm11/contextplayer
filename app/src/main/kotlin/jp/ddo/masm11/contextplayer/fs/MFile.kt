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
package jp.ddo.masm11.contextplayer.fs

import java.io.File

import jp.ddo.masm11.logger.Log

import android.os.Environment
import android.content.Context

// Mapped file
class MFile(val path: String) {
    constructor(file: File) : this(file.toString())

    val mapping: HashMap<String, String> = HashMap()
    init {
	mapping.put("primary", "/storage/emulated/0")
	mapping.put("9016-4EF8", "/storage/9016-4EF8")
    }
    
    val isDirectory: Boolean
    get() {
	return File(path).isDirectory
    }

    val absolutePath: String
    get() {
	return File(path).absolutePath
    }
    
    val name: String
    get() {
	val i = path.lastIndexOf('/')
	return path.substring(i + 1)
    }

    override fun toString(): String {
	return path;
    }

    fun parentMFile(): MFile {
	if (path == "//")
	    return this
	else {
	    val i = path.lastIndexOf('/')
	    if (i < 2)
	        return MFile("//")
	    else
	        return MFile(path.substring(0, i))
	}
    }

    val file: File
    get() {
	// fixme:
	return File(path)
    }

    fun listFiles(): Array<MFile>? {
	if (path == "//") {
	    return mapping.keys.map<String, MFile>{ s ->
		MFile(s)
	    }.toTypedArray()
	} else {
	    val files = file.listFiles()
	    if (files == null)
	        return null
	    return files.map<File, MFile>{ f -> 
		MFile(path + "/" + f.name)
	    }.toTypedArray()
	}
    }

    val parentFile: MFile
    get() {
	if (path == "//")
	    return this
	val i = path.lastIndexOf('/')
	return MFile(path.substring(0, i))
    }

    fun compareTo(file: MFile): Int {
	return this.absolutePath.compareTo(file.absolutePath)
    }
}
