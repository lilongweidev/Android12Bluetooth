package com.llw.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.llw.bluetooth.databinding.ActivityMainBinding

@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity() {

    //请求定位权限意图
    private val requestLocation =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                //扫描蓝牙
                startScan()
            } else {
                showMsg("Android12以下，6及以上需要定位权限才能扫描设备")
            }
        }

    //请求BLUETOOTH_CONNECT权限意图
    private val requestBluetoothConnect =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                //打开蓝牙
                enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            } else {
                showMsg("Android12中未获取此权限，则无法打开蓝牙。")
            }
        }

    //请求BLUETOOTH_SCAN权限意图
    private val requestBluetoothScan =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                //进行扫描
                startScan()
            } else {
                showMsg("Android12中未获取此权限，则无法扫描蓝牙。")
            }
        }

    //打开蓝牙意图
    private val enableBluetooth = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            if (isOpenBluetooth()) {
                mBluetoothAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
                scanner = mBluetoothAdapter.bluetoothLeScanner
                showMsg("蓝牙已打开")
            } else {
                showMsg("蓝牙未打开")
            }
        }
    }

    private val TAG = MainActivity::class.java.simpleName

    //获取系统蓝牙适配器
    private lateinit var mBluetoothAdapter: BluetoothAdapter

    //扫描者
    private lateinit var scanner: BluetoothLeScanner

    //是否正在扫描
    var isScanning = false

    //设备列表
    private val deviceList = mutableListOf<MyDevice>()
    //适配器
    private lateinit var myDeviceAdapter: MyDeviceAdapter

    //扫描结果回调
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            addDeviceList(MyDevice(result.device,result.rssi))
        }
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (isOpenBluetooth()) {
            mBluetoothAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
            scanner = mBluetoothAdapter.bluetoothLeScanner
        }

        initView()
    }

    /**
     * 添加到设备列表中
     */
    private fun addDeviceList(device: MyDevice) {
        val index = findDeviceIndex(device, deviceList)
        if (index == -1) {
            Log.d(TAG, "name: ${device.device.name}, address: ${device.device.address}")
            deviceList.add(device)
            myDeviceAdapter.notifyDataSetChanged()
        } else {
            deviceList[index].rssi = device.rssi
            myDeviceAdapter.notifyItemChanged(index)
        }
    }

    /**
     * 在扫描结果列表中查找现有设备的索引。
     *
     * @param  scanDevice 扫描到的设备
     * @return 如果未找到，则为 return -1 的索引 则是没有添加过
     */
    private fun findDeviceIndex(scanDevice: MyDevice, deviceList: List<MyDevice>): Int {
        var index = 0
        for (device in deviceList) {
            if (scanDevice.device.address.equals(device.device.address)) return index
            index += 1
        }
        return -1
    }

    private fun initView() {
        //打开蓝牙
        binding.btnOpenBluetooth.setOnClickListener {
            //蓝牙是否已打开
            if (isOpenBluetooth()) {
                showMsg("蓝牙已打开")
                return@setOnClickListener
            }
            //是Android12
            if (isAndroid12()) {
                //检查是否有BLUETOOTH_CONNECT权限
                if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                    //打开蓝牙
                    enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                } else {
                    //请求权限
                    requestBluetoothConnect.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
                return@setOnClickListener
            }
            //不是Android12 直接打开蓝牙
            enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }

        //扫描蓝牙
        binding.btnScanBluetooth.setOnClickListener {
            if (isAndroid12()) {
                if (hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                    //扫描或者停止扫描
                    if (isScanning) stopScan() else startScan()
                } else {
                    //请求权限
                    requestBluetoothScan.launch(Manifest.permission.BLUETOOTH_SCAN)
                }
            } else {
                if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    //开始扫描
                    if (isScanning) stopScan() else startScan()
                } else {
                    requestLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }

        //初始化列表
        myDeviceAdapter = MyDeviceAdapter(deviceList)
        binding.rvDevice.layoutManager = LinearLayoutManager(this)
        binding.rvDevice.adapter = myDeviceAdapter
    }

    private fun startScan() {
        if (!isScanning) {
            scanner.startScan(scanCallback)
            isScanning = true
            binding.btnScanBluetooth.text = "停止扫描"
        }
    }

    private fun stopScan() {
        if (isScanning) {
            scanner.stopScan(scanCallback)
            isScanning = false
            binding.btnScanBluetooth.text = "扫描蓝牙"
        }
    }

    private fun isOpenBluetooth(): Boolean {
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter ?: return false
        return adapter.isEnabled
    }

    private fun isAndroid12() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    private fun hasPermission(permission: String) =
        checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun showMsg(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

