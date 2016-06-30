/*Copyright 2016 WorkStress Experiment
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package uk.ac.mdx.cs.ie.acontextlib;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Abstract class for Bluetooth LE Devices
 *
 * @author Dean Kramer <d.kramer@mdx.ac.uk>
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class BluetoothLEDevice extends ContextObserver {

    private static final long SCAN_PERIOD = 10000;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private Handler mHandler = new Handler();
    private boolean mScanning;
    private ArrayList<UUID> mInterestedServices = new ArrayList<>();
    private ArrayList<UUID> mInterestedMeasurements = new ArrayList<>();
    private String mDeviceID;
    private UUID mPhoneID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public BluetoothLEDevice(Context c, ContextReceiver cr) {

        super(c, cr);
        final BluetoothManager bluetoothManager =
                (BluetoothManager) c.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    public BluetoothLEDevice(Context c, ContextReceiver cr, UUID service,
                             UUID measurement) {

        super(c, cr);
        final BluetoothManager bluetoothManager =
                (BluetoothManager) c.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        mInterestedServices.add(service);
        mInterestedMeasurements.add(measurement);

    }

    public BluetoothLEDevice(Context c, ContextReceiver cr, ArrayList<UUID> services,
                             ArrayList<UUID> measurements) {

        super(c, cr);
        final BluetoothManager bluetoothManager =
                (BluetoothManager) c.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        mInterestedServices = services;
        mInterestedMeasurements = measurements;

    }


    @Override
    public boolean setContextParameters(HashMap<String, Object> parameters) {
        if (super.setContextParameters(parameters)) {
            return true;
        } else {
            return false;
        }
    }

    private void scanForLeDevice(final boolean enable) {
        if (enable) {

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);

                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    @Override
    public boolean start() {
        if (!mBluetoothAdapter.isEnabled()) {
            return false;
        }

        scanForLeDevice(true);

        return true;
    }

    @Override
    public boolean pause() {

        if (mBluetoothGatt == null) {
            return false;
        }

        mBluetoothGatt.close();
        mBluetoothGatt = null;

        return true;
    }

    @Override
    public boolean resume() {

        if (!mBluetoothAdapter.isEnabled()) {
            return false;
        }

        scanForLeDevice(true);

        return true;
    }

    @Override
    public boolean stop() {

        if (mBluetoothGatt == null) {
            return false;
        }

        mBluetoothGatt.close();
        mBluetoothGatt = null;

        return true;
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    if (device.getAddress() == mDeviceID) {
                        mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
                    }
                }
            };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (status == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {

                for (UUID interestedService : mInterestedServices) {
                    BluetoothGattService service = gatt.getService(interestedService);

                    if (service != null) {

                        for (UUID interestedMeasurement : mInterestedMeasurements) {
                            BluetoothGattCharacteristic characteristic = service.getCharacteristic(interestedMeasurement);

                            if (characteristic != null) {

                                gatt.setCharacteristicNotification(characteristic, true);
                                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(mPhoneID);
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(descriptor);
                            }
                        }

                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            checkContext(characteristic);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

        }
    };

    public abstract void checkContext(BluetoothGattCharacteristic characteristic);
}
