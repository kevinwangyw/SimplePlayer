package com.kevinwang.simpleplayer.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.kevinwang.simpleplayer.R;

public class NotificationService extends Service {
    public static final String NOTIFICATION_PLAY = "notification_play";
    public static final String NOTIFICATION_PREV = "notification_prev";
    public static final String NOTIFICATION_NEXT = "notification_next";

    public static final int MUSIC_NOTIFICATION_ID = 111;
    public static final String NOTIFICATION_IMG = "notification_img";
    private NotificationReceiver mNotificationReceiver;

    @Override
    public void onCreate() {
        Log.i("NotificationService", "onCreate");
        mNotificationReceiver = new NotificationReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NOTIFICATION_IMG);
        intentFilter.addAction(NOTIFICATION_PLAY);
        intentFilter.addAction(NOTIFICATION_NEXT);
        intentFilter.addAction(NOTIFICATION_PREV);
        registerReceiver(mNotificationReceiver, intentFilter);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("NotificationService", "onStartCommand");

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);

        setClickEvent(remoteViews, 80, R.id.music_img_notification, NOTIFICATION_IMG);
        setClickEvent(remoteViews, 81, R.id.play_btn_notification, NOTIFICATION_PLAY);
        setClickEvent(remoteViews, 82, R.id.next_btn_notification, NOTIFICATION_NEXT);
        setClickEvent(remoteViews, 83, R.id.prev_btn_notification, NOTIFICATION_PREV);

        //NotificationManager notificationManagerCompat = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
        notificationBuilder.setContent(remoteViews);
        notificationBuilder.setSmallIcon(R.drawable.logo);
        android.app.Notification notification = notificationBuilder.build();
        startForeground(MUSIC_NOTIFICATION_ID, notification);

        return START_NOT_STICKY;
    }

    private void setClickEvent(RemoteViews remoteViews, int requestCod, int r_id, String action) {
        Intent tmpIntent = new Intent(action);
        //like context.sendBroadcast
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), requestCod, tmpIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(r_id, pendingIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("NotificationService", "onBind");
        return null;
    }

    @Override
    public void onDestroy() {
        if (mNotificationReceiver != null) {
            unregisterReceiver(mNotificationReceiver);
        }
        super.onDestroy();
    }

    public class NotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case NOTIFICATION_PLAY:

                    break;
                case NOTIFICATION_PREV:

                    break;
                case NOTIFICATION_NEXT:

                    break;
                case NOTIFICATION_IMG:

                    break;
            }


        }
    }
}
