package lt.loksys.mifbaltupiai;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final LatLng MIF_BALTUPIAI = new LatLng(54.72973774845973,25.26341289281845);

    private GoogleMap mMap;
    private BitmapDescriptor mImage;
    private GroundOverlay mGroundOverlay;
    private File savePath;
    private CheckBox scanCheckbox;
    private TextView statusText;

    private BluetoothAdapter mBluetoothAdapter;
    private BleManager bleManager;
    private MarkerManager markerManager;

    public GoogleMap getMap() {
        return mMap;
    }

    public File getSavePath() {
        return savePath;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_map);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        scanCheckbox = (CheckBox) findViewById(R.id.scan);
        scanCheckbox.setEnabled(false);

        statusText = (TextView) findViewById(R.id.status_text);

        savePath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Loksys");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, true);
        }

        if (savedInstanceState == null) {

            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            } else {

                final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                mBluetoothAdapter = bluetoothManager.getAdapter();

                if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }

                // Is Bluetooth supported on this device?
                if (mBluetoothAdapter != null) {

                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                            != PackageManager.PERMISSION_GRANTED) {

                        PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                                Manifest.permission.BLUETOOTH, true);
                    }

                    this.bleManager = new BleManager(this);
                    scanCheckbox.setEnabled(true);

                }
            }
        }

        this.markerManager = new MarkerManager(this);

        updateStatus();



    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mImage = BitmapDescriptorFactory.fromResource(R.drawable.mif_3a_plotas);

        mGroundOverlay = map.addGroundOverlay(new GroundOverlayOptions()
                .image(mImage)
                .positionFromBounds(new LatLngBounds(
                        new LatLng(54.72973625, 25.26259929),
                        new LatLng(54.73002198, 25.26375304))));

        try {

            InputStream input = this.getBaseContext().getResources().openRawResource(R.raw.graph);
            CSVReader reader = new CSVReader(new InputStreamReader(input));

            for (String[] line: reader.readAll()) {
                map.addMarker(new MarkerOptions()
                    .position(new LatLng(Double.valueOf(line[1]), Double.valueOf(line[2])))
                    .title(line[0])
                    .anchor(0.5f,0.5f)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.black_point)));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        enableMyLocation();
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(MIF_BALTUPIAI, 18));

        if (!savePath.exists()) {
            if (!savePath.mkdirs()) {
                android.util.Log.w("Warning!", "Cannot create directory: " + savePath.toString());
            }
        }

        this.markerManager.initialize();

    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    public void onClearMarkers(View view) {
        this.markerManager.clear();
    }

    public void onScanToggled(View view) {
        this.bleManager.scanLeDevice(scanCheckbox.isChecked());
    }

    public void onSaveMarkers(View view) {
        this.markerManager.save();
    }

    public void updateStatus() {
        long blinks = -1;
        int markers = -1;
        if (this.bleManager != null) {
            blinks = bleManager.getBlinksCount();
        }
        if (this.markerManager != null) {
            markers = markerManager.getSize();
        }
        statusText.setText(String.format("Markers: %d, Blinks: %d", markers, blinks));
    }

}
