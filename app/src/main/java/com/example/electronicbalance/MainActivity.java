package com.example.electronicbalance;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    final static String TAG = "moon";
    final static String SERVICE_UUID = "0000AAA1-0000-1000-8000-00805F9B34FB";
    final static String CHARACTERISTIC_UUID = "0000CCC1-0000-1000-8000-00805F9B34FB";
    final static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    private BluetoothLeScanner mBluetoothLeScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        if (bluetoothManager != null) {
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            mBluetoothLeScanner.startScan(leScanCallback);
        }
    }

    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if ("0_moon".equals(result.getDevice().getName())) {
                Log.i(TAG, result.getDevice().getName());
                result.getDevice().connectGatt(getApplicationContext(), true, gattCallback);
                mBluetoothLeScanner.stopScan(this);
            }
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "new State:" + newState);
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.i(TAG, "service count:" + gatt.getServices().size());
            BluetoothGattService service = gatt.getService(UUID.fromString(SERVICE_UUID));
            if (service != null) {
                Log.i(TAG, "service UUID:" + service.getUuid().toString());
                BluetoothGattCharacteristic data = service.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID));
                BluetoothGattDescriptor descriptor = data.getDescriptor(UUID.fromString(CHARACTERISTIC_UUID));
                if (null != descriptor) {
                    Log.i(TAG, "null != descriptor");
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                } else {
                    Log.i(TAG, "null == descriptor");
                    descriptor = data.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                }
                gatt.setCharacteristicNotification(data, true);
            }
//            for(BluetoothGattService service : gatt.getServices()){
//                if (service.getUuid().equals(UUID.fromString(SERVICE_UUID))) {
//
//                }
//            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.i(TAG, "onCharacteristicChanged:" + characteristic.getUuid().toString());
        }
    };
}
