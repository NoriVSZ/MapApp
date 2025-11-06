package com.example.appmap;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private MapView map;
    private JSONArray locationsArray;
    private LocationManager locationManager;
    private GeoPoint myLocation;
    private Marker myLocationMarker;
    private boolean isFirstLocationUpdate = true;
    private CompassOverlay mCompassOverlay;
    private Spinner distanceFilterSpinner;
    private Spinner statusFilterSpinner;
    private String currentDistanceFilter = "Mundial";
    private String currentStatusFilter = "Todos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        setContentView(R.layout.activity_main);

        map = findViewById(R.id.map);
        map.setMultiTouchControls(true);

        mCompassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this), map);
        mCompassOverlay.enableCompass();
        map.getOverlays().add(mCompassOverlay);

        distanceFilterSpinner = findViewById(R.id.distance_filter_spinner);
        statusFilterSpinner = findViewById(R.id.status_filter_spinner);
        setupFilters();

        FloatingActionButton fab = findViewById(R.id.fab_my_location);
        fab.setOnClickListener(v -> {
            if (myLocation != null) {
                map.getController().animateTo(myLocation);
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        } else {
            setupLocation();
        }

        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                InfoWindow.closeAllInfoWindowsOn(map);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                showAddLocationDialog(p);
                return true;
            }
        };
        MapEventsOverlay OverlayEvents = new MapEventsOverlay(mReceive);
        map.getOverlays().add(0, OverlayEvents); // Add at the beginning

        loadLocations();
    }

    private void setupFilters() {
        ArrayAdapter<CharSequence> distanceAdapter = ArrayAdapter.createFromResource(this,
                R.array.distance_filter_array, android.R.layout.simple_spinner_item);
        distanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        distanceFilterSpinner.setAdapter(distanceAdapter);

        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.status_filter_array, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusFilterSpinner.setAdapter(statusAdapter);

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentDistanceFilter = distanceFilterSpinner.getSelectedItem().toString();
                currentStatusFilter = statusFilterSpinner.getSelectedItem().toString();
                refreshMarkers();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        distanceFilterSpinner.setOnItemSelectedListener(filterListener);
        statusFilterSpinner.setOnItemSelectedListener(filterListener);
    }

    private void setupLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        myLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
        if (isFirstLocationUpdate && map != null) {
            map.getController().setCenter(myLocation);
            map.getController().setZoom(15.0);
            isFirstLocationUpdate = false;
        }
        updateMyLocationMarker(location.getBearing());

        if (currentDistanceFilter.equals("Por Zona (4km)")) {
            refreshMarkers();
        }
    }

    private void updateMyLocationMarker(float bearing) {
        if (map == null) return;
        if (myLocationMarker == null) {
            myLocationMarker = new Marker(map);
            myLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            myLocationMarker.setTitle("Mi Ubicación");
            myLocationMarker.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_send));
            map.getOverlays().add(myLocationMarker);
        } else {
             if(!map.getOverlays().contains(myLocationMarker)){
                  map.getOverlays().add(myLocationMarker);
             }
        }
        myLocationMarker.setPosition(myLocation);
        myLocationMarker.setRotation(bearing);
        map.invalidate();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupLocation();
            }
        }
    }

    private void showAddLocationDialog(final GeoPoint p) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Agregar nueva ubicación");

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.edit_location_dialog, null);
        builder.setView(dialogView);

        final EditText editTitle = dialogView.findViewById(R.id.edit_title);
        final EditText editDescription = dialogView.findViewById(R.id.edit_description);
        final Spinner statusSpinner = dialogView.findViewById(R.id.edit_status_spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String newTitle = editTitle.getText().toString();
            String newDescription = editDescription.getText().toString();
            String newStatus = statusSpinner.getSelectedItem().toString();
            saveLocation(newTitle, newStatus, newDescription, p, true, null);
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void saveLocation(String title, String status, String description, GeoPoint p, boolean isPublic, JSONObject existingLocation) {
        try {
            JSONObject locationToSave;
            if (existingLocation == null) {
                locationToSave = new JSONObject();
            } else {
                locationToSave = existingLocation;
            }

            locationToSave.put("title", title);
            locationToSave.put("estado", status);
            locationToSave.put("descripcion", description);
            locationToSave.put("latitude", p.getLatitude());
            locationToSave.put("longitude", p.getLongitude());
            locationToSave.put("es_publico", isPublic);

            if (existingLocation == null) {
                locationsArray.put(locationToSave);
            }

            writeLocationsToFile();
            refreshMarkers();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void deleteLocation(JSONObject locationToDelete) {
         new AlertDialog.Builder(this)
                .setTitle("Eliminar Ubicación")
                .setMessage("¿Estás seguro de que quieres eliminar esta ubicación?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    JSONArray newArray = new JSONArray();
                    for (int i = 0; i < locationsArray.length(); i++) {
                        try {
                            if (!locationsArray.getJSONObject(i).toString().equals(locationToDelete.toString())) {
                                newArray.put(locationsArray.getJSONObject(i));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    locationsArray = newArray;
                    writeLocationsToFile();
                    refreshMarkers();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void loadLocations() {
        try {
            File file = new File(getFilesDir(), "locations.json");
            InputStream is;
            if (file.exists() && file.length() > 0) {
                is = new FileInputStream(file);
            } else {
                is = getResources().openRawResource(R.raw.locations);
                FileOutputStream fos = openFileOutput("locations.json", Context.MODE_PRIVATE);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                fos.close();
                is.close();
                is = new FileInputStream(file);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            is.close();

            locationsArray = new JSONArray(sb.toString());
            refreshMarkers();

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            locationsArray = new JSONArray();
        }
    }

    private void writeLocationsToFile() {
        try {
            FileOutputStream fos = openFileOutput("locations.json", Context.MODE_PRIVATE);
            fos.write(locationsArray.toString(2).getBytes());
            fos.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void refreshMarkers() {
        if (map == null) return;
        List<org.osmdroid.views.overlay.Overlay> overlaysToKeep = new ArrayList<>();
        for (org.osmdroid.views.overlay.Overlay overlay : map.getOverlays()){
            if (overlay instanceof MapEventsOverlay || overlay instanceof CompassOverlay || overlay == myLocationMarker) {
                 overlaysToKeep.add(overlay);
            }
        }
        map.getOverlays().clear();
        map.getOverlays().addAll(overlaysToKeep);

        try {
            for (int i = 0; i < locationsArray.length(); i++) {
                JSONObject location = locationsArray.getJSONObject(i);

                if (!currentStatusFilter.equals("Todos") && !location.getString("estado").equals(currentStatusFilter)) {
                    continue;
                }

                GeoPoint point = new GeoPoint(location.getDouble("latitude"), location.getDouble("longitude"));

                if (currentDistanceFilter.equals("Por Zona (4km)")) {
                    if (myLocation == null) continue;
                    Location loc1 = new Location("");
                    loc1.setLatitude(myLocation.getLatitude());
                    loc1.setLongitude(myLocation.getLongitude());

                    Location loc2 = new Location("");
                    loc2.setLatitude(point.getLatitude());
                    loc2.setLongitude(point.getLongitude());

                    if (loc1.distanceTo(loc2) > 4000) {
                        continue;
                    }
                }
                addMarker(point, location);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        map.invalidate();
    }

    private void addMarker(GeoPoint p, final JSONObject location) {
        Marker marker = new Marker(map);
        marker.setPosition(p);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        try {
            marker.setTitle(location.getString("title"));
            marker.setInfoWindow(new CustomInfoWindow(R.layout.custom_info_window, map, location));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        map.getOverlays().add(marker);
    }

    private void showEditLocationDialog(final JSONObject location) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar Ubicación");

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.edit_location_dialog, null);
        builder.setView(dialogView);

        final EditText editTitle = dialogView.findViewById(R.id.edit_title);
        final EditText editDescription = dialogView.findViewById(R.id.edit_description);
        final Spinner statusSpinner = dialogView.findViewById(R.id.edit_status_spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);

        try {
            editTitle.setText(location.getString("title"));
            editDescription.setText(location.getString("descripcion"));
            String status = location.getString("estado");
            if (status != null) {
                int spinnerPosition = adapter.getPosition(status);
                statusSpinner.setSelection(spinnerPosition);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            try {
                String newTitle = editTitle.getText().toString();
                String newDescription = editDescription.getText().toString();
                String newStatus = statusSpinner.getSelectedItem().toString();
                GeoPoint point = new GeoPoint(location.getDouble("latitude"), location.getDouble("longitude"));
                boolean isPublic = location.getBoolean("es_publico");

                saveLocation(newTitle, newStatus, newDescription, point, isPublic, location);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private class CustomInfoWindow extends InfoWindow {
        private final JSONObject location;

        public CustomInfoWindow(int layoutResId, MapView mapView, JSONObject location) {
            super(layoutResId, mapView);
            this.location = location;
        }

        @Override
        public void onOpen(Object arg0) {
            TextView title = mView.findViewById(R.id.info_title);
            TextView description = mView.findViewById(R.id.info_description);
            TextView status = mView.findViewById(R.id.info_status);
            Button editButton = mView.findViewById(R.id.info_edit_button);
            Button deleteButton = mView.findViewById(R.id.info_delete_button);

            try {
                title.setText(location.getString("title"));
                description.setText(location.getString("descripcion"));
                status.setText("Estado: " + location.getString("estado"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            editButton.setOnClickListener(v -> {
                showEditLocationDialog(location);
                close();
            });

            deleteButton.setOnClickListener(v -> {
                deleteLocation(location);
                close();
            });
        }

        @Override
        public void onClose() {
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
        if (mCompassOverlay != null) {
            mCompassOverlay.enableCompass();
        }
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, this);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
        if (mCompassOverlay != null) {
            mCompassOverlay.disableCompass();
        }
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }
}
