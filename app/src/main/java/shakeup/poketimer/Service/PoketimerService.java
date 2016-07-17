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
    public static final int MS_START_TIMER = 30000;
    public static final int MS_TICK_TIMER = 1000;
    Intent bi = new Intent(COUNTDOWN_BR);

    // Shared variables
    CountDownTimer cdt = null;
    android.support.v4.app.NotificationCompat.Builder builder;
    NotificationManager mNotificationManager;

    @Override
    public void onCreate() {
        super.onCreate();

        // Assign global objects
        builder = new NotificationCompat.Builder(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Push to foreground and start notification ticker
        startForeground(NOTIFICATION_ID, tickNotification(MS_START_TIMER));

        // Start timer
        startTimer();
    }

    @Override
    public void onDestroy() {
        cdt.cancel();
        Log.i(TAG, "Timer ended");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startTimer();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private Notification tickNotification(long millisUntilFinished) {
        builder.setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text) + ": " + formatTimer(millisUntilFinished))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setShowWhen(false);

        // Clicking the notification will just open the app
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(this, 0, resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        return builder.build();
    }

    private Notification spinNotification(){
        builder.setContentText(getString(R.string.notification_text_ready));

        return builder.build();
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
