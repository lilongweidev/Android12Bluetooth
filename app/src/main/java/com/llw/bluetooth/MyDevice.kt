package com.llw.bluetooth

import android.bluetooth.BluetoothDevice

/**
 * 我的设备
 */
data class MyDevice(val device: BluetoothDevice, var rssi: Int)
