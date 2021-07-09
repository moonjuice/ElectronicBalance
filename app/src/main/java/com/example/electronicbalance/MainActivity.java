package com.example.electronicbalance;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    final static String TAG = "moon";
    final static String SERVICE_UUID = "0000AAA1-0000-1000-8000-00805F9B34FB";
    final static String CHARACTERISTIC_UUID = "0000CCC1-0000-1000-8000-00805F9B34FB";
    final static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    private final Handler mainHandler = new MainHandler() {};
    private TextView mWeightView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWeightView = findViewById(R.id.weightView);
        mainHandler.obtainMessage(MSG_START_SCAN).sendToTarget();
    }

    private final static int MSG_START_SCAN = 0;
    private final static int MSG_STOP_SCAN = 1;
    private final static int MSG_UPDATE_WEIGHT = 2;
    public class MainHandler extends Handler {
        private BluetoothLeScanner mBluetoothLeScanner;
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_START_SCAN:
                    final BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
                    if (bluetoothManager != null) {
                        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
                        mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                        mBluetoothLeScanner.startScan(leScanCallback);
                    }
                    break;
                case MSG_STOP_SCAN:
                    mBluetoothLeScanner.stopScan(leScanCallback);
                    break;
                case MSG_UPDATE_WEIGHT:
                    mWeightView.setText(String.format(getResources().getString(R.string.format_str), msg.obj));
                    break;
            }
        }
    }

    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if ("0_moon".equals(result.getDevice().getName())) {
                Log.i(TAG, result.getDevice().getName());
                result.getDevice().connectGatt(getApplicationContext(), true, gattCallback);
                mainHandler.obtainMessage(MSG_STOP_SCAN).sendToTarget();
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
            BluetoothGattService service = gatt.getService(UUID.fromString(SERVICE_UUID));
            if (service != null) {
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
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Message msg = new Message();
            msg.what = MSG_UPDATE_WEIGHT;
            msg.obj = new String(characteristic.getValue(), StandardCharsets.UTF_8);
            mainHandler.sendMessage(msg);
        }
    };
}