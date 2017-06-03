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
package jp.ddo.masm11.contextplayer.util

import java.util.Collections
import java.util.WeakHashMap

fun <T> emptyMutableListOf(): MutableList<T> {
    return ArrayList<T>()
}

// delegation 使えばもっと簡単に書けそうな気もする。

open class WeakSet<E>: Set<E> {
    protected val set = Collections.newSetFromMap(WeakHashMap<E, Boolean>())

    override val size: Int
	get() { return set.size }

    override fun contains(element: E): Boolean = set.contains(element)
    override fun containsAll(elements: Collection<E>): Boolean = set.containsAll(elements)
    override fun isEmpty(): Boolean = set.isEmpty()
    override fun iterator(): Iterator<E> = set.iterator()
}

class MutableWeakSet<E>: WeakSet<E>(), MutableSet<E> {
    override fun add(element: E): Boolean = set.add(element)
    override fun addAll(elements: Collection<E>): Boolean = set.addAll(elements)
    override fun clear() = set.clear()
    override fun iterator(): MutableIterator<E> = set.iterator()
    override fun remove(element: E): Boolean = set.remove(element)
    override fun removeAll(elements: Collection<E>): Boolean = set.removeAll(elements)
    override fun retainAll(elements: Collection<E>): Boolean = set.retainAll(elements)
}
