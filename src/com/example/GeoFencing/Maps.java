package com.example.GeoFencing;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.*;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.google.android.gms.maps.MapFragment;
import com.example.GeoFencing.GeoFenceUtils.REMOVE_TYPE;
import com.example.GeoFencing.GeoFenceUtils.REQUEST_TYPE;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Arrays;

public class Maps extends Activity implements
        LocationClient.OnAddGeofencesResultListener,
        LocationClient.OnRemoveGeofencesResultListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {
    /**
     * Called when the activity is first created.
     */

    private GoogleMap mMap;
    private LocationClient mLocationClient;
    private Geofence mGeoFence;
    //private static final LatLng GREAT_MALL = new LatLng(37.4171078,-121.9008198);
    private static final LatLng SAMPLE_LOCATION = new LatLng(37.518259,-121.991471);
    private final static int mgeofenceRadius = 150;

    IntentFilter  mIntentFilter;
    private ParkingGeofenceReceiver mGeofenceBroadcastReceiver;
    private REQUEST_TYPE mRequestType;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private boolean mRequestInProgress;
    private PendingIntent mGeofencePendingIntent;
    private String mGeofenceState;
    private ArrayList<Geofence> mCurrentGeofencesList;

    public static final String CAN_START_GEOFENCE =
            "com.example.android.geofence.CAN_START";

    public static final String CAN_REGISTER_GEOFENCE =
            "com.example.android.geofence.CAN_REGISTER";

    public static final String GEOFENCE_REGISTERED =
            "com.example.android.geofence.REGISTERED";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mLocationClient = new LocationClient(this, this, this);

        // Create a new broadcast receiver to receive updates from the listeners and service
       // ParkingGeofenceReceiver mGeofenceBroadcastReceiver = new ParkingGeofenceReceiver();
        mGeofenceBroadcastReceiver = new ParkingGeofenceReceiver();


        // Create an intent filter for the broadcast receiver
        mIntentFilter = new IntentFilter();

        // Action for broadcast Intents that report successful addition of geofences
        mIntentFilter.addAction(GeoFenceUtils.ACTION_GEOFENCES_ADDED);

        // Action for broadcast Intents that report successful removal of geofences
        mIntentFilter.addAction(GeoFenceUtils.ACTION_GEOFENCES_REMOVED);

        // Action for broadcast Intents containing various types of geofencing errors
        mIntentFilter.addAction(GeoFenceUtils.ACTION_GEOFENCE_ERROR);

        mIntentFilter.addAction(GeoFenceUtils.ACTION_GEOFENCE_TRANSITION);

        // All Location Services sample apps use this category
        mIntentFilter.addCategory(GeoFenceUtils.CATEGORY_LOCATION_SERVICES);

        // Register the broadcast receiver to receive status updates
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mGeofenceBroadcastReceiver, mIntentFilter);

        mGeofenceState = new String();
        mGeofenceState = CAN_START_GEOFENCE;

       mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
       mMap.setMyLocationEnabled(true);
        /*
        Location curLocation = mMap.getMyLocation();
        LatLng curLatLng = new LatLng(curLocation.getLatitude(),
                curLocation.getLongitude());
        */
        CameraPosition curPosition = new CameraPosition.Builder()
                .target(SAMPLE_LOCATION).zoom(17).build();
        mMap.animateCamera(
                CameraUpdateFactory.newCameraPosition(curPosition));

        mMap.addMarker(new MarkerOptions()
                .position(SAMPLE_LOCATION)
                .title("Geo-Fence Center")
                .snippet("Great mall parking as Center")
                .icon(BitmapDescriptorFactory
                        .fromResource(R.drawable.ic_launcher)));

        CircleOptions circleOptions = new CircleOptions()
                .center(new LatLng(SAMPLE_LOCATION.latitude, SAMPLE_LOCATION.longitude))
                .radius(mgeofenceRadius)
                .fillColor(0x40ff0000)
                .strokeColor(Color.TRANSPARENT)
                .strokeWidth(2);
       mMap.addCircle(circleOptions);

        createAndRegisterGeofence();
    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    public void createAndRegisterGeofence() {

        mGeoFence = new Geofence.Builder()
                    .setRequestId("1")
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .setCircularRegion(
                            SAMPLE_LOCATION.latitude, SAMPLE_LOCATION.longitude,mgeofenceRadius)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .build();
        mCurrentGeofencesList = new ArrayList<Geofence>();
        mCurrentGeofencesList.add(mGeoFence);
        if (mGeofenceState == CAN_REGISTER_GEOFENCE || mGeofenceState == CAN_START_GEOFENCE) {
            registerGeofence();
            mGeofenceState = GEOFENCE_REGISTERED;
        }
            else {
                unregisterGeofence();
            mGeofenceState = CAN_REGISTER_GEOFENCE;
            }
    }

    private boolean checkGooglePlayServices() {
        int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (result == ConnectionResult.SUCCESS) {
            return true;
        }
        else {
            Dialog errDialog = GooglePlayServicesUtil.getErrorDialog(
                    result,
                    this,
                    0);

            if (errDialog != null) {
                errDialog.show();
            }
        }
        return false;
    }


    public void registerGeofence() {

        if (!checkGooglePlayServices()) {

            return;

        }
        mRequestType = REQUEST_TYPE.ADD;

        try {
            // Try to add geofences
            requestConnectToLocationServices();
        } catch (UnsupportedOperationException e) {
            // handle the exception
        }

    }

    public void unregisterGeofence() {

        if (!checkGooglePlayServices()) {
            return;
        }

        // Record the type of removal
        mRequestType = REQUEST_TYPE.REMOVE;

        // Try to make a removal request
        try {
            mGeofencePendingIntent = getRequestPendingIntent();
            requestDisconnectToLocationServices();

        } catch (UnsupportedOperationException e) {
            // handle the exception
        }
    }

    public void requestConnectToLocationServices () throws UnsupportedOperationException {
        // If a request is not already in progress
        if (!mRequestInProgress) {

            mRequestInProgress = true;

            locationClient().connect();
        }
        else {
            // Throw an exception and stop the request
            throw new UnsupportedOperationException();
        }
    }


    /**
     * Get a location client and disconnect from Location Services
     */
    private void requestDisconnectToLocationServices() {

        // A request is no longer in progress
        mRequestInProgress = false;

        locationClient().disconnect();

        if (mRequestType == REQUEST_TYPE.REMOVE) {
            mGeofencePendingIntent.cancel();
        }

    }

    /**
     * returns A LocationClient object
     */
    private GooglePlayServicesClient locationClient() {
        if (mLocationClient == null) {

            mLocationClient = new LocationClient(this, this, this);
        }
        return mLocationClient;

    }

    /*
  Called back from the Location Services when the request to connect the client finishes successfully. At this point, you can
request the current location or start periodic updates
  */
    @Override
    public void onConnected(Bundle bundle) {
        if (mRequestType == REQUEST_TYPE.ADD) {
            // Create a PendingIntent for Location Services to send when a geofence transition occurs
            mGeofencePendingIntent = createRequestPendingIntent();
            // Send a request to add the current geofences
            mLocationClient.addGeofences(mCurrentGeofencesList, mGeofencePendingIntent, this);

        }
        else if (mRequestType == REQUEST_TYPE.REMOVE){

            mLocationClient.removeGeofences(mGeofencePendingIntent, this);
        }
    }

    @Override
    public void onDisconnected() {
        mRequestInProgress = false;
        mLocationClient = null;
    }

    /*
         * Handle the result of adding the geofences
         */
    @Override
    public void onAddGeofencesResult(int statusCode, String[] geofenceRequestIds) {

        // Create a broadcast Intent that notifies other components of success or failure
        Intent broadcastIntent = new Intent();

        // Temp storage for messages
        String msg;

        // If adding the geocodes was successful
        if (LocationStatusCodes.SUCCESS == statusCode) {

            // Create a message containing all the geofence IDs added.
            msg = getString(R.string.add_geofences_result_success,
                    Arrays.toString(geofenceRequestIds));

            // Create an Intent to broadcast to the app
            broadcastIntent.setAction(GeoFenceUtils.ACTION_GEOFENCES_ADDED)
                    .addCategory(GeoFenceUtils.CATEGORY_LOCATION_SERVICES)
                    .putExtra(GeoFenceUtils.EXTRA_GEOFENCE_STATUS, msg);
            // If adding the geofences failed
        } else {
            msg = getString(
                    R.string.add_geofences_result_failure,
                    statusCode,
                    Arrays.toString(geofenceRequestIds)
            );
            broadcastIntent.setAction(GeoFenceUtils.ACTION_GEOFENCE_ERROR)
                    .addCategory(GeoFenceUtils.CATEGORY_LOCATION_SERVICES)
                    .putExtra(GeoFenceUtils.EXTRA_GEOFENCE_STATUS, msg);
        }

        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(broadcastIntent);

        // request to disconnect the location client
        requestDisconnectToLocationServices();
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mRequestInProgress = false;
        if (connectionResult.hasResolution()) {

            try {
                connectionResult.startResolutionForResult(this,CONNECTION_FAILURE_RESOLUTION_REQUEST);
            }

            catch (IntentSender.SendIntentException e) {
                // log the error
            }
        }
        else {
            Intent errorBroadcastIntent = new Intent(GeoFenceUtils.ACTION_CONNECTION_ERROR);
            errorBroadcastIntent.addCategory(GeoFenceUtils.CATEGORY_LOCATION_SERVICES)
                    .putExtra(GeoFenceUtils.EXTRA_CONNECTION_ERROR_CODE,
                            connectionResult.getErrorCode());
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(errorBroadcastIntent);
        }
    }

    @Override
    public void onRemoveGeofencesByPendingIntentResult(int statusCode,
                                                       PendingIntent requestIntent) {

        // Create a broadcast Intent that notifies other components of success or failure
        Intent broadcastIntent = new Intent();

        // If removing the geofences was successful
        if (statusCode == LocationStatusCodes.SUCCESS) {

            // Set the action and add the result message
            broadcastIntent.setAction(GeoFenceUtils.ACTION_GEOFENCES_REMOVED);
            broadcastIntent.putExtra(GeoFenceUtils.EXTRA_GEOFENCE_STATUS,
                    getString(R.string.remove_geofences_intent_success));

        }
        else {
            // removing the geocodes failed


            // Set the action and add the result message
            broadcastIntent.setAction(GeoFenceUtils.ACTION_GEOFENCE_ERROR);
            broadcastIntent.putExtra(GeoFenceUtils.EXTRA_GEOFENCE_STATUS,
                    getString(R.string.remove_geofences_intent_failure,
                            statusCode));
        }
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(broadcastIntent);

        // request to disconnect the location client
        requestDisconnectToLocationServices();
    }

    public class ParkingGeofenceReceiver extends BroadcastReceiver {


        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // Intent contains information about errors in adding or removing geofences
            if (TextUtils.equals(action,GeoFenceUtils. ACTION_GEOFENCE_ERROR)) {
                // handleGeofenceError(context, intent);
                Toast.makeText(context, "Geo Fence Error", Toast.LENGTH_LONG).show();
            }
            else if (TextUtils.equals(action, GeoFenceUtils.ACTION_GEOFENCES_ADDED)) {

                // handleGeofenceStatus(context, intent);
                Toast.makeText(context, "Geo Fence Added", Toast.LENGTH_LONG).show();

            } else if (TextUtils.equals(action, GeoFenceUtils.ACTION_GEOFENCES_REMOVED)) {
                Toast.makeText(context, "Geo Fence Removed", Toast.LENGTH_LONG).show();
            }
            else if (TextUtils.equals(action, GeoFenceUtils.ACTION_GEOFENCE_TRANSITION)) {

                Toast.makeText(context, "Geo Fence Transitioned", Toast.LENGTH_LONG).show();
            }
            else {
                // handle error
            }

        }
    }

    public PendingIntent getRequestPendingIntent() {
        return createRequestPendingIntent();
    }

    private PendingIntent createRequestPendingIntent() {

        if (mGeofencePendingIntent != null) {

            // Return the existing intent
            return mGeofencePendingIntent;

            // If no PendingIntent exists
        } else {

            // Create an Intent pointing to the IntentService
            Intent intent = new Intent(this,
                    GeofenceService.class);

            return PendingIntent.getService(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }


    @Override
    public void onRemoveGeofencesByRequestIdsResult(int statusCode,
                                                    String[] geofenceRequestIds) {

        // it should not come here because we only remove geofences by PendingIntent
        // Disconnect the location client
        requestDisconnectToLocationServices();
    }

}




