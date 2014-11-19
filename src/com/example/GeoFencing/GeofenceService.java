package com.example.GeoFencing;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

import java.util.List;

/**
 * Created by moulikadevineni on 11/19/14.
 */
public class GeofenceService extends IntentService {

    //public ReceiveGeofenceTransitionIntentService() {

    public GeofenceService() {

        super("ReceiveGeofenceTransitionsIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        // Create a local broadcast Intent
        Intent broadcastIntent = new Intent();

        // Give it the category for all intents sent by the Intent Service
        broadcastIntent.addCategory(GeoFenceUtils.CATEGORY_LOCATION_SERVICES);


        // First check for errors
        if (LocationClient.hasError(intent)) {
            // Get the error code with a static method
            int errorCode = LocationClient.getErrorCode(intent);
        }
        else {
            // Get the type of transition (entry or exit)
            int transition =
                    LocationClient.getGeofenceTransition(intent);

            if ((transition == Geofence.GEOFENCE_TRANSITION_ENTER) || (transition == Geofence.GEOFENCE_TRANSITION_EXIT)) {
                broadcastIntent.setAction(GeoFenceUtils.ACTION_GEOFENCE_TRANSITION);
                // Broadcast the error *locally* to other components in this app
                LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
                // Post a notification
                List<Geofence> geofences = LocationClient.getTriggeringGeofences(intent);
                String[] geofenceIds = new String[geofences.size()];
                for (int index = 0; index < geofences.size() ; index++) {
                    geofenceIds[index] = geofences.get(index).getRequestId();
                }
                String ids = TextUtils.join(GeoFenceUtils.GEOFENCE_ID_DELIMITER, geofenceIds);
                String transitionType = getTransitionString(transition);

                sendNotification(transitionType, ids);
            }
            else {
                // handle the error
            }
        }
    }

    private void sendNotification(String transitionType, String ids) {

        // Create an explicit content Intent that starts the main Activity
        Intent notificationIntent =
                new Intent(getApplicationContext(),Maps.class);

        // Construct a task stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Adds the main Activity to the task stack as the parent
        stackBuilder.addParentStack(Maps.class);

        // Push the content Intent onto the stack
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        // Set the notification contents
        builder.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(
                        getString(R.string.geofence_transition_notification_title,
                                transitionType, ids))
                .setContentText(getString(R.string.geofence_transition_notification_text))
                .setContentIntent(notificationPendingIntent);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }
    private String getTransitionString(int transitionType) {
        switch (transitionType) {

            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return getString(R.string.geofence_transition_entered);

            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return getString(R.string.geofence_transition_exited);

            default:
                return getString(R.string.geofence_transition_unknown);
        }
    }

}
