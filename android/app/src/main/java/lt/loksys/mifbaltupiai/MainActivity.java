package lt.loksys.mifbaltupiai;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
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
import java.io.File;

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
        mImage = BitmapDescriptorFactory.fromResource(R.drawable.mif325_3a);

        mGroundOverlay = map.addGroundOverlay(new GroundOverlayOptions()
                .image(mImage).anchor(0, 0)
                .position(new LatLng(54.730021177604215,25.262626917658963), 82f, 40f));

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
        int anchors = -1, markers = -1;
        if (this.bleManager != null) {
            anchors = bleManager.getAnchorsCount();
        }
        if (this.markerManager != null) {
            markers = markerManager.getSize();
        }
        statusText.setText(String.format("Markers: %d, Anchors: %d", markers, anchors));
    }

}
