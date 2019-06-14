package lt.loksys.mifbaltupiai;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class BleManagerScanner extends ListActivity {

    private final String DEVICE_NAME = "loksys.lt";
    private static final long SCAN_REPORT_PERIOD = 500L;
    private static final long SCAN_WINDOW_PERIOD = 3000L;

    private final ExecutorService exec = Executors.newSingleThreadExecutor();

    private Future<?> saver = null;
    private BluetoothLeScanner scanner = null;
    private final ScannerCallBack callBack = new ScannerCallBack();
    private final List<ScanFilter> scanFilters = Collections.singletonList(new ScanFilter.Builder().setDeviceName(DEVICE_NAME).build());
    private final ScanSettings scanSettings = new ScanSettings.Builder().setReportDelay(SCAN_REPORT_PERIOD).build();
    private final BlockingQueue<Anchor> queue = new LinkedBlockingQueue<>();
    private final MainActivity activity;
    private long blinks = 0L;

    public long getBlinksCount() {
        return blinks;
    }

    public BleManagerScanner(MainActivity activity) {
        this.activity = activity;
    }

    public void scanLeDevice(final boolean enable) {

        if (scanner == null) {
            this.scanner = activity.getBluetoothAdapter().getBluetoothLeScanner();
        }

        if (enable && saver == null) {

            this.blinks = 0L;
            this.saver = exec.submit(new Saver());
            this.scanner.startScan(scanFilters, scanSettings, callBack);

        } else if (!enable && saver != null) {

            this.scanner.stopScan(callBack);
            this.saver.cancel(true);
            this.saver = null;

        }

    }

    public void close() throws Exception {
        scanLeDevice(false);
    }

    public class ScannerCallBack extends ScanCallback {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.i("ScannerCallBack", String.format("onBatchScanResults: %d", results.size()));
        }

        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            if (saver != null && result != null) {

                final BluetoothDevice device = result.getDevice();
                final ScanRecord record = result.getScanRecord();

                Log.i("ScannerCallBack", String.format("onScanResult: from %s (callBackType: %d, rssi: %d)", record.getDeviceName(), callbackType, result.getRssi()));

                queue.add(new Anchor(device.getAddress(), result.getRssi()));
                blinks += 1;
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

    /*
    BLE Scan record type IDs
    data from:
    https://www.bluetooth.org/en-us/specification/assigned-numbers/generic-access-profile
    */
    static final int EBLE_FLAGS           = 0x01;//«Flags»	Bluetooth Core Specification:
    static final int EBLE_16BitUUIDInc    = 0x02;//«Incomplete List of 16-bit Service Class UUIDs»	Bluetooth Core Specification:
    static final int EBLE_16BitUUIDCom    = 0x03;//«Complete List of 16-bit Service Class UUIDs»	Bluetooth Core Specification:
    static final int EBLE_32BitUUIDInc    = 0x04;//«Incomplete List of 32-bit Service Class UUIDs»	Bluetooth Core Specification:
    static final int EBLE_32BitUUIDCom    = 0x05;//«Complete List of 32-bit Service Class UUIDs»	Bluetooth Core Specification:
    static final int EBLE_128BitUUIDInc   = 0x06;//«Incomplete List of 128-bit Service Class UUIDs»	Bluetooth Core Specification:
    static final int EBLE_128BitUUIDCom   = 0x07;//«Complete List of 128-bit Service Class UUIDs»	Bluetooth Core Specification:
    static final int EBLE_SHORTNAME       = 0x08;//«Shortened Local Name»	Bluetooth Core Specification:
    static final int EBLE_LOCALNAME       = 0x09;//«Complete Local Name»	Bluetooth Core Specification:
    static final int EBLE_TXPOWERLEVEL    = 0x0A;//«Tx Power Level»	Bluetooth Core Specification:
    static final int EBLE_DEVICECLASS     = 0x0D;//«Class of Device»	Bluetooth Core Specification:
    static final int EBLE_SIMPLEPAIRHASH  = 0x0E;//«Simple Pairing Hash C»	Bluetooth Core Specification:​«Simple Pairing Hash C-192»	​Core Specification Supplement, Part A, section 1.6
    static final int EBLE_SIMPLEPAIRRAND  = 0x0F;//«Simple Pairing Randomizer R»	Bluetooth Core Specification:​«Simple Pairing Randomizer R-192»	​Core Specification Supplement, Part A, section 1.6
    static final int EBLE_DEVICEID        = 0x10;//«Device ID»	Device ID Profile v1.3 or later,«Security Manager TK Value»	Bluetooth Core Specification:
    static final int EBLE_SECURITYMANAGER = 0x11;//«Security Manager Out of Band Flags»	Bluetooth Core Specification:
    static final int EBLE_SLAVEINTERVALRA = 0x12;//«Slave Connection Interval Range»	Bluetooth Core Specification:
    static final int EBLE_16BitSSUUID     = 0x14;//«List of 16-bit Service Solicitation UUIDs»	Bluetooth Core Specification:
    static final int EBLE_128BitSSUUID    = 0x15;//«List of 128-bit Service Solicitation UUIDs»	Bluetooth Core Specification:
    static final int EBLE_SERVICEDATA     = 0x16;//«Service Data»	Bluetooth Core Specification:​«Service Data - 16-bit UUID»	​Core Specification Supplement, Part A, section 1.11
    static final int EBLE_PTADDRESS       = 0x17;//«Public Target Address»	Bluetooth Core Specification:
    static final int EBLE_RTADDRESS       = 0x18;;//«Random Target Address»	Bluetooth Core Specification:
    static final int EBLE_APPEARANCE      = 0x19;//«Appearance»	Bluetooth Core Specification:
    static final int EBLE_DEVADDRESS      = 0x1B;//«​LE Bluetooth Device Address»	​Core Specification Supplement, Part A, section 1.16
    static final int EBLE_LEROLE          = 0x1C;//«​LE Role»	​Core Specification Supplement, Part A, section 1.17
    static final int EBLE_PAIRINGHASH     = 0x1D;//«​Simple Pairing Hash C-256»	​Core Specification Supplement, Part A, section 1.6
    static final int EBLE_PAIRINGRAND     = 0x1E;//«​Simple Pairing Randomizer R-256»	​Core Specification Supplement, Part A, section 1.6
    static final int EBLE_32BitSSUUID     = 0x1F;//​«List of 32-bit Service Solicitation UUIDs»	​Core Specification Supplement, Part A, section 1.10
    static final int EBLE_32BitSERDATA    = 0x20;//​«Service Data - 32-bit UUID»	​Core Specification Supplement, Part A, section 1.11
    static final int EBLE_128BitSERDATA   = 0x21;//​«Service Data - 128-bit UUID»	​Core Specification Supplement, Part A, section 1.11
    static final int EBLE_SECCONCONF      = 0x22;//​«​LE Secure Connections Confirmation Value»	​Core Specification Supplement Part A, Section 1.6
    static final int EBLE_SECCONRAND      = 0x23;//​​«​LE Secure Connections Random Value»	​Core Specification Supplement Part A, Section 1.6​
    static final int EBLE_3DINFDATA       = 0x3D;//​​«3D Information Data»	​3D Synchronization Profile, v1.0 or later
    static final int EBLE_MANDATA         = 0xFF;//«Manufacturer Specific Data»	Bluetooth Core Specification:

    /*
    BLE Scan record parsing
    inspired by:
    http://stackoverflow.com/questions/22016224/ble-obtain-uuid-encoded-in-advertising-packet
     */
    static public Map<Integer,String> ParseRecord(byte[] scanRecord){
        Map <Integer,String> ret = new HashMap<>();
        int index = 0;
        while (index < scanRecord.length) {
            int length = scanRecord[index++];
            //Zero value indicates that we are done with the record now
            if (length == 0) break;

            int type = scanRecord[index];
            //if the type is zero, then we are pass the significant section of the data,
            // and we are thud done
            if (type == 0) break;

            byte[] data = Arrays.copyOfRange(scanRecord, index + 1, index + length);
            if(data != null && data.length > 0) {
                StringBuilder hex = new StringBuilder(data.length * 2);
                // the data appears to be there backwards
                for (int bb = data.length- 1; bb >= 0; bb--){
                    hex.append(String.format("%02X", data[bb]));
                }
                ret.put(type,hex.toString());
            }
            index += length;
        }

        return ret;
    }

}