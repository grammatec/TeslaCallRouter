# Tesla Call Router

Automatically route incoming phone calls to your Tesla Bluetooth device instead of your car's Android Auto system.

## Overview

This Android app solves the problem where multiple Bluetooth devices (car + Android Auto) compete for call audio. It ensures calls always route to your Tesla Model 3 FNB device.

## Features

- ✅ Automatic call detection and routing to Tesla
- ✅ Keeps CAR-80D6 connected (doesn't disconnect it)
- ✅ Routes call audio only to Tesla device
- ✅ Runs silently in background
- ✅ Works on Android 12+ (tested on Android 14)

## Device Support

- **Tesla Device**: Tesla Model 3 FNB (04:4E:AF:F6:B6:DE)
- **Car Device**: CAR-80D6 (kept connected for music/media)

## Installation

1. Download the APK from GitHub Actions artifacts
2. On your phone: **Settings → Apps & Notifications → App Permissions → Allow install from unknown sources**
3. Tap the APK to install
4. Grant all requested permissions

## How It Works

1. Monitors incoming phone calls
2. Detects when call state changes (ringing or active)
3. Finds Tesla Model 3 FNB in bonded devices
4. Routes call audio to Tesla using Android's `AudioManager.setCommunicationDevice()`
5. CAR-80D6 remains connected for music/navigation

## Requirements

- Android 12 or higher (API 31+)
- Bluetooth devices paired: Tesla Model 3 FNB + CAR-80D6
