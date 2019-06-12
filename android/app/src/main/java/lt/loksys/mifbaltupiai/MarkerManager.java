package lt.loksys.mifbaltupiai;

import android.os.Handler;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MarkerManager {

    private static final int MARKER_REMOVE_PERIOD = 3000;
    private Handler markerRemoveHandler = new Handler();

    private final MainActivity activity;
    private final ConcurrentMap<Marker, Long> markers = new ConcurrentHashMap<>();

    public MarkerManager(final MainActivity activity) {
        this.activity = activity;
    }

    public void initialize() {
        activity.getMap().setOnMapClickListener(new GoogleMap.OnMapClickListener()
        {
            @Override
            public void onMapClick(LatLng coord)
            {
                final Marker marker = activity.getMap().addMarker(new MarkerOptions()
                        .position(coord)
                        .title("My Spot")
                        .snippet("This is my spot!")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                markers.put(marker, System.currentTimeMillis());
                markerRemoveHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        marker.remove();
                    }
                }, MARKER_REMOVE_PERIOD);
                activity.updateStatus();
            }

        });
    }

    public int getSize() {
        return markers.size();
    }

    public void clear() {
        for (Marker m: markers.keySet()) {
            m.remove();
        }
        markers.clear();
        this.activity.updateStatus();
    }

    public void save() {

        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String fileName = "m".concat(timeStamp.concat(".csv"));
        String filePath = activity.getSavePath() + File.separator + fileName;

        File f = new File(filePath);
        android.util.Log.i("Saving!", f.toString());

        try {
            FileWriter writer = new FileWriter(f);
            for (Map.Entry<Marker, Long> e: markers.entrySet()) {
                writer.write(Long.toString(e.getValue()));
                writer.write(",");
                writer.write(Double.toString(e.getKey().getPosition().latitude));
                writer.write(",");
                writer.write(Double.toString(e.getKey().getPosition().longitude));
                writer.write("\n");
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        clear();
        activity.updateStatus();

    }


}