package com.sonara.app.autoeq
import com.sonara.app.data.SonaraLogger

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
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
    private val TAG = "HeadphoneDetector"
    private val _headphone = MutableStateFlow(HeadphoneInfo())
    val headphone: StateFlow<HeadphoneInfo> = _headphone.asStateFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var a2dpProxy: BluetoothA2dp? = null

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) { scan() }
        override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) { scan() }
    }

    private val btProfileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.A2DP) {
                a2dpProxy = proxy as BluetoothA2dp
                SonaraLogger.bt( "A2DP proxy connected")
                scan()
            }
        }
        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.A2DP) { a2dpProxy = null }
        }
    }

    fun start() {
        audioManager.registerAudioDeviceCallback(callback, handler)
        // Get A2DP proxy for accurate BT device name
        try {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            btManager?.adapter?.getProfileProxy(context, btProfileListener, BluetoothProfile.A2DP)
        } catch (e: SecurityException) {
            SonaraLogger.w("BT", "BT permission denied: ${e.message}")
        }
        scan()
    }

    fun stop() {
        try { audioManager.unregisterAudioDeviceCallback(callback) } catch (_: Exception) {}
        try {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            a2dpProxy?.let { btManager?.adapter?.closeProfileProxy(BluetoothProfile.A2DP, it) }
        } catch (_: Exception) {}
    }

    fun scan() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val hp = devices.firstOrNull { isHeadphone(it) }

        if (hp != null) {
            var name = hp.productName?.toString()?.takeIf { it.isNotBlank() && it != "null" } ?: ""

            // BT devices: productName is often empty, get from A2DP proxy
            if (name.isBlank() && isBluetooth(hp.type)) {
                name = getConnectedA2dpDeviceName() ?: getBondedBtName() ?: "Bluetooth Device"
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

            SonaraLogger.bt( "Detected: $name (${type.name})")
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

    private fun isBluetooth(type: Int): Boolean = type in listOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET
    )

    /**
     * Get ACTUALLY CONNECTED A2DP device name (not just paired)
     */
    private fun getConnectedA2dpDeviceName(): String? {
        return try {
            val proxy = a2dpProxy ?: return null
            val connected = proxy.connectedDevices
            val device = connected.firstOrNull()
            device?.name?.also { SonaraLogger.bt( "A2DP connected: $it") }
        } catch (e: SecurityException) {
            SonaraLogger.w("BT", "A2DP permission denied")
            null
        }
    }

    /**
     * Fallback: get first bonded device that appears to be connected
     */
    private fun getBondedBtName(): String? {
        return try {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = btManager?.adapter ?: return null
            // Check if A2DP profile is connected
            if (adapter.getProfileConnectionState(BluetoothProfile.A2DP) != BluetoothProfile.STATE_CONNECTED) return null
            adapter.bondedDevices?.firstOrNull()?.name
        } catch (_: SecurityException) { null }
    }
}
