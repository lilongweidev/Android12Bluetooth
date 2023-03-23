package com.llw.bluetooth;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.llw.bluetooth.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("MissingPermission")
public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> enableBluetooth;         //打开蓝牙意图
    private ActivityResultLauncher<String> requestBluetoothConnect; //请求蓝牙连接权限意图
    private ActivityResultLauncher<String> requestBluetoothScan;    //请求蓝牙扫描权限意图
    private ActivityResultLauncher<String> requestLocation;         //请求定位权限

    private ActivityMainBinding binding;

    private final String TAG = MainActivity.class.getSimpleName();
    //获取系统蓝牙适配器
    private BluetoothAdapter mBluetoothAdapter;
    //扫描者
    private BluetoothLeScanner scanner;
    //是否正在扫描
    boolean isScanning = false;

    //设备列表
    private final List<MyDevice> deviceList = new ArrayList<>();
    //适配器
    private MyDeviceAdapter myDeviceAdapter;

    private void registerIntent() {
        //打开蓝牙意图
        enableBluetooth = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                if (isOpenBluetooth()){
                    BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
                    mBluetoothAdapter = manager.getAdapter();
                    scanner = mBluetoothAdapter.getBluetoothLeScanner();
                    showMsg("蓝牙已打开");
                } else {
                    showMsg("蓝牙未打开");
                }
            }
        });
        //请求BLUETOOTH_CONNECT权限意图
        requestBluetoothConnect = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            if (result) {
                enableBluetooth.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            } else {
                showMsg("Android12中未获取此权限，无法打开蓝牙。");
            }
        });
        //请求BLUETOOTH_SCAN权限意图
        requestBluetoothScan = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            if (result) {
                //进行扫描
                startScan();
            } else {
                showMsg("Android12中未获取此权限，则无法扫描蓝牙。");
            }
        });
        //请求定位权限
        requestLocation = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            if (result) {
                //扫描蓝牙
                startScan();
            } else {
                showMsg("Android12以下，6及以上需要定位权限才能扫描设备");
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        registerIntent();
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initView();
    }

    //扫描结果回调
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addDeviceList(new MyDevice(result.getDevice(),result.getRssi()));
        }
    };

    private void initView() {
        if (isOpenBluetooth()) {
            BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            mBluetoothAdapter = manager.getAdapter();
            scanner = mBluetoothAdapter.getBluetoothLeScanner();
        }
        //打开蓝牙按钮点击事件
        binding.btnOpenBluetooth.setOnClickListener(v -> {
            //蓝牙是否已打开
            if (isOpenBluetooth()) {
                showMsg("蓝牙已打开");
                return;
            }
            //是Android12
            if (isAndroid12()) {
                //检查是否有BLUETOOTH_CONNECT权限
                if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                    //打开蓝牙
                    enableBluetooth.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                } else {
                    //请求权限
                    requestBluetoothConnect.launch(Manifest.permission.BLUETOOTH_CONNECT);
                }
                return;
            }
            //不是Android12 直接打开蓝牙
            enableBluetooth.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
        });
        //扫描蓝牙按钮点击事件
        binding.btnScanBluetooth.setOnClickListener(v -> {
            if (isAndroid12()) {
                if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                    requestBluetoothConnect.launch(Manifest.permission.BLUETOOTH_CONNECT);
                    return;
                }
                if (hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                    //扫描或者停止扫描
                    if (isScanning) stopScan();
                    else startScan();
                } else {
                    //请求权限
                    requestBluetoothScan.launch(Manifest.permission.BLUETOOTH_SCAN);
                }
            } else {
                if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    //扫描或者停止扫描
                    if (isScanning) stopScan();
                    else startScan();
                } else {
                    requestLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            }
        });
        //列表初始化
        myDeviceAdapter = new MyDeviceAdapter(deviceList);
        binding.rvDevice.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDevice.setAdapter(myDeviceAdapter);
    }

    private void startScan() {
        if (!isScanning) {
            scanner.startScan(scanCallback);
            isScanning = true;
            binding.btnScanBluetooth.setText("停止扫描");
        }
    }

    private void stopScan() {
        if (isScanning) {
            scanner.stopScan(scanCallback);
            isScanning = false;
            binding.btnScanBluetooth.setText("扫描蓝牙");
        }
    }

    private int findDeviceIndex(MyDevice scanDevice, List<MyDevice> deviceList) {
        int index = 0;
        for (MyDevice myDevice : deviceList) {
            if (myDevice.getDevice().getAddress().equals(scanDevice.getDevice().getAddress())) return index;
            index += 1;
        }
        return -1;
    }

    private void addDeviceList(MyDevice device) {
        int index = findDeviceIndex(device, deviceList);
        if (index == -1) {
            deviceList.add(device);
            myDeviceAdapter.notifyDataSetChanged();
        } else {
            deviceList.get(index).setRssi(device.getRssi());
            myDeviceAdapter.notifyItemChanged(index);
        }
    }

    private boolean isOpenBluetooth() {
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();
        if (adapter == null) {
            return false;
        }
        return adapter.isEnabled();
    }

    private boolean isAndroid12() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    private boolean hasPermission(String permission) {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void showMsg(CharSequence msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}