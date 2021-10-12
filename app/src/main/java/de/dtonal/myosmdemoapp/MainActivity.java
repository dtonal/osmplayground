package de.dtonal.myosmdemoapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.mapsforge.map.android.rendertheme.AssetsRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.config.Configuration;
import org.osmdroid.mapsforge.MapsForgeTileProvider;
import org.osmdroid.mapsforge.MapsForgeTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import org.osmdroid.views.overlay.Polyline;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.ticofab.androidgpxparser.parser.GPXParser;
import io.ticofab.androidgpxparser.parser.domain.Gpx;

public class MainActivity extends AppCompatActivity {
    private static final String MY_USER_AGENT = "USERAGENT";
    private static final int PICK_GPX_FILE = 12;
    private static final int PICK_MAP_FILE = 13;
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    private Button fileButton = null;
    private Button mapButton = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        super.onCreate(savedInstanceState);
        //handle permissions first, before map is created. not depicted here
       // StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
       // StrictMode.setThreadPolicy(policy);
        Configuration.getInstance().setUserAgentValue("MyOwnUserAgent/1.0");
        setContentView(R.layout.activity_main);
        map = (MapView) findViewById(R.id.map);
        map.setMultiTouchControls(true);
//
//        fileButton = (Button) findViewById(R.id.fileButton);
//        fileButton.setOnClickListener((view) -> onFileClicked());
//
//
//        mapButton = (Button) findViewById(R.id.mapButton);
//        mapButton.setOnClickListener((view) -> onMapFileClicked());
//
//
//
//        GeoPoint startPoint = new GeoPoint(48.7969087,9.0157356);
//        IMapController mapController = map.getController();
//        mapController.setZoom(15.0);
//        mapController.setCenter(startPoint);
//
//        Marker startMarker = new Marker(map);
//        startMarker.setPosition(startPoint);
//        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//        map.getOverlays().add(startMarker);
//
//        startMarker.setIcon(getResources().getDrawable(R.drawable.ic_muh_foreground, null));
//        startMarker.setTitle("Start point");
//
//        RoadManager roadManager = new OSRMRoadManager(this, MY_USER_AGENT);
//
//        ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
//        GeoPoint start = new GeoPoint(48.796477593248795, 9.011379737789367);
//        waypoints.add(startPoint);
//        GeoPoint endPoint = new GeoPoint(48.794717776143095, 9.022998642089236);
//        waypoints.add(endPoint);
//
//        Road road = roadManager.getRoad(waypoints);
//        Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
//        map.getOverlays().add(roadOverlay);
//
//        Drawable nodeIcon = getResources().getDrawable(R.drawable.marker_node_foreground, null);
//        for (int i=0; i<road.mNodes.size(); i++){
//            RoadNode node = road.mNodes.get(i);
//            Marker nodeMarker = new Marker(map);
//            nodeMarker.setPosition(node.mLocation);
//            nodeMarker.setIcon(nodeIcon);
//            nodeMarker.setTitle("Step "+i);
//            map.getOverlays().add(nodeMarker);
//        }

        handleMapFile();
        parseGpx();
        map.invalidate();

    }



    @Override
    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    private void handleMapFile() {
        MapsForgeTileSource.createInstance(this.getApplication());
        File[] maps = new File[1];
        try(InputStream in = this.getAssets().open("map/soerm.map")){
        //try (InputStream in = getContentResolver().openInputStream(fileUri)){
            File mapFile = new File(getCacheDir()+"/test.map");
            //File root = new File(Environment.getExternalStorageDirectory(), "Notes");
            //if (!root.exists()) {
              //  root.mkdirs();
            //}
            //File mapFile = new File(root,"test.map");
            copyInputStreamToFile(in, mapFile);
            maps[0] = mapFile;
            boolean exists = maps[0].exists();

            XmlRenderTheme theme = null; //null is ok here, uses the default rendering theme if it's not set
            try {
//this file should be picked up by the mapsforge dependencies
                theme = new AssetsRenderTheme(getApplicationContext(), "renderthemes/", "rendertheme-v4.xml");
                //alternative: theme = new ExternalRenderTheme(userDefinedRenderingFile);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            MapsForgeTileSource fromFiles = MapsForgeTileSource.createFromFiles(maps, theme, "rendertheme-v4");
            MapsForgeTileProvider forge = new MapsForgeTileProvider(
                    new SimpleRegisterReceiver(getApplicationContext()),
                    fromFiles, null);

            map.setTileProvider(forge);


//now for a magic trick
//since we have no idea what will be on the
//user's device and what geographic area it is, this will attempt to center the map
//on whatever the map data provides
            map.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            map.zoomToBoundingBox(fromFiles.getBoundsOsmdroid(), false);
                        }
                    });
        } catch (IOException e) {
            // do something with this exception
            e.printStackTrace();
        }



    }

    // Copy an InputStream to a File.
//
    private void copyInputStreamToFile(InputStream in, File file) {
        OutputStream out = null;

        try {
            out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            // Ensure that the InputStreams are closed even if there's an exception.
            try {
                if ( out != null ) {
                    out.close();
                }

                // If you want to close the "in" InputStream yourself then remove this
                // from here but ensure that you close it yourself eventually.
                in.close();
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }


    private void parseGpx() {
        GPXParser parser = new GPXParser(); // consider injection

        Gpx parsedGpx = null;

        try(InputStream in = this.getAssets().open("gpx/001.gpx")){
        //try (InputStream in = getContentResolver().openInputStream(fileUri)){
            parsedGpx = parser.parse(in); // consider using a background thread
        } catch (IOException | XmlPullParserException e) {
            // do something with this exception
            e.printStackTrace();
        }
        if (parsedGpx == null) {
            // error parsing track
        } else {
            parsedGpx.getRoutes();
            // do something with the parsed track
            // see included example app and tests

            List<GeoPoint> waypoints = parsedGpx.getTracks().get(0).getTrackSegments().get(0).getTrackPoints().stream().map(point -> new GeoPoint(point.getLatitude(), point.getLongitude())).collect(Collectors.toList());
            Polyline polyline = new Polyline();
            polyline.setPoints(waypoints);
            map.getOverlayManager().add(polyline);

            map.invalidate();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            permissionsToRequest.add(permissions[i]);
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }


    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }
}