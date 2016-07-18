package shakeup.poketimer.Service;

/**
 * Created by Jayson Dela Cruz on 7/17/2016.
 */

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import shakeup.poketimer.MainActivity;
import shakeup.poketimer.R;

public class PoketimerService extends Service {

    // Constants
    private final static String TAG = "PoketimerService";
    public static final String COUNTDOWN_BR = "shakeup.poketimer.countdown_br";
    public static final int NOTIFICATION_ID = 1;
    public static final int MS_START_TIMER = 300000; // 300000 = 5 min
    public static final int MS_TICK_TIMER = 1000;
    public static final int NOTIFICATION_PRIORITY = NotificationCompat.PRIORITY_HIGH;
    public static final String ACTION_RESET_TIMER = "reset timer"; // Request code for resetting timer
    public static final String ACTION_STOP_TIMER = "stop timer"; // Request code for stopping timer and service
    public static final long[] VIBRATE_PATTERN = {0, 250, 250, 250, 250, 200, 100, 200, 100, 400};
    Intent bi = new Intent(COUNTDOWN_BR);

    // Shared variables
    CountDownTimer cdt = null;
    android.support.v4.app.NotificationCompat.Builder mNotificationBuilder;
    NotificationManager mNotificationManager;

    @Override
    public void onCreate() {
        super.onCreate();

        // Assign globals
        mNotificationBuilder = new NotificationCompat.Builder(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Push to foreground and start notification ticker
        startForeground(NOTIFICATION_ID, makeNotification(MS_START_TIMER));

        // Start timer
        startTimer();
    }

    @Override
    public void onDestroy() {
        cdt.cancel();
        mNotificationBuilder = null;
        mNotificationManager = null;
        Log.i(TAG, "Timer ended");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getAction() == null){ // Start new timer
            startTimer();
        }
        else if(intent.getAction().equals(ACTION_RESET_TIMER)){
            resetNotification();
            startTimer();
        }
        else if(intent.getAction().equals(ACTION_STOP_TIMER)){
            stopSelf();
        }
        else{ // Start new timer
            Log.i(TAG, "Intent specified an unknown action");
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private Notification makeNotification(long millisUntilFinished){

        // Build notification for the first time
        mNotificationBuilder.setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text) + ": " + formatTimer(millisUntilFinished))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setShowWhen(false)
                .setPriority(NOTIFICATION_PRIORITY)
                .setAutoCancel(true);

        // Clicking the notification will just open the app
        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent activityPendingIntent =
                PendingIntent.getActivity(this, 0, activityIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        mNotificationBuilder.setContentIntent(activityPendingIntent);

        // Clicking reset will reset the timer
        Intent resetIntent = new Intent(this, PoketimerService.class);
        resetIntent.setAction(ACTION_RESET_TIMER);
        PendingIntent resetPendingIntent =
                PendingIntent.getService(this, 0, resetIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        mNotificationBuilder.addAction(new android.support.v4.app.NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_rotate,
                "Reset",
                resetPendingIntent)
                .build());

        // Clicking stop will stop the entire service and clear the notification
        Intent stopIntent = new Intent(this, PoketimerService.class);
        stopIntent.setAction(ACTION_STOP_TIMER);
        PendingIntent stopPendingIntent =
                PendingIntent.getService(this, 0, stopIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        mNotificationBuilder.addAction(new android.support.v4.app.NotificationCompat.Action.Builder(
                android.R.drawable.ic_delete,
                "Stop",
                stopPendingIntent)
                .build());

        return mNotificationBuilder.build();
    }

    private Notification tickNotification(long millisUntilFinished) {
        mNotificationBuilder.setContentText(getString(R.string.notification_text) + ": " + formatTimer(millisUntilFinished));

        return mNotificationBuilder.build();
    }

    private Notification spinNotification(){
        // Set notification sound URI
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Update notification
        mNotificationBuilder.setContentText(getString(R.string.notification_text_ready))
                .setVibrate(VIBRATE_PATTERN)
                .setSound(uri);

        return mNotificationBuilder.build();
    }

    private void resetNotification(){
        mNotificationBuilder.setVibrate(new long[] {0, 0}); // Turn off vibrate
        mNotificationBuilder.setSound(null);
    }

    private void startTimer(){
        Log.i(TAG, "Starting timer...");

        // Cancel timer if one is already started
        if(cdt != null){
            cdt.cancel();
        }

        // Create new timer. Timer broadcasts current time which can be received by the MainActivity
        cdt = new CountDownTimer(MS_START_TIMER, MS_TICK_TIMER) {
            @Override
            public void onTick(long millisUntilFinished) {

                // Update notification with time remaining
                mNotificationManager.notify(NOTIFICATION_ID, tickNotification(millisUntilFinished));

                // TODO Use time in MainActivity
                Log.i(TAG, "Countdown seconds remaining: " + millisUntilFinished / 1000);
                bi.putExtra("countdown", millisUntilFinished);
                sendBroadcast(bi);
            }

            @Override
            public void onFinish() {
                Log.i(TAG, "Timer finished");
                // Change notification info
                mNotificationManager.notify(NOTIFICATION_ID, spinNotification());
                // Play some notification sound
            }
        };
        cdt.start();
    }

    public String formatTimer(long millis){
        // Format miliseconds to readable time format
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }
}
