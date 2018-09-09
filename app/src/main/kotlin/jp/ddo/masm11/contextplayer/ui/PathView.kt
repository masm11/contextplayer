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
package jp.ddo.masm11.contextplayer.ui

import android.content.Context
import android.widget.TextView
import android.text.SpannableString
import android.text.Spannable
import android.text.style.BackgroundColorSpan
import android.util.AttributeSet

class PathView(context: Context, attrs: AttributeSet) : TextView(context, attrs) {
    var rootDir: String = "//"
        set(rootDir) {
	    field = rootDir
	    updateText()
	}
    var topDir: String = "//"
	set(topDir) {
	    field = topDir
	    updateText()
	}
    var path: String? = null
	set(path) {
	    field = path;
            updateText()
	}

    private fun updateText() {
        var top = topDir
        var cur = path ?: top
        if (!top.endsWith("/"))
            top = "${top}/"

        if (cur.startsWith(top)) {
            val ss = SpannableString(cur)
            ss.setSpan(span, 0, top.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            text = ss
        } else {
            text = cur
        }
    }

    companion object {
        private val span = BackgroundColorSpan(0xffdddddd.toInt())
    }
}
