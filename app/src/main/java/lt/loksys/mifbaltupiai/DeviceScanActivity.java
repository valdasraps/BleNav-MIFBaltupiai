package lt.loksys.mifbaltupiai;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import com.google.android.gms.maps.model.Marker;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DeviceScanActivity extends ListActivity {

    private final BluetoothAdapter bluetoothAdapter;
    private final BLECallBack callBack;
    private boolean mScanning;
    private final Handler handler = new Handler();
    private final Map<String, Anchor> anchors = new HashMap<>();
    private final MapsActivity activity;
    private final File savePath;
    private File saveFile;

    private static final long SCAN_WINDOW_PERIOD = 3000;

    public int getAnchorsCount() {
        return anchors.size();
    }

    public DeviceScanActivity(BluetoothAdapter bluetoothAdapter, MapsActivity activity, File savePath) {
        this.bluetoothAdapter = bluetoothAdapter;
        this.callBack = new BLECallBack();
        this.activity = activity;
        this.savePath = savePath;
    }

    public void scanLeDevice(final boolean enable) {
        if (enable) {

            if (!savePath.exists()) {
                if (!savePath.mkdirs()) {
                    android.util.Log.w("Warning!", "Cannot create directory: " + savePath.toString());
                }
            }

            String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String fileName = "a".concat(timeStamp.concat(".csv"));
            String filePath = savePath + File.separator + fileName;

            saveFile = new File(filePath);
            android.util.Log.i("Will be Saving!", saveFile.toString());

            mScanning = true;
            bluetoothAdapter.startLeScan(callBack);

            runSaver();

        } else {

            mScanning = false;
            bluetoothAdapter.stopLeScan(callBack);

            anchors.clear();

        }

    }

    public class BLECallBack implements BluetoothAdapter.LeScanCallback {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (mScanning && device != null) {
                String name = device.getName();
                if ("iBKS Plus".equals(name)) {
                    String address = device.getAddress();
                    Anchor a = anchors.get(address);
                    if (a == null) {
                        a = new Anchor(address);
                        anchors.put(address, a);
                        activity.updateStatus();
                    }
                    a.addRssi(rssi);
                    Log.i("BLE!", String.format("%s: %s = %d", address, name, rssi));
                }
            }
        }

        public class GATTCallBack extends BluetoothGattCallback {

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    for (BluetoothGattService service : gatt.getServices()) {
                        for (BluetoothGattCharacteristic ch : service.getCharacteristics()) {
                            for (BluetoothGattDescriptor ds : ch.getDescriptors()) {
                                Log.i("BLE!", String.format("%s = %s", ds.getUuid().toString(), new String(ds.getValue())));
                            }
                        }
                    }
                }
            }

        }

    }

    public void runSaver() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                String time = Long.toString(System.currentTimeMillis());

                try {
                    FileWriter writer = new FileWriter(saveFile, true);
                    for (Anchor a: anchors.values()) {
                        writer.write(time);
                        writer.write(",");
                        writer.write(a.address);
                        writer.write(",");
                        writer.write(Long.toString(a.count));
                        writer.write(",");
                        writer.write(Long.toString(a.sum));
                        writer.write("\n");
                    }
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                anchors.clear();
                activity.updateStatus();

                // Run again if still scanning
                if (mScanning) {
                    runSaver();
                }

            }
        }, SCAN_WINDOW_PERIOD);
    }

    private class Anchor {

        private final String address;
        private long count = 0L;
        private long sum = 0L;

        public Anchor(String address) {
            this.address = address;
        }

        public void addRssi(int rssi) {
            this.count += 1;
            this.sum += rssi;
        }

    }

}