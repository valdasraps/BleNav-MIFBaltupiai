package lt.loksys.mifbaltupiai;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MapsActivity extends AppCompatActivity
        implements
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final LatLng MIFBALTUPIAI = new LatLng(54.72973774845973,25.26341289281845);
    private static final int MARKER_REMOVE_PERIOD = 3000;

    private GoogleMap mMap;
    private BitmapDescriptor mImage;
    private GroundOverlay mGroundOverlay;
    private File savePath;
    private Handler markerRemoveHandler = new Handler();
    private CheckBox scanCheckbox;
    private TextView statusText;

    private BluetoothAdapter mBluetoothAdapter;
    private DeviceScanActivity deviceScanActivity;

    private final List<Marker> markers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.button_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        scanCheckbox = (CheckBox) findViewById(R.id.scan);
        scanCheckbox.setEnabled(false);

        statusText = (TextView) findViewById(R.id.status_text);

        savePath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Loksys");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
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
                        // Permission to access the location is missing.
                        PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                                Manifest.permission.BLUETOOTH, true);
                    }

                    //                // Is Bluetooth turned on?
                    //                if (mBluetoothAdapter.isEnabled()) {
                    //
                    //                    // Are Bluetooth Advertisements supported on this device?
                    //                    if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                    //
                    //                        // Everything is supported and enabled, load the fragments.
                    //                        setupFragments();
                    //
                    //                    } else {
                    //
                    //                        // Bluetooth Advertisements are not supported.
                    //                        showErrorText(R.string.bt_ads_not_supported);
                    //                    }
                    //
                    //                } else {
                    //
                    //                    // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
                    //                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    //                    startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
                    //                }

                    this.deviceScanActivity = new DeviceScanActivity(this.mBluetoothAdapter, this, savePath);

                    scanCheckbox.setEnabled(true);

                }
            }
        }

        updateStatus();

    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mImage = BitmapDescriptorFactory.fromResource(R.drawable.mif325_3a);

        mGroundOverlay = map.addGroundOverlay(new GroundOverlayOptions()
                .image(mImage).anchor(0, 0)
                .position(new LatLng(54.730021177604215,25.262626917658963), 82f, 40f));
                //.bearing(30));

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener()
        {
            @Override
            public void onMapClick(LatLng coord)
            {
                android.util.Log.i("Clicked!", coord.toString());
                final Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(coord)
                        .title("My Spot")
                        .snippet("This is my spot!")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                markers.add(marker);
                markerRemoveHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        marker.remove();
                    }
                }, MARKER_REMOVE_PERIOD);
                updateStatus();
            }

        });

        enableMyLocation();
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(MIFBALTUPIAI, 18));

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
        for (Marker m: markers) {
            m.remove();
        }
        markers.clear();
        updateStatus();
    }

    public void onScanToggled(View view) {
        this.deviceScanActivity.scanLeDevice(scanCheckbox.isChecked());
    }

    public void onSaveMarkers(View view) {

        if (!savePath.exists()) {
            if (!savePath.mkdirs()) {
                android.util.Log.w("Warning!", "Cannot create directory: " + savePath.toString());
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String fileName = "m".concat(timeStamp.concat(".csv"));
        String filePath = savePath + File.separator + fileName;

        File f = new File(filePath);
        android.util.Log.i("Saving!", f.toString());

        String time = Long.toString(System.currentTimeMillis());

        try {
            FileWriter writer = new FileWriter(f);
            for (Marker m: markers) {
                writer.write(time);
                writer.write(",");
                writer.write(Double.toString(m.getPosition().latitude));
                writer.write(",");
                writer.write(Double.toString(m.getPosition().longitude));
                writer.write("\n");
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        markers.clear();
        updateStatus();

    }

    public void updateStatus() {
        statusText.setText(String.format("Markers: %d, Anchors: %d", markers.size(), deviceScanActivity.getAnchorsCount()));
    }

}
