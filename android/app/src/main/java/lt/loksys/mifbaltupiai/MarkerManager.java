package lt.loksys.mifbaltupiai;

import android.os.Handler;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

public class MarkerManager implements GoogleMap.OnMarkerClickListener {

    private static final int MARKER_REMOVE_PERIOD = 3000;
    private Handler markerRemoveHandler = new Handler();

    private final MainActivity activity;
    private final Map<Marker, Integer> graph = new HashMap<>();
    private final ConcurrentLinkedDeque<MarkerClick> markers = new ConcurrentLinkedDeque<>();

    public MarkerManager(final MainActivity activity) {

        this.activity = activity;
        this.activity.getMap().setOnMarkerClickListener(this);

        try {

            InputStream input = activity.getResources().openRawResource(R.raw.grafas);
            CSVReader reader = new CSVReader(new InputStreamReader(input));

            for (String[] line: reader.readAll()) {
                final Marker marker = activity.getMap().addMarker(new MarkerOptions()
                        .position(new LatLng(Double.valueOf(line[1]), Double.valueOf(line[2])))
                        .anchor(0.5f, 0.5f)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.black_point)));
                graph.put(marker, Integer.valueOf(line[0]));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public int getSize() {
        return markers.size();
    }

    public void clear() {
        markers.removeLast();
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
            while (!markers.isEmpty()) {
                MarkerClick click = markers.pop();
                StringBuilder line = new StringBuilder();
                line.append(click.time).append(",");
                line.append(click.id).append(",");
                line.append(click.pos.latitude).append(",");
                line.append(click.pos.longitude).append("\n");
                writer.write(line.toString());
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        activity.updateStatus();

    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        final Marker current = activity.getMap().addMarker(new MarkerOptions()
                .position(marker.getPosition())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        markerRemoveHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                current.remove();
            }
        }, MARKER_REMOVE_PERIOD);

        markers.add(new MarkerClick(marker));
        activity.updateStatus();
        return false;
    }

    private class MarkerClick {

        LatLng pos;
        Integer id;
        Long time = System.currentTimeMillis();

        MarkerClick(Marker marker) {
            this.pos = marker.getPosition();
            this.id = graph.get(marker);
        }

    }

}
