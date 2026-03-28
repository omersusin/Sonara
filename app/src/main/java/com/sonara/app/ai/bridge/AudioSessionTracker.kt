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
