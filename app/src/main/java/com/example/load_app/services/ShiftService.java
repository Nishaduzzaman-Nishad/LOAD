package com.example.load_app.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.load_app.R;
import com.example.load_app.activities.RiderDashboardActivity;

public class ShiftService extends Service {

    private static final String CHANNEL_ID = "ShiftChannel";
    private Handler handler;
    private Runnable runnable;
    private int seconds = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start the foreground service immediately
        startForeground(1, buildNotification("00:00:00"));

        handler = new Handler(Looper.getMainLooper());
        runnable = new Runnable() {
            @Override
            public void run() {
                seconds++;
                int h = seconds / 3600;
                int m = (seconds % 3600) / 60;
                int s = seconds % 60;
                String time = String.format("%02d:%02d:%02d", h, m, s);

                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.notify(1, buildNotification(time));
                }

                // Loop every 1 second (1000 ms)
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable);

        return START_STICKY;
    }

    private Notification buildNotification(String time) {
        // This intent opens the Rider Dashboard when the notification is tapped
        Intent notificationIntent = new Intent(this, RiderDashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Shift Active")
                .setContentText("Time elapsed: " + time)
                .setSmallIcon(R.mipmap.ic_launcher) // Using your existing icon
                .setContentIntent(pendingIntent)    // Added: Opens app on tap
                .setOngoing(true)
                .setOnlyAlertOnce(true)             // Stops sound/vibration from spamming every second
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Shift Service Channel", NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}