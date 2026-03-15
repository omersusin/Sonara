package com.sonara.app.autoeq

import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import com.sonara.app.data.models.ConnectionType
import com.sonara.app.data.models.HeadphoneInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HeadphoneDetector(private val context: Context) {
    private val _headphone = MutableStateFlow(HeadphoneInfo())
    val headphone: StateFlow<HeadphoneInfo> = _headphone.asStateFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) { scan() }
        override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) { scan() }
    }

    fun start() {
        audioManager.registerAudioDeviceCallback(callback, handler)
        scan()
    }

    fun stop() {
        try { audioManager.unregisterAudioDeviceCallback(callback) } catch (_: Exception) {}
    }

    fun scan() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val hp = devices.firstOrNull { isHeadphone(it) }

        if (hp != null) {
            var name = hp.productName?.toString()?.takeIf { it.isNotBlank() && it != "null" } ?: ""

            if (name.isBlank() && (hp.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || hp.type == AudioDeviceInfo.TYPE_BLE_HEADSET)) {
                name = getBluetoothDeviceName() ?: "Bluetooth Device"
            }

            if (name.isBlank()) name = when (hp.type) {
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headphones"
                AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headphones"
                else -> "Headphones"
            }

            val type = when (hp.type) {
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> ConnectionType.WIRED
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> ConnectionType.BLUETOOTH_A2DP
                AudioDeviceInfo.TYPE_BLE_HEADSET -> ConnectionType.BLUETOOTH_LE
                AudioDeviceInfo.TYPE_USB_HEADSET -> ConnectionType.USB
                else -> ConnectionType.UNKNOWN
            }

            _headphone.value = HeadphoneInfo(name = name, type = type, isConnected = true)
        } else {
            _headphone.value = HeadphoneInfo()
        }
    }

    private fun isHeadphone(d: AudioDeviceInfo): Boolean = d.type in listOf(
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_USB_HEADSET
    )

    private fun getBluetoothDeviceName(): String? {
        return try {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = btManager?.adapter ?: return null
            val connected = adapter.getProfileConnectionState(BluetoothProfile.A2DP)
            if (connected == BluetoothProfile.STATE_CONNECTED) {
                adapter.bondedDevices?.firstOrNull()?.name
            } else null
        } catch (_: SecurityException) { null }
    }
}
