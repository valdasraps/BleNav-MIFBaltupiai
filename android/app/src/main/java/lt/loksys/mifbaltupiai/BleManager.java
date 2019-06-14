package lt.loksys.mifbaltupiai;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseData;
import android.os.Handler;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class BleManager extends ListActivity {

    private final ExecutorService exec = Executors.newSingleThreadExecutor();

    private Future<?> saver = null;
    private final BLECallBack callBack = new BLECallBack();
    private final BlockingQueue<Anchor> queue = new LinkedBlockingQueue<>();
    private final MainActivity activity;
    private long blinks = 0L;
    private final Handler scanHandler = new Handler();

    private static final long SCAN_WINDOW_PERIOD = 3000;

    public long getBlinksCount() {
        return blinks;
    }

    public BleManager(MainActivity activity) {
        this.activity = activity;
    }

    public void scanLeDevice(final boolean enable) {
        if (enable && saver == null) {

            this.blinks = 0L;
            this.saver = exec.submit(new Saver());
            activity.getBluetoothAdapter().startLeScan(callBack);

        } else if (!enable && saver != null) {

            activity.getBluetoothAdapter().stopLeScan(callBack);
            this.saver.cancel(true);
            this.saver = null;

        }

    }

    public void close() throws Exception {
        scanLeDevice(false);
    }

    public class BLECallBack implements BluetoothAdapter.LeScanCallback {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            if (saver != null && device != null) {
                final String name = device.getName();
                if ("loksys.lt".equals(name)) {
                    scanHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            final long time = System.currentTimeMillis();
                            AdvertiseData data = new AdvertiseData.Builder()
                                    .addServiceUuid(ParcelUuid
                                            .fromString(UUID.nameUUIDFromBytes(scanRecord).toString())).build();
                            String uuid = getUUID(data);
                            queue.add(new Anchor(time, device.getAddress(), uuid, rssi));
                            //Log.i("onLeScan", String.format("Adv from %s (type: %d, rssi: %d) = %s", name, device.getType(), rssi, uuid));
                            blinks += 1;

                        }
                    });

                }
            }
        }

    }

    private class Saver implements Runnable {

        private final File saveFile;

        public Saver() {
            String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String fileName = "a".concat(timeStamp.concat(".csv"));
            String filePath = activity.getSavePath() + File.separator + fileName;
            this.saveFile = new File(filePath);
        }

        @Override
        public void run() {

            FileWriter writer = null;
            try {

                writer = new FileWriter(saveFile, true);

                while (true) {
                    Anchor a = queue.take();
                    writer.write(a.toString());
                    writer.write("\n");
                    blinks += 1;
                    activity.updateStatus();
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class Anchor {

        private final String data[];

        public Anchor(long time, String address, String uuid, int rssi) {
            data = new String[] {
                    Long.toString(time),
                    address,
                    uuid,
                    Integer.toString(rssi)
            };
        }

        @Override
        public String toString() {
            return TextUtils.join(",", data);
        }

    }

    public String getUUID(AdvertiseData data){
        List<ParcelUuid> UUIDs = data.getServiceUuids();
        //ToastMakers.message(scannerActivity.getApplicationContext(), UUIDs.toString());
        String UUIDx = UUIDs.get(0).getUuid().toString();
        //Log.e("UUID", " as list ->" + UUIDx);
        return UUIDx;
    }

}