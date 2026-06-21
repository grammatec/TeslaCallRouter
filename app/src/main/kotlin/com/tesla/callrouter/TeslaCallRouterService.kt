package com.tesla.callrouter

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log

class TeslaCallRouterService : Service() {
    companion object {
        private const val TAG = "TeslaCallRouterService"
    }

    private lateinit var phoneStateReceiver: PhoneStateReceiver
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): TeslaCallRouterService = this@TeslaCallRouterService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        phoneStateReceiver = PhoneStateReceiver()
        val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(phoneStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(phoneStateReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(phoneStateReceiver)
        Log.d(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "Service bound")
        return binder
    }
}