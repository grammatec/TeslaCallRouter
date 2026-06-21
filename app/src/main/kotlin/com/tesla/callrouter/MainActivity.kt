package com.tesla.callrouter

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val PERMISSION_REQUEST_CODE = 100
    private lateinit var statusDevice: TextView
    private lateinit var statusService: TextView
    private lateinit var deviceSpinner: Spinner
    private lateinit var toggleSwitch: Switch
    private lateinit var prefs: SharedPreferences
    private val deviceList = mutableListOf<Pair<String, String>>()
    private var isServiceRunning = false
    private var serviceBound = false
    private var routerService: TeslaCallRouterService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TeslaCallRouterService.LocalBinder
            routerService = binder.getService()
            serviceBound = true
            updateStatus()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            routerService = null
            serviceBound = false
            updateStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("TeslaCallRouter", Context.MODE_PRIVATE)

        statusDevice = findViewById(R.id.statusDevice)
        statusService = findViewById(R.id.statusService)
        deviceSpinner = findViewById(R.id.deviceSpinner)
        toggleSwitch = findViewById(R.id.toggleSwitch)

        requestPermissions()
        loadBluetoothDevices()
        setupUI()
    }

    private fun loadBluetoothDevices() {
        deviceList.clear()
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                for (device in bluetoothAdapter.bondedDevices) {
                    deviceList.add(Pair("${device.name} (${device.address})", device.address))
                }
            }
        } catch (e: Exception) {
            statusDevice.text = "Error loading devices: ${e.message}"
        }

        if (deviceList.isEmpty()) {
            deviceList.add(Pair("No Bluetooth devices found", ""))
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deviceList.map { it.first })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deviceSpinner.adapter = adapter

        val savedMac = prefs.getString("selected_device_mac", "")
        val index = deviceList.indexOfFirst { it.second == savedMac }
        if (index >= 0) {
            deviceSpinner.setSelection(index)
        }
    }

    private fun setupUI() {
        isServiceRunning = prefs.getBoolean("service_enabled", true)
        toggleSwitch.isChecked = isServiceRunning

        toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("service_enabled", isChecked).apply()
            isServiceRunning = isChecked
            
            if (isChecked) {
                startService(Intent(this, TeslaCallRouterService::class.java))
                bindService(Intent(this, TeslaCallRouterService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
            } else {
                unbindService(serviceConnection)
                stopService(Intent(this, TeslaCallRouterService::class.java))
            }
            updateStatus()
        }

        deviceSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position < deviceList.size) {
                    val selectedMac = deviceList[position].second
                    prefs.edit().putString("selected_device_mac", selectedMac).apply()
                    updateStatus()
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        if (isServiceRunning) {
            bindService(Intent(this, TeslaCallRouterService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun updateStatus() {
        val selectedMac = prefs.getString("selected_device_mac", "")
        val selectedName = deviceList.find { it.second == selectedMac }?.first ?: "None"
        val serviceStatus = if (isServiceRunning) "Running" else "Stopped"

        statusDevice.text = "Active Device: $selectedName"
        statusService.text = "Service: $serviceStatus"
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.READ_PHONE_STATE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions, PERMISSION_REQUEST_CODE)
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
        }
    }
}