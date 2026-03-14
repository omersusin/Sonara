package com.sonara.app.preset
data class Preset(val id: Long = 0, val name: String = "", val bands: FloatArray = FloatArray(10), val bassBoost: Int = 0, val virtualizer: Int = 0, val loudness: Int = 0, val isBuiltIn: Boolean = false)
