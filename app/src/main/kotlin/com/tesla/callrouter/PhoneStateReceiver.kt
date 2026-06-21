package com.tesla.callrouter

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log

class PhoneStateReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "TeslaCallRouter"
        private var callActive = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }

        val prefs = context.getSharedPreferences("TeslaCallRouter", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("service_enabled", true)
        
        if (!isEnabled) {
            Log.d(TAG, "Service disabled, ignoring call")
            return
        }

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val selectedDeviceMac = prefs.getString("selected_device_mac", "")

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                if (!callActive && selectedDeviceMac.isNotEmpty()) {
                    Log.d(TAG, "Incoming call detected, setting device: $selectedDeviceMac")
                    routeCallToDevice(context, selectedDeviceMac)
                }
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (!callActive && selectedDeviceMac.isNotEmpty()) {
                    Log.d(TAG, "Call active (outgoing), holding device: $selectedDeviceMac")
                    callActive = true
                    routeCallToDevice(context, selectedDeviceMac)
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (callActive) {
                    Log.d(TAG, "Call ended, releasing device")
                    callActive = false
                    releaseDevice(context)
                }
            }
        }
    }

    private fun routeCallToDevice(context: Context, deviceMac: String) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                val bondedDevices = bluetoothAdapter.bondedDevices

                for (device in bondedDevices) {
                    if (device.address.uppercase() == deviceMac.uppercase()) {
                        Log.d(TAG, "Found device: ${device.name} (${device.address})")

                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val audioDevices = audioManager.getAvailableCommunicationDevices()
                                for (audioDevice in audioDevices) {
                                    if (audioDevice.address == device.address) {
                                        audioManager.setCommunicationDevice(audioDevice)
                                        Log.d(TAG, "Successfully routed to device")
                                        return
                                    }
                                }
                                Log.w(TAG, "Device not available in communication devices")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error setting communication device: ${e.message}")
                        }
                        return
                    }
                }
                Log.w(TAG, "Device not found in bonded devices")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in routeCallToDevice: ${e.message}")
        }
    }

    private fun releaseDevice(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
                Log.d(TAG, "Cleared communication device hold")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing device: ${e.message}")
        }
    }
}