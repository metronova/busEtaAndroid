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
import android.annotation.SuppressLint;
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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
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
import java.io.FileNotFoundException;
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

    private final static String BUS_STOP_JSON_FILE_NAME = "stop/busStop";
    private final static String BUS_STOP_JSON_FILE_TMP_NAME = "stop/busStop_Tmp";

    private final static String STOP_ETA_JSON_FILE_NAME = "eta/ETA_";
    private final static String STOP_ETA_JSON_FILE_TMP_NAME = "eta/ETA_Tmp_";
    private final static String STOP_ETA_JSON_FOLDER_NAME = "eta";
    private final static String JSON_SUFFIX = ".json";

    private final static int closestStopCount = 20;

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
    private TextView deleteTextView;

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
    //Timer progressTimer;
    ArrayList<BusStop> closestStop = new ArrayList<BusStop>();


    LinearLayout linearLayout;
    RelativeLayout etaProgressLayout;
    RelativeLayout etaDataLayout;
    RelativeLayout busStopLayout;

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
        progressText = (TextView) findViewById(R.id.progressText);

        busStopJSONTextView = (TextView) findViewById(R.id.bus_stop_json);
        stopEtaJSONTextView = (TextView) findViewById(R.id.stop_eta_update_time);
        deleteTextView = (TextView) findViewById(R.id.delete_text);


        linearLayout = (LinearLayout) findViewById(R.id.linear_layout);
        etaProgressLayout = (RelativeLayout) findViewById(R.id.eta_progress_layout);
        etaDataLayout = (RelativeLayout) findViewById(R.id.eta_data_layout);
        busStopLayout = (RelativeLayout) findViewById(R.id.bus_stop_layout);

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


    @Override
    protected void onPause() {
        super.onPause();

        // Remove location updates to save battery.
        stopLocationUpdates();
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


    /**
     * Stores activity data in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(KEY_LOCATION, mCurrentLocation);
        savedInstanceState.putString(KEY_LAST_UPDATED_TIME_STRING, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
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

    //////////////////////////////////////////////////////////
    //Button Handler
    //////////////////////////////////////////////////////////

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

    public void deleteBusStopJsonButtonHandler(View view) {

        File filePath = MainActivity.this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);

        File file = new File(filePath, BUS_STOP_JSON_FILE_NAME);

        if (file.exists()) {
            file.delete();
            deleteTextView.setText("Bus Stop JSON deleted.");
        } else {
            deleteTextView.setText("Bus Stop JSON not found.");
        }


    }

    public void deleteStopEtaJsonButtonHandler(View view) {

        File filePath = MainActivity.this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);

        File dir = new File(filePath, STOP_ETA_JSON_FOLDER_NAME);
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                new File(dir, children[i]).delete();
            }

            if (dir.delete()) {
                deleteTextView.setText("Stop ETA JSON deleted.");
            }

        } else {
            deleteTextView.setText("Stop ETA JSON not found.");
        }


    }

    public void startDownloadJSONButtonHandler(View view) {

        //https://stackoverflow.com/questions/15542641/how-to-show-download-progress-in-progress-bar-in-android
        downloadJSON(BUS_STOP_JSON_URL, "Bus Stop Data", "Bus Stop Data", BUS_STOP_JSON_FILE_NAME);
        checkDownloadStatusFunction(progressText, progressBar, BUS_STOP_JSON_FILE_TMP_NAME, BUS_STOP_JSON_FILE_NAME);


    }

    public void startReadJSONButtonHandler(View view) throws Exception {


        ArrayList<BusStop> distanceArray = new ArrayList<BusStop>();
        ArrayList<Object> listData = new ArrayList<Object>();
        closestStop = new ArrayList<BusStop>();


        try {

            convertJsonToArrayList(listData, BUS_STOP_JSON_FILE_NAME, busStopJSONTextView);
            createDistanceArray(distanceArray, listData);
            sortDistanceArray(distanceArray);
            outputDistanceData(distanceArray, closestStop);
        } catch (FileNotFoundException e) {
            //busStop1TextView.setText("Bus stop json not found");
            System.out.println("Bus stop json not found");
        } catch (Throwable e) {
            //busStop1TextView.setText(e.toString());
            System.out.println(e.toString());
        }


    }


    public void startDownloadEtaJSONButtonHandler(View view) throws Exception {

        //vvvvvvvvvvvvvvvv show download progress text field
        ArrayList<TextView> progressTextArray = new ArrayList<TextView>();
        ArrayList<ProgressBar> progressBarArray = new ArrayList<ProgressBar>();

        etaProgressLayout.removeAllViews();

        int j = 0;
        for (int i = 0; i < closestStop.size(); i++) {
            ProgressBar dummyProgressBar = new ProgressBar(MainActivity.this);
            j += 1;
            dummyProgressBar.setId(j);

            TextView dummyTxt = new TextView(MainActivity.this);
            j += 1;
            dummyTxt.setId(j);

            RelativeLayout.LayoutParams paramBar = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            paramBar.addRule(RelativeLayout.BELOW, j-2);
            etaProgressLayout.addView(dummyProgressBar, paramBar);

            RelativeLayout.LayoutParams paramText = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            paramText.addRule(RelativeLayout.BELOW, j-1);
            etaProgressLayout.addView(dummyTxt, paramText);

            //^^^^^^^^^^^^^^^^^^^^^show download progress text field


            //vvvvvvvvvvvvvvvv show download progress text

            try {
                BusStop busStop = closestStop.get(i);

                String stopID = busStop.getStopID();

                //System.out.println(BUS_STOP_ETA_JSON_URL + stopID);
                downloadJSON(BUS_STOP_ETA_JSON_URL + stopID, stopID + " Stop ETA", stopID + " Stop ETA", STOP_ETA_JSON_FILE_TMP_NAME + stopID);

                checkDownloadStatusFunction(dummyTxt, dummyProgressBar, STOP_ETA_JSON_FILE_TMP_NAME + stopID, STOP_ETA_JSON_FILE_NAME + stopID);


            } catch (IndexOutOfBoundsException e) {
                //etaProgressText1.setText("Closest stop data not found");
                System.out.println("Closest stop data not found");
            } catch (Throwable e) {
                //etaProgressText1.setText(e.toString());
                System.out.println(e.toString());
            }

            //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^show download progress text


        }

    }


    public void startReadEtaJSONButtonHandler(View view) throws Exception {

        ArrayList<ArrayList<StopEta>> outputEtaArray = new ArrayList<ArrayList<StopEta>>();

        //try {
            for (int i = 0; i < closestStop.size(); i++) {
                ArrayList<Object> listData = new ArrayList<Object>();
                ArrayList<StopEta> oneEtaArray = new ArrayList<StopEta>();


                BusStop busStop = closestStop.get(i);
                String stopID = busStop.getStopID();
                convertJsonToArrayList(listData, STOP_ETA_JSON_FILE_NAME + stopID, stopEtaJSONTextView);
                createEtaArray(oneEtaArray, listData);
                outputEtaArray.add(oneEtaArray);


            }
        /*} catch (IndexOutOfBoundsException e) {
            //eta1TextView.setText("Closest stop data not found");
            System.out.println("Closest stop data not found");
        } catch (Throwable e) {
            //eta1TextView.setText((e.toString()));
            System.out.println("output one eta");
            System.out.println(e.toString());
        }*/

       // try {
            outputEtaData(outputEtaArray);
        /*} catch (Throwable e) {
            //eta1TextView.setText((e.toString()));

            System.out.println("output eta");
            System.out.println(e.toString());
        }*/

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

    //////////////////////////////////////////////////////////
    //Location Fnction
    //////////////////////////////////////////////////////////


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
     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
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

    //////////////////////////////////////////////////////////
    //JSON download
    //////////////////////////////////////////////////////////

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


    private void downloadJSON(String url, String title, String description, String subPath) {

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

        // update progressbar
        final long downloadReferenceInUse = downloadReference;

        final Timer progressTimer = new Timer();
        progressTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                DownloadManager.Query downloadQuery = new DownloadManager.Query();
                downloadQuery.setFilterById(downloadReferenceInUse);

                final Cursor cursor = downloadManager.query(downloadQuery);

                if (cursor.moveToFirst()) { // this "if" is crucial to prevent a kind of error
                    final int downloadedBytes = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    final int totalBytes = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)); // integer is enough for files under 2GB
                    final String downloadPath = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI));

                    int downloadStatus = DownloadStatus(cursor);
                    removeTmpFile(tmpFileName, realFileName);


                    cursor.close();

                    final float downloadProgress = downloadedBytes * 100f / totalBytes;
                    //if (downloadProgress > 99.9) // stop repeating timer (it's also useful for error prevention)
                    if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL) { // stop repeating timer (it's also useful for error prevention)
                        progressTimer.cancel();
                    }


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressText.setText(downloadPath + "\n" + downloadedBytes + " bytes\n" + totalBytes + " bytes\n" + downloadProgress + "%");
                            progressBar.setProgress((int) downloadProgress);
                        }
                    });
                }


            }
        }, 0, 10);
    }

    private void removeTmpFile(String tmpFileName, String realFileName) {

        File filePath = MainActivity.this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);

        File file = new File(filePath, tmpFileName);
        File file2 = new File(filePath, realFileName);

        if (file.exists()) {
            file2.delete();
            file.renameTo(file2);
            file.delete();
        }


    }


    private static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }


    //////////////////////////////////////////////////////////
    //Array
    //////////////////////////////////////////////////////////

    private void convertJsonToArrayList(ArrayList<Object> listData, String filename, TextView showTextView) throws Exception {
        //https://stackoverflow.com/questions/31670076/android-download-and-store-json-so-app-can-work-offline


        File filePath = MainActivity.this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(filePath, filename);


        FileInputStream fileStream = new FileInputStream(file);


        String JSONString = convertStreamToString(fileStream);
        //Make sure you close all streams.
        fileStream.close();

        System.out.print(JSONString);

        JSONObject result = new JSONObject(JSONString);


        JSONArray jsonArray = result.getJSONArray("data");
        String timeStamp = result.getString("generated_timestamp");

        String dateTimeString = DateUtil.returnDatetimeString(timeStamp);
        showTextView.setText(dateTimeString);


        //Checking whether the JSON array has some value or not
        if (jsonArray != null) {

            //Iterating JSON array
            for (int i = 0; i < jsonArray.length(); i++) {

                //Adding each element of JSON array into ArrayList
                listData.add(jsonArray.get(i));
            }
        }
    }

    private void createDistanceArray(ArrayList<BusStop> distanceArray, ArrayList<Object> listData) throws JSONException {


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


            BusStop busStop = new BusStop();
            String[] tmpArray = new String[7];

            JSONObject result2 = new JSONObject(array.toString());

            double stopLat = Float.parseFloat(result2.get("lat").toString());
            double stopLon = Float.parseFloat(result2.get("long").toString());


            double distance;

            double latDiff = lat - stopLat;
            double lonDiff = lon - stopLon;

            distance = Math.sqrt(Math.pow(latDiff, 2) + Math.pow(lonDiff, 2));

            result2.accumulate("distance", distance);

            busStop.setStopID(result2.get("stop").toString());
            busStop.setNameEn(result2.get("name_en").toString());
            busStop.setNameTc(result2.get("name_tc").toString());
            busStop.setNameSc(result2.get("name_sc").toString());
            busStop.setLat(result2.get("lat").toString());
            busStop.setLon(result2.get("long").toString());
            busStop.setDistance(result2.get("distance").toString());

            distanceArray.add(busStop);

        }

    }

    private void createEtaArray(ArrayList<StopEta> etaArray, ArrayList<Object> listData) throws JSONException {

        for (int i = 0; i < listData.size(); i++) {
            Object array = listData.get(i);
            StopEta stopEta = new StopEta();

            JSONObject result2 = new JSONObject(array.toString());

            stopEta.setCo(result2.get("co").toString());
            stopEta.setRoute(result2.get("route").toString());
            stopEta.setDir(result2.get("dir").toString());
            stopEta.setServiceType(result2.get("service_type").toString());
            stopEta.setSeq(result2.get("seq").toString());
            stopEta.setDestTc(result2.get("dest_tc").toString());
            stopEta.setDestSc(result2.get("dest_sc").toString());
            stopEta.setDestEn(result2.get("dest_en").toString());
            stopEta.setEtaSeq(result2.get("eta_seq").toString());
            stopEta.setEta(result2.get("eta").toString());
            stopEta.setRmkTc(result2.get("rmk_tc").toString());
            stopEta.setRmkSc(result2.get("rmk_sc").toString());
            stopEta.setRmkEn(result2.get("rmk_en").toString());
            stopEta.setDataTimestamp(result2.get("data_timestamp").toString());

            etaArray.add(stopEta);

        }

    }

    private void outputEtaData(ArrayList<ArrayList<StopEta>> outputEtaArray) {




        etaDataLayout.removeAllViews();

        String etaString = "";
        String tmpString = "";

        ArrayList<TextView> etaDataTextArray = new ArrayList<TextView>();
        for (int i = 0; i < closestStop.size(); i++) {


            //vvvvvvvvvvvvvvvv add text field
            TextView dummyTxt = new TextView(MainActivity.this);
            dummyTxt.setId(i+1);


            RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            param.addRule(RelativeLayout.BELOW, i);
            etaDataLayout.addView(dummyTxt, param);

            etaDataTextArray.add(dummyTxt);

            //^^^^^^^^^^^^^^^^^^^^^^^^^^^^




            //vvvvvvvvvvvvvvvv assign text field value

            BusStop busStop = closestStop.get(i);
            tmpString = "";

            for (int j = 0; j < outputEtaArray.get(i).size(); j++) {
                StopEta stopEta = outputEtaArray.get(i).get(j);
                tmpString += stopEta.toString();
                tmpString += "\n";
            }

            etaString += busStop.getNameTc();
            etaString += "\n";
            etaString += tmpString;

            System.out.println(etaString);


            dummyTxt.setText(etaString);
            //^^^^^^^^^^^^^^^^^^^^^^^
        }



    }

    private void outputDistanceData(ArrayList<BusStop> distanceArray, ArrayList<BusStop> closestStop) {

        busStopLayout.removeAllViews();

        for (int i = 0; i < closestStopCount; i++) {

            //vvvvvvv add to array
            closestStop.add(distanceArray.get(i));
            //^^^^^^^^^^^^^^^^


            //vvvvvvvvvvvvvvv show to text view
            TextView dummyTxt = new TextView(MainActivity.this);
            dummyTxt.setId(i + 1);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.BELOW, i);

            String addText = closestStop.get(i).toString();
            dummyTxt.setText(addText);

            busStopLayout.addView(dummyTxt, params);
            //^^^^^^^^^^^^^^^^^^^^
        }


    }

    private void sortDistanceArray(ArrayList<BusStop> distanceArray) {
        // Sort list
        Collections.sort(distanceArray, new Comparator<BusStop>() {
            public int compare(BusStop x, BusStop y) {
                if (parseDouble(x.getDistance()) < parseDouble(y.getDistance())) {
                    return -1;
                } else if (parseDouble(x.getDistance()) == parseDouble(y.getDistance())) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });
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
     * Updates all UI fields.
     */
    private void updateUI() {
        setButtonsEnabledState();
        updateLocationUI();
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

}
