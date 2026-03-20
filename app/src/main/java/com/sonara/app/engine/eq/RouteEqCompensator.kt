package com.sonara.app.engine.eq

class RouteEqCompensator {
    companion object {
        private val SPEAKER = shortArrayOf(250, 100, 0, -50, -100)
        private val BT = shortArrayOf(150, 50, 50, 100, 50)
        private val WIRED = shortArrayOf(0, 0, 0, 0, 0)
    }
    fun apply(bands: ShortArray, route: EqSessionController.AudioRoute): ShortArray {
        val offset = when (route) { EqSessionController.AudioRoute.SPEAKER -> SPEAKER; EqSessionController.AudioRoute.BLUETOOTH -> BT; EqSessionController.AudioRoute.WIRED -> WIRED }
        return ShortArray(bands.size) { i -> (bands[i] + offset.getOrElse(i) { 0 }).toShort().coerceIn(-1500, 1500) }
    }
    fun neutralize(bands: ShortArray, sourceRoute: EqSessionController.AudioRoute): ShortArray {
        val offset = when (sourceRoute) { EqSessionController.AudioRoute.SPEAKER -> SPEAKER; EqSessionController.AudioRoute.BLUETOOTH -> BT; EqSessionController.AudioRoute.WIRED -> WIRED }
        return ShortArray(bands.size) { i -> (bands[i] - offset.getOrElse(i) { 0 }).toShort().coerceIn(-1500, 1500) }
    }
}
