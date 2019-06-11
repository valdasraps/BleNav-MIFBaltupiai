package lt.loksys.mifbaltupiai;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (saver != null && device != null) {
                String name = device.getName();
                //if ("iBKS Plus".equals(name)) {
                    queue.add(new Anchor(device.getAddress(), rssi));
                //}
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

        public Anchor(String address, int rssi) {
            data = new String[] {
                    Long.toString(System.currentTimeMillis()),
                    address,
                    Integer.toString(rssi)
            };
        }

        @Override
        public String toString() {
            return TextUtils.join(",", data);
        }

    }

}