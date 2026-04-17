/*
 * Sonara - AI-powered audio equalizer
 * Copyright (C) 2024-2026 Sonara
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sonara.app.ai.bridge

import java.util.concurrent.atomic.AtomicInteger

object AudioSessionTracker {
    private val currentId = AtomicInteger(0)
    private val listeners = mutableListOf<(Int) -> Unit>()

    fun set(id: Int) {
        val old = currentId.getAndSet(id)
        if (old != id && id > 0) {
            synchronized(listeners) { listeners.forEach { it(id) } }
        }
    }

    fun clear(id: Int) { currentId.compareAndSet(id, 0) }
    fun get(): Int = currentId.get()
    fun has(): Boolean = currentId.get() > 0

    fun addListener(l: (Int) -> Unit) { synchronized(listeners) { listeners.add(l) } }
}
