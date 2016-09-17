package com.kevinwang.simpleplayer.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.kevinwang.simpleplayer.R;
import com.kevinwang.simpleplayer.activity.MainActivity;
import com.kevinwang.simpleplayer.bean.MusicItem;
import com.kevinwang.simpleplayer.bean.MusicLab;
import com.kevinwang.simpleplayer.frag.PlayerFragment;
import com.kevinwang.simpleplayer.helper.PlayStateHelper;
import com.kevinwang.simpleplayer.widget.MusicWidgetProvider;

import java.util.ArrayList;

public class NotificationService extends Service {
    public static final String NOTIFICATION_PLAY = "notification_play";
    public static final String NOTIFICATION_PREV = "notification_prev";
    public static final String NOTIFICATION_NEXT = "notification_next";
    public static final String NOTIFICATION_RECEIVER = "NotificationReceiver";
    public static final int MUSIC_NOTIFICATION_ID = 111;
    public static final String NOTIFICATION_IMG = "notification_img";
    public static final String UPDATE_NOTIFICATION = "update_notification";
    public static final String NOTIFICATION = "notification";
    public static final int HANDLER_NEXT = 51;
    public static final int HANDLER_PRE = 52;
    private static RemoteViews mRemoteViews;
    private static ArrayList<MusicItem> mMusics;
    private static Notification mNotification;
    private static NotificationManager mNotificationManagerCompat;
    private static Messenger musicServiceMessenger;
    private static Messenger notificationMessenger;
    private static Context mContext;

    @Override
    public void onCreate() {
        Log.i("NotificationService", "onCreate");
//        mNotificationReceiver = new NotificationReceiver();
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(NOTIFICATION_IMG);
//        intentFilter.addAction(NOTIFICATION_PLAY);
//        intentFilter.addAction(NOTIFICATION_NEXT);
//        intentFilter.addAction(NOTIFICATION_PREV);
//        registerReceiver(mNotificationReceiver, intentFilter);
        mMusics = MusicLab.getsMusicLab(getApplicationContext()).getmMusicItem();
        musicServiceMessenger = MainActivity.getMusicServiceMessenger();
        notificationMessenger = new Messenger(new NotificationHandler(getApplicationContext()));
        mContext = getApplicationContext();
        super.onCreate();
    }

    private void setSongImg(RemoteViews remoteViews, ArrayList<MusicItem> musics, int position) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(musics.get(position).getPath());
        byte[] songImg = retriever.getEmbeddedPicture();
        Log.e("setSongImg", "songImg == null return " + (songImg == null));
        if (songImg != null) {
            remoteViews.setImageViewBitmap(R.id.music_img_notification, BitmapFactory.decodeByteArray(songImg, 0, songImg.length));
        }else {
            remoteViews.setImageViewResource(R.id.music_img_notification, R.drawable.cd01);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("NotificationService", "onStartCommand--->PlayStateHelper.getCurPos() == " + PlayStateHelper.getCurPos());

        mRemoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);
        if (PlayStateHelper.getCurPos() != -1){
            mRemoteViews.setTextViewText(R.id.artist_notification, mMusics.get(PlayStateHelper.getCurPos()).getSinger());
            mRemoteViews.setTextViewText(R.id.song_title_notification, mMusics.get(PlayStateHelper.getCurPos()).getName());
            setSongImg(mRemoteViews, mMusics, PlayStateHelper.getCurPos());
        }

        setClickEvent(mRemoteViews, 80, R.id.music_img_notification, NOTIFICATION_IMG);
        setClickEvent(mRemoteViews, 81, R.id.play_btn_notification, NOTIFICATION_PLAY);
        setClickEvent(mRemoteViews, 82, R.id.next_btn_notification, NOTIFICATION_NEXT);
        setClickEvent(mRemoteViews, 83, R.id.prev_btn_notification, NOTIFICATION_PREV);

        mNotificationManagerCompat = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
        //notificationBuilder.setContent(mRemoteViews);
        notificationBuilder.setSmallIcon(R.drawable.logo);
        notificationBuilder.setCustomBigContentView(mRemoteViews);
        mNotification = notificationBuilder.build();
        startForeground(MUSIC_NOTIFICATION_ID, mNotification);

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
        super.onDestroy();
    }

    public class NotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(NOTIFICATION_SERVICE,"onReceive");
            musicServiceMessenger = MainActivity.getMusicServiceMessenger();
            Intent tmpintent = new Intent();
            int pos = PlayStateHelper.getCurPos();
            switch (intent.getAction()) {
                case NOTIFICATION_PLAY:
                    Log.e(NOTIFICATION_SERVICE,"case NOTIFICATION_PLAY");
                    if (PlayStateHelper.getCurPos() == -1) {
                        return;
                    }
                    Log.i(NOTIFICATION_RECEIVER,"onReceive()，play button is clicked");
                    int mPlayState = PlayStateHelper.getPlayState();
                    mPlayState = (++mPlayState) % 2;
                    PlayStateHelper.setPlayState(mPlayState);
                    Log.e(NOTIFICATION_RECEIVER, "playState = " + mPlayState);
                    if (PlayStateHelper.getPlayState() == 0) {
                        mRemoteViews.setImageViewResource(R.id.play_btn_notification, R.mipmap.ic_action_playback_play);
                    }else {
                        mRemoteViews.setImageViewResource(R.id.play_btn_notification, R.mipmap.ic_action_playback_pause);
                    }

                    if (mPlayState == 1) {
                        if (PlayStateHelper.isJustStart()) {
                            String path = MusicLab.getsMusicLab(context).getmMusicItem().get(PlayStateHelper.getCurPos()).getPath();
                            requestSetMusicSrc(musicServiceMessenger, path, PlayerFragment.PLAY_BTN_CLICKED);
                            Log.e(NOTIFICATION_RECEIVER,"onReceive()，just start, StartMusic after play button was called");
                            PlayStateHelper.setJustStart(false);
                        } else {
                            Log.e(NOTIFICATION_RECEIVER,"onReceive()，resumeMusic after play button was called");
                            Message msgToService = Message.obtain();
                            msgToService.what = PlayMusicService.RESUME_MUSIC;
                            try {
                                musicServiceMessenger.send(msgToService);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                                Log.e("setPreOrNext", "向service发送消息失败");
                            }
                            PlayStateHelper.isPlaying = true;
                        }
                    } else {
                        Message msgToService = Message.obtain();
                        msgToService.what = PlayMusicService.PAUSE_MUSIC;
                        try {
                            musicServiceMessenger.send(msgToService);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            Log.e("setPreOrNext", "向service发送消息失败");
                        }
                        PlayStateHelper.isPlaying = false;
                    }
//                    tmpintent.setAction(PlayerFragment.PlayReceiver.UPDATE_FROM_NOTIFICATION);
//                    tmpintent.putExtra("notification_operation", 6); //4:next 5:pre 6:play
//                    mContext.sendBroadcast(tmpintent);
                    tmpintent = new Intent();
                    tmpintent.setAction(MusicWidgetProvider.WIDGET_ACTION);
                    tmpintent.putExtra("operation", 0); //0:play 1:pre 2:next
                    mContext.sendBroadcast(tmpintent);
                    break;
                case NOTIFICATION_PREV:
                    Log.e(NOTIFICATION_SERVICE,"case NOTIFICATION_PREV");
                    if (PlayStateHelper.getCurPos() == -1) {
                        return;
                    }
                    Log.e(NOTIFICATION_SERVICE,"onReceive()，prev button is clicked");
                    Log.e(NOTIFICATION_SERVICE,"current position is " + PlayStateHelper.getCurPos());
                    if (PlayStateHelper.getCurPos() == 0) {
                        pos = mMusics.size() - 1;
                    }else {
                        pos--;
                    }
                    PlayStateHelper.setCurPos(pos);
                    PlayStateHelper.setJustStart(false);
                    PlayStateHelper.setPlayState(1);
                    requestSetMusicSrc(musicServiceMessenger, mMusics.get(PlayStateHelper.getCurPos()).getPath(), PlayerFragment.PRE_BTN_CLICKED);
                    break;
                case NOTIFICATION_NEXT:
                    Log.e(NOTIFICATION_SERVICE,"case NOTIFICATION_NEXT");
                    if (PlayStateHelper.getCurPos() == -1) {
                        return;
                    }
                    Log.e(NOTIFICATION_SERVICE,"onReceive()，next button is clicked");
                    Log.e(NOTIFICATION_SERVICE,"current position is " + PlayStateHelper.getCurPos());
                    if (PlayStateHelper.getCurPos() == (mMusics.size() - 1)) {
                        pos = 0;
                    }else {
                        pos++;
                    }
                    PlayStateHelper.setCurPos(pos);
                    PlayStateHelper.setJustStart(false);
                    PlayStateHelper.setPlayState(1);
                    requestSetMusicSrc(musicServiceMessenger, mMusics.get(PlayStateHelper.getCurPos()).getPath(), PlayerFragment.NEXT_BTN_CLICKED);
                    break;
                case NOTIFICATION_IMG:

                    break;
                case UPDATE_NOTIFICATION:
                    Log.e(NOTIFICATION_SERVICE,"case UPDATE_NOTIFICATION");
                    if (PlayStateHelper.getPlayState() == 0) {
                        mRemoteViews.setImageViewResource(R.id.play_btn_notification, R.mipmap.ic_action_playback_play);
                    }else {
                        mRemoteViews.setImageViewResource(R.id.play_btn_notification, R.mipmap.ic_action_playback_pause);
                    }
                    mRemoteViews.setTextViewText(R.id.artist_notification, mMusics.get(PlayStateHelper.getCurPos()).getSinger());
                    mRemoteViews.setTextViewText(R.id.song_title_notification, mMusics.get(PlayStateHelper.getCurPos()).getName());
                    setSongImg(mRemoteViews, mMusics, PlayStateHelper.getCurPos());
                    break;
            }
            mNotificationManagerCompat.notify(MUSIC_NOTIFICATION_ID, mNotification);
        }
    }

    private class NotificationHandler extends Handler {
        private Context mContext;

        NotificationHandler(Context context) {
            mContext = context;
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            Intent intent;
            switch (msg.what) {
                case HANDLER_NEXT:
                    intent = new Intent();
                    intent.setAction(NotificationService.UPDATE_NOTIFICATION);
                    mContext.sendBroadcast(intent);
                    intent = new Intent();
                    intent.setAction(MusicWidgetProvider.WIDGET_ACTION);
                    intent.putExtra("operation", 2); //0:play 1:pre 2:next
                    mContext.sendBroadcast(intent);
                    intent = new Intent();
                    intent.setAction(PlayerFragment.PlayReceiver.UPDATE_FROM_NOTIFICATION);
                    intent.putExtra("notification_operation", 4); //4:next 5:pre 6:play
                    mContext.sendBroadcast(intent);
                    break;
                case HANDLER_PRE:
                    intent = new Intent();
                    intent.setAction(NotificationService.UPDATE_NOTIFICATION);
                    mContext.sendBroadcast(intent);
                    intent = new Intent();
                    intent.setAction(MusicWidgetProvider.WIDGET_ACTION);
                    intent.putExtra("operation", 1); //0:play 1:pre 2:next
                    mContext.sendBroadcast(intent);
                    intent = new Intent();
                    intent.setAction(PlayerFragment.PlayReceiver.UPDATE_FROM_NOTIFICATION);
                    intent.putExtra("notification_operation", 5); //4:next 5:pre 6:play
                    mContext.sendBroadcast(intent);
                    break;
            }
        }
    }

    private void requestSetMusicSrc(Messenger musicServiceMessenger, String path, int whichBtnClicked) {
        Message msg = Message.obtain();
        msg.what = PlayMusicService.SET_MUSIC_SRC;
        Bundle data = new Bundle();
        data.putString(PlayerFragment.PATH, path);
        data.putInt(PlayerFragment.WHICH_BTN_CLICKED, whichBtnClicked);
        data.putString(PlayerFragment.WHICH_COMPONENT, NOTIFICATION);
        msg.replyTo = notificationMessenger;
        msg.setData(data);
        try {
            musicServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
            Log.e("requestSetMusicSrc", "向service发送消息失败");
        }
    }
}
