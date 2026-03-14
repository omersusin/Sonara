package com.sonara.app.data.models

data class HeadphoneInfo(val name: String = "", val address: String = "", val type: ConnectionType = ConnectionType.UNKNOWN, val isConnected: Boolean = false)
enum class ConnectionType { WIRED, BLUETOOTH_A2DP, BLUETOOTH_LE, USB, UNKNOWN }
