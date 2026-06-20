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
        private const val TESLA_MAC = "04:4E:AF:F6:B6:DE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        
        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING,
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                Log.d(TAG, "Call detected, routing to Tesla")
                routeCallToTesla(context)
            }
        }
    }

    private fun routeCallToTesla(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                val bondedDevices = bluetoothAdapter.bondedDevices
                
                for (device in bondedDevices) {
                    if (device.address.uppercase() == TESLA_MAC.uppercase()) {
                        Log.d(TAG, "Found Tesla device: ${device.name} (${device.address})")
                        
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val audioDevices = audioManager.getAvailableCommunicationDevices()
                                for (audioDevice in audioDevices) {
                                    if (audioDevice.address == device.address) {
                                        audioManager.setCommunicationDevice(audioDevice)
                                        Log.d(TAG, "Successfully routed to Tesla")
                                        return
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error setting communication device: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in routeCallToTesla: ${e.message}")
        }
    }
}
