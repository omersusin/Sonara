package com.sonara.app.autoeq

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.sonara.app.data.models.ConnectionType
import com.sonara.app.data.models.HeadphoneInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HeadphoneDetector(private val context: Context) {
    private val _headphone = MutableStateFlow(HeadphoneInfo())
    val headphone: StateFlow<HeadphoneInfo> = _headphone.asStateFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) { scan() }
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) { scan() }
    }

    fun start() {
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        scan()
    }

    fun stop() {
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
    }

    fun scan() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val headphone = devices.firstOrNull { isHeadphone(it) }

        if (headphone != null) {
            val name = headphone.productName?.toString() ?: getBluetoothName() ?: "Unknown"
            val type = when (headphone.type) {
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

    private fun isHeadphone(device: AudioDeviceInfo): Boolean = device.type in listOf(
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_USB_HEADSET
    )

    private fun getBluetoothName(): String? {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return null
            adapter.bondedDevices?.firstOrNull { it.type != 0 }?.name
        } catch (e: SecurityException) { null }
    }
}
