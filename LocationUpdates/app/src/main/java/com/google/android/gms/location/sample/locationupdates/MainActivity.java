/*
  Copyright 2017 Google Inc. All Rights Reserved.
  <p>
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  <p>
  http://www.apache.org/licenses/LICENSE-2.0
  <p>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.google.android.gms.location.sample.locationupdates;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Double.parseDouble;


/**
 * Using location settings.
 * <p/>
 * Uses the {@link com.google.android.gms.location.SettingsApi} to ensure that the device's system
 * settings are properly configured for the app's location needs. When making a request to
 * Location services, the device's system settings may be in a state that prevents the app from
 * obtaining the location data that it needs. For example, GPS or Wi-Fi scanning may be switched
 * off. The {@code SettingsApi} makes it possible to determine if a device's system settings are
 * adequate for the location request, and to optionally invoke a dialog that allows the user to
 * enable the necessary settings.
 * <p/>
 * This sample allows the user to request location updates using the ACCESS_FINE_LOCATION setting
 * (as specified in AndroidManifest.xml).
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Code used in requesting runtime permissions.
     */
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    /**
     * Constant used in the location settings dialog.
     */
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 100;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Keys for storing activity state in the Bundle.
    private final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    private final static String KEY_LOCATION = "location";
    private final static String KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string";

    private final static String BUS_STOP_JSON_URL = "https://data.etabus.gov.hk/v1/transport/kmb/stop";
    private final static String BUS_STOP_ETA_JSON_URL = "https://data.etabus.gov.hk/v1/transport/kmb/stop-eta/";

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Provides access to the Location Settings API.
     */
    private SettingsClient mSettingsClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private LocationSettingsRequest mLocationSettingsRequest;

    /**
     * Callback for Location events.
     */
    private LocationCallback mLocationCallback;

    /**
     * Represents a geographical location.
     */
    private Location mCurrentLocation;

    // UI Widgets.
    private Button mStartUpdatesButton;
    private Button mStopUpdatesButton;
    private TextView mLastUpdateTimeTextView;
    private TextView mLatitudeTextView;
    private TextView mLongitudeTextView;
    private TextView progressText;
    private TextView etaProgressText;
    private TextView busStopJSONTextView;
    private TextView busStop1TextView;
    private TextView busStop2TextView;
    private TextView busStop3TextView;
    private TextView busStop4TextView;
    private TextView busStop5TextView;
    private TextView stopEtaJSONTextView;
    private TextView eta1TextView;
    private TextView eta2TextView;
    private TextView eta3TextView;
    private TextView eta4TextView;
    private TextView eta5TextView;

    // Labels.
    private String mLatitudeLabel;
    private String mLongitudeLabel;
    private String mLastUpdateTimeLabel;

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    private Boolean mRequestingLocationUpdates;

    /**
     * Time when the location was updated represented as a String.
     */
    private String mLastUpdateTime;

    DownloadManager downloadManager;
    long downloadReference;

    ProgressBar progressBar;
    ProgressBar etaProgressBar;
    Timer progressTimer;
    ArrayList<String[]> closestStop = new ArrayList<String[]>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        // Locate the UI widgets.
        mStartUpdatesButton = (Button) findViewById(R.id.start_updates_button);
        mStopUpdatesButton = (Button) findViewById(R.id.stop_updates_button);
        mLatitudeTextView = (TextView) findViewById(R.id.latitude_text);
        mLongitudeTextView = (TextView) findViewById(R.id.longitude_text);
        mLastUpdateTimeTextView = (TextView) findViewById(R.id.last_update_time_text);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        etaProgressBar = (ProgressBar) findViewById(R.id.etaProgressBar);


        progressText = (TextView) findViewById(R.id.progressText);
        etaProgressText = (TextView) findViewById(R.id.etaProgressText);
        busStopJSONTextView = (TextView) findViewById(R.id.bus_stop_json);
        busStop1TextView = (TextView) findViewById(R.id.bus_stop_1);
        busStop2TextView = (TextView) findViewById(R.id.bus_stop_2);
        busStop3TextView = (TextView) findViewById(R.id.bus_stop_3);
        busStop4TextView = (TextView) findViewById(R.id.bus_stop_4);
        busStop5TextView = (TextView) findViewById(R.id.bus_stop_5);
        stopEtaJSONTextView = (TextView) findViewById(R.id.stop_eta_json);
        eta1TextView = (TextView) findViewById(R.id.eta_1);
        eta2TextView = (TextView) findViewById(R.id.eta_2);
        eta3TextView = (TextView) findViewById(R.id.eta_3);
        eta4TextView = (TextView) findViewById(R.id.eta_4);
        eta5TextView = (TextView) findViewById(R.id.eta_5);

        // Set labels.
        mLatitudeLabel = getResources().getString(R.string.latitude_label);
        mLongitudeLabel = getResources().getString(R.string.longitude_label);
        mLastUpdateTimeLabel = getResources().getString(R.string.last_update_time_label);

        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        // Kick off the process of building the LocationCallback, LocationRequest, and
        // LocationSettingsRequest objects.
        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();
    }

    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        KEY_REQUESTING_LOCATION_UPDATES);
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                // Since KEY_LOCATION was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(KEY_LAST_UPDATED_TIME_STRING)) {
                mLastUpdateTime = savedInstanceState.getString(KEY_LAST_UPDATED_TIME_STRING);
            }
            updateUI();
        }
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                mCurrentLocation = locationResult.getLastLocation();
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                updateLocationUI();
            }
        };
    }

    /**
     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        mRequestingLocationUpdates = false;
                        updateUI();
                        break;
                }
                break;
        }
    }

    /**
     * Handles the Start Updates button and requests start of location updates. Does nothing if
     * updates have already been requested.
     */
    public void startUpdatesButtonHandler(View view) {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            setButtonsEnabledState();
            startLocationUpdates();
        }
    }

    /**
     * Handles the Stop Updates button, and requests removal of location updates.
     */
    public void stopUpdatesButtonHandler(View view) {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        stopLocationUpdates();
    }

    private void downloadJSON(String url, String title, String description, String subPath) {

        //System.out.println("download" + url);

        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        Uri uri = Uri.parse(url); // Path where you want to download file.
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(title);

        //Setting description of request
        request.setDescription(description);


        request.setDestinationInExternalFilesDir(MainActivity.this,
                Environment.DIRECTORY_DOWNLOADS, subPath);


        downloadReference = downloadManager.enqueue(request); // enqueue a new download

    }

    private int DownloadStatus(Cursor cursor) {

        //https://www.codeproject.com/articles/1112730/android-download-manager-tutorial-how-to-download

        //column for download  status
        int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
        int status = cursor.getInt(columnIndex);
        //column for reason code if the download failed or paused
        int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
        int reason = cursor.getInt(columnReason);
        //get the download filename
        //int filenameIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
        //String filename = cursor.getString(filenameIndex);

        String statusText = "";
        String reasonText = "";

        switch (status) {
            case DownloadManager.STATUS_FAILED:
                statusText = "STATUS_FAILED";
                switch (reason) {
                    case DownloadManager.ERROR_CANNOT_RESUME:
                        reasonText = "ERROR_CANNOT_RESUME";
                        break;
                    case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                        reasonText = "ERROR_DEVICE_NOT_FOUND";
                        break;
                    case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                        reasonText = "ERROR_FILE_ALREADY_EXISTS";
                        break;
                    case DownloadManager.ERROR_FILE_ERROR:
                        reasonText = "ERROR_FILE_ERROR";
                        break;
                    case DownloadManager.ERROR_HTTP_DATA_ERROR:
                        reasonText = "ERROR_HTTP_DATA_ERROR";
                        break;
                    case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                        reasonText = "ERROR_INSUFFICIENT_SPACE";
                        break;
                    case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                        reasonText = "ERROR_TOO_MANY_REDIRECTS";
                        break;
                    case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                        reasonText = "ERROR_UNHANDLED_HTTP_CODE";
                        break;
                    case DownloadManager.ERROR_UNKNOWN:
                        reasonText = "ERROR_UNKNOWN";
                        break;
                }
                break;
            case DownloadManager.STATUS_PAUSED:
                statusText = "STATUS_PAUSED";
                switch (reason) {
                    case DownloadManager.PAUSED_QUEUED_FOR_WIFI:
                        reasonText = "PAUSED_QUEUED_FOR_WIFI";
                        break;
                    case DownloadManager.PAUSED_UNKNOWN:
                        reasonText = "PAUSED_UNKNOWN";
                        break;
                    case DownloadManager.PAUSED_WAITING_FOR_NETWORK:
                        reasonText = "PAUSED_WAITING_FOR_NETWORK";
                        break;
                    case DownloadManager.PAUSED_WAITING_TO_RETRY:
                        reasonText = "PAUSED_WAITING_TO_RETRY";
                        break;
                }
                break;
            case DownloadManager.STATUS_PENDING:
                statusText = "STATUS_PENDING";
                break;
            case DownloadManager.STATUS_RUNNING:
                statusText = "STATUS_RUNNING";
                break;
            case DownloadManager.STATUS_SUCCESSFUL:
                statusText = "STATUS_SUCCESSFUL";
                break;
        }

        return status;


    }


    private void checkDownloadStatusFunction(final TextView progressText, final ProgressBar progressBar, final String tmpFileName, final String realFileName) {
        System.out.println(tmpFileName);
        System.out.println(realFileName);
        // update progressbar
        progressTimer = new Timer();
        progressTimer.schedule(new TimerTask() {
            @Override
            public void run() {

                DownloadManager.Query downloadQuery = new DownloadManager.Query();
                downloadQuery.setFilterById(downloadReference);

                final Cursor cursor = downloadManager.query(downloadQuery);

                if (cursor.moveToFirst()) { // this "if" is crucial to prevent a kind of error
                    final int downloadedBytes = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    final int totalBytes = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)); // integer is enough for files under 2GB

                    int downloadStatus = DownloadStatus(cursor);
                    removeTmpFile(tmpFileName, realFileName);


                    cursor.close();

                    final float downloadProgress = downloadedBytes * 100f / totalBytes;
                    if (downloadProgress > 99.9) // stop repeating timer (it's also useful for error prevention)
                        progressTimer.cancel();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressText.setText(downloadedBytes + " bytes\n" + totalBytes + " bytes\n" + downloadProgress + "%");
                            progressBar.setProgress((int) downloadProgress);
                        }
                    });
                }


            }
        }, 0, 10);
    }

    public void removeTmpFile( String tmpFileName, String realFileName) {

        File filePath = MainActivity.this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);

        File file = new File(filePath, tmpFileName);
        File file2 = new File(filePath, realFileName);

        if (file.exists()) {
            file2.delete();
            file.renameTo(file2);
            file.delete();
        }


    }

    public void startDownloadJSONButtonHandler(View view) {

        //https://stackoverflow.com/questions/15542641/how-to-show-download-progress-in-progress-bar-in-android
        downloadJSON(BUS_STOP_JSON_URL, "Bus Stop Data", "Bus Stop Data", "busStopTmp.json");
        checkDownloadStatusFunction(progressText, progressBar, "busStopTmp.json", "busStop.json");


    }

    public void startReadJSONButtonHandler(View view) throws Exception {


        ArrayList<String[]> distanceArray = new ArrayList<String[]>();
        ArrayList<Object> listData = new ArrayList<Object>();


        convertJsonToArrayList(listData, "busStop.json", busStopJSONTextView);
        createDistanceArray(distanceArray, listData);
        sortDistanceArray(distanceArray);
        outputDistanceData(distanceArray, closestStop);


    }

    public void startReadEtaJSONButtonHandler(View view) throws Exception {

        ArrayList<ArrayList<String[]>> fiveEtaArray =  new ArrayList<ArrayList<String[]>>();

        for (int i = 0; i < 5; i++) {
            ArrayList<Object> listData = new ArrayList<Object>();
            ArrayList<String[]> oneEtaArray = new ArrayList<String[]>();

            String[] stopInfoArray = closestStop.get(i);
            String stopID = stopInfoArray[0];
            convertJsonToArrayList(listData, stopID + " ETA.json", stopEtaJSONTextView);
            createEtaArray(oneEtaArray, listData);
            fiveEtaArray.add(oneEtaArray);
        }

        outputEtaData(fiveEtaArray);

    }

    public void startDownloadEtaJSONButtonHandler(View view) throws Exception {
        for (int i = 0; i < 5; i++) {
            TextView dummyTxt = new TextView(MainActivity.this);
            ProgressBar dummyProgressBar = new ProgressBar(MainActivity.this);

            String[] stopInfoArray = closestStop.get(i);
            String stopID = stopInfoArray[0];

            System.out.println(BUS_STOP_ETA_JSON_URL + stopID);
            downloadJSON(BUS_STOP_ETA_JSON_URL + stopID, stopID + " Stop ETA", stopID + " Stop ETA", stopID + " ETA_Tmp.json");
            checkDownloadStatusFunction(etaProgressText, etaProgressBar, stopID + " ETA_Tmp.json", stopID + " ETA.json");
        }
    }

    private void convertJsonToArrayList(ArrayList<Object> listData, String filename, TextView showTextView) throws Exception {


        //https://stackoverflow.com/questions/31670076/android-download-and-store-json-so-app-can-work-offline

        //File path = Environment.getExternalStoragePublicDirectory(
        //       Environment.DIRECTORY_DOWNLOADS);
        //File file = new File(path, "busStop.json");

        File filePath = MainActivity.this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        //System.out.println(yourFilePath);
        //File yourFile = new File(yourFilePath);
        File file = new File(filePath, filename);

        //System.out.println(yourFile.exists());

        FileInputStream fileStream = new FileInputStream(file);


        String JSONString = convertStreamToString(fileStream);
        //Make sure you close all streams.
        fileStream.close();

        String showInTextView = JSONString.substring(0, 100);
        showTextView.setText(showInTextView);

        JSONObject result = new JSONObject(JSONString);

        //System.out.println(result.get("type"));
        JSONArray jsonArray = result.getJSONArray("data");


        //Checking whether the JSON array has some value or not
        if (jsonArray != null) {

            //Iterating JSON array
            for (int i = 0; i < jsonArray.length(); i++) {

                //Adding each element of JSON array into ArrayList
                listData.add(jsonArray.get(i));
            }
        }
    }

    private void createDistanceArray(ArrayList<String[]> distanceArray, ArrayList<Object> listData) throws JSONException {


        double lat = 0;
        double lon = 0;

        if (mCurrentLocation != null) {
            lat = mCurrentLocation.getLatitude();
        }
        if (mCurrentLocation != null) {
            lon = mCurrentLocation.getLongitude();
        }

        for (int i = 0; i < listData.size(); i++) {
            Object array = listData.get(i);
            String[] tmpArray = new String[7];
            //System.out.println(array.toString());

            JSONObject result2 = new JSONObject(array.toString());

            double stopLat = Float.parseFloat(result2.get("lat").toString());
            double stopLon = Float.parseFloat(result2.get("long").toString());


            double distance;

            double latDiff = lat - stopLat;
            double lonDiff = lon - stopLon;

            distance = Math.sqrt(Math.pow(latDiff, 2) + Math.pow(lonDiff, 2));

            /*System.out.println(" ");
            System.out.println(result2.get("name_tc").toString());
            System.out.println("distance " + distance);
            System.out.println("latDiff" + " " + latDiff);
            System.out.println("lonDiff" + " " + lonDiff);
            System.out.println("Stop location" + " " + stopLat + " " + stopLon);
            System.out.println("current location" + " " + lat + " " + lon);*/

            result2.accumulate("distance", distance);
            //System.out.println(result2.toString());

            tmpArray[0] = result2.get("stop").toString();
            tmpArray[1] = result2.get("name_en").toString();
            tmpArray[2] = result2.get("name_tc").toString();
            tmpArray[3] = result2.get("name_sc").toString();
            tmpArray[4] = result2.get("lat").toString();
            tmpArray[5] = result2.get("long").toString();
            tmpArray[6] = result2.get("distance").toString();

            distanceArray.add(tmpArray);

        }

    }

    private void createEtaArray(ArrayList<String[]> etaArray, ArrayList<Object> listData) throws JSONException {

        for (int i = 0; i < listData.size(); i++) {
            Object array = listData.get(i);
            String[] tmpArray = new String[14];
            //System.out.println(array.toString());

            JSONObject result2 = new JSONObject(array.toString());

            //System.out.println(result2.toString());

            tmpArray[0] = result2.get("co").toString();
            tmpArray[1] = result2.get("route").toString();
            tmpArray[2] = result2.get("dir").toString();
            tmpArray[3] = result2.get("service_type").toString();
            tmpArray[4] = result2.get("seq").toString();
            tmpArray[5] = result2.get("dest_tc").toString();
            tmpArray[6] = result2.get("dest_sc").toString();
            tmpArray[7] = result2.get("dest_en").toString();
            tmpArray[8] = result2.get("eta_seq").toString();
            tmpArray[9] = result2.get("eta").toString();
            tmpArray[10] = result2.get("rmk_tc").toString();
            tmpArray[11] = result2.get("rmk_sc").toString();
            tmpArray[12] = result2.get("rmk_en").toString();
            tmpArray[13] = result2.get("data_timestamp").toString();

            etaArray.add(tmpArray);

        }

    }

    private void outputEtaData(ArrayList<ArrayList<String[]>> fiveEtaArray) {

        // Output list
        /*for(String[] strs : distanceArray) {
            System.out.println(Arrays.toString(strs));
        }*/

        /*System.out.println(Arrays.toString(distanceArray.get(0)));
        System.out.println(Arrays.toString(distanceArray.get(1)));
        System.out.println(Arrays.toString(distanceArray.get(2)));
        System.out.println(Arrays.toString(distanceArray.get(3)));
        System.out.println(Arrays.toString(distanceArray.get(4)));*/

        String eta1 = "";
        String eta2 = "";
        String eta3 = "";
        String eta4 = "";
        String eta5 = "";
        String tmp = "";

        for(int j = 0; j<5; j++) {
            tmp = "";
            for (int i = 0; i < fiveEtaArray.get(j).size(); i++) {
                String[] tmpArray = fiveEtaArray.get(j).get(i);
                tmp += tmpArray[1]; //route
                tmp += " " + tmpArray[9]; //eta
                tmp += "\n";
            }
            switch(j){
                case 0:
                    eta1 = tmp;
                case 1:
                    eta2 = tmp;
                case 2:
                    eta3 = tmp;
                case 3:
                    eta4 = tmp;
                case 4:
                    eta5 = tmp;
            }
        }
        eta1TextView.setText(eta1);
        eta2TextView.setText(eta2);
        eta3TextView.setText(eta3);
        eta4TextView.setText(eta4);
        eta5TextView.setText(eta5);

    }

    private void outputDistanceData(ArrayList<String[]> distanceArray, ArrayList<String[]> closestStop) {

        // Output list
        /*for(String[] strs : distanceArray) {
            System.out.println(Arrays.toString(strs));
        }*/

        /*System.out.println(Arrays.toString(distanceArray.get(0)));
        System.out.println(Arrays.toString(distanceArray.get(1)));
        System.out.println(Arrays.toString(distanceArray.get(2)));
        System.out.println(Arrays.toString(distanceArray.get(3)));
        System.out.println(Arrays.toString(distanceArray.get(4)));*/

        busStop1TextView.setText(Arrays.toString(distanceArray.get(0)));
        busStop2TextView.setText(Arrays.toString(distanceArray.get(1)));
        busStop3TextView.setText(Arrays.toString(distanceArray.get(2)));
        busStop4TextView.setText(Arrays.toString(distanceArray.get(3)));
        busStop5TextView.setText(Arrays.toString(distanceArray.get(4)));

        closestStop.add(distanceArray.get(0));
        closestStop.add(distanceArray.get(1));
        closestStop.add(distanceArray.get(2));
        closestStop.add(distanceArray.get(3));
        closestStop.add(distanceArray.get(4));

    }

    private void sortDistanceArray(ArrayList<String[]> distanceArray) {
        // Sort list
        Collections.sort(distanceArray, new Comparator<String[]>() {
            public int compare(String[] x, String[] y) {
                if (parseDouble(x[6]) < parseDouble(y[6])) {
                    return -1;
                } else if (parseDouble(x[6]) == parseDouble(y[6])) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });
    }


    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());

                        updateUI();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                mRequestingLocationUpdates = false;
                        }

                        updateUI();
                    }
                });
    }

    /**
     * Updates all UI fields.
     */
    private void updateUI() {
        setButtonsEnabledState();
        updateLocationUI();
    }

    /**
     * Disables both buttons when functionality is disabled due to insuffucient location settings.
     * Otherwise ensures that only one button is enabled at any time. The Start Updates button is
     * enabled if the user is not requesting location updates. The Stop Updates button is enabled
     * if the user is requesting location updates.
     */
    private void setButtonsEnabledState() {
        if (mRequestingLocationUpdates) {
            mStartUpdatesButton.setEnabled(false);
            mStopUpdatesButton.setEnabled(true);
        } else {
            mStartUpdatesButton.setEnabled(true);
            mStopUpdatesButton.setEnabled(false);
        }
    }

    /**
     * Sets the value of the UI fields for the location latitude, longitude and last update time.
     */
    private void updateLocationUI() {
        if (mCurrentLocation != null) {
            mLatitudeTextView.setText(String.format(Locale.ENGLISH, "%s: %f", mLatitudeLabel,
                    mCurrentLocation.getLatitude()));
            mLongitudeTextView.setText(String.format(Locale.ENGLISH, "%s: %f", mLongitudeLabel,
                    mCurrentLocation.getLongitude()));
            mLastUpdateTimeTextView.setText(String.format(Locale.ENGLISH, "%s: %s",
                    mLastUpdateTimeLabel, mLastUpdateTime));


        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.");
            return;
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mRequestingLocationUpdates = false;
                        setButtonsEnabledState();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we remove location updates. Here, we resume receiving
        // location updates if the user has requested them.
        if (mRequestingLocationUpdates && checkPermissions()) {
            startLocationUpdates();
        } else if (!checkPermissions()) {
            requestPermissions();
        }

        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Remove location updates to save battery.
        stopLocationUpdates();
    }

    /**
     * Stores activity data in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(KEY_LOCATION, mCurrentLocation);
        savedInstanceState.putString(KEY_LAST_UPDATED_TIME_STRING, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates) {
                    Log.i(TAG, "Permission granted, updates requested, starting location updates");
                    startLocationUpdates();
                }
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }
}
