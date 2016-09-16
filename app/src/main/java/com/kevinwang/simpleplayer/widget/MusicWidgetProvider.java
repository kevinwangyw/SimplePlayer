package com.kevinwang.simpleplayer.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.kevinwang.simpleplayer.R;
import com.kevinwang.simpleplayer.activity.MainActivity;
import com.kevinwang.simpleplayer.bean.MusicItem;
import com.kevinwang.simpleplayer.bean.MusicLab;
import com.kevinwang.simpleplayer.frag.PlayerFragment;
import com.kevinwang.simpleplayer.helper.PlayStateHelper;
import com.kevinwang.simpleplayer.service.PlayMusicService;

import java.util.ArrayList;

public class MusicWidgetProvider extends AppWidgetProvider {
    public static final String WIDGET_ACTION = "com.kevinwangyw.simpleplayer.musicWidgetProvider";
    public static final int PLAY = 31;
    public static final String WIDGET = "widget";
    public static final int PRE_OR_NEXT = 32;
    public static final int UPDATE = 33;
    public static final String ACTION_BUTTON_PREV = "prev";
    public static final String ACTION_BUTTON_NEXT = "next";
    public static final String ACTION_BUTTON_PLAY = "play";
    public static final String MUSIC_WIDGET_PROVIDER = "MusicWidgetProvider";
    private Messenger widgetMessenger = new Messenger(new widgetHandler());
    private RemoteViews mRemoteViews;
    private ArrayList<MusicItem> mMusics;

    private void requestSetMusicSrc(Messenger musicServiceMessenger, int whatOperation, String path) {
        Message msg = Message.obtain();
        msg.what = PlayMusicService.SET_MUSIC_SRC;
        msg.arg1 = whatOperation;
        Bundle data = new Bundle();
        data.putString(PlayerFragment.PATH, path);
        data.putString(PlayerFragment.WHICH_COMPONENT, WIDGET);
        msg.replyTo = widgetMessenger;
        msg.setData(data);
        try {
            musicServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
            Log.e("requestSetMusicSrc", "向service发送消息失败");
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(MUSIC_WIDGET_PROVIDER,"==onReceive()==");
        super.onReceive(context, intent);

        Messenger musicServiceMessenger = MainActivity.getMusicServiceMessenger();

        mRemoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        PlayerFragment.PlayReceiver playReceiver = (PlayerFragment.newInstance()).new PlayReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PlayerFragment.PlayReceiver.UPDATE_FROM_WIDGET);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.registerReceiver(playReceiver, intentFilter);

        Intent broadCastIntent = new Intent();
        broadCastIntent.setAction(PlayerFragment.PlayReceiver.UPDATE_FROM_WIDGET);

        if (intent != null) {
            Log.i(MUSIC_WIDGET_PROVIDER,"onReceive()，intent = null : " + (intent == null));
            mMusics = MusicLab.getsMusicLab(context).getmMusicItem();
            if (TextUtils.equals(intent.getAction(), WIDGET_ACTION)) {
                Log.i(MUSIC_WIDGET_PROVIDER, "WIDGET_ACTION");
                int operation = intent.getIntExtra("operation", 0);  //0:play 1:pre 2:nex 3.setProgress
                Log.i(MUSIC_WIDGET_PROVIDER, "operation = " + operation);
                switch (operation) {
                    case 0:
                        updateBtnAndImg(mRemoteViews, mMusics, musicServiceMessenger);
                        if (PlayStateHelper.getPlayState() == 0) {
                            mRemoteViews.setImageViewResource(R.id.widget_play_btn, R.drawable.ic_action_playback_play);
                        }else {
                            mRemoteViews.setImageViewResource(R.id.widget_play_btn, R.drawable.ic_action_playback_pause);
                        }
                        break;
                    case 1:
                        updateBtnAndImg(mRemoteViews, mMusics, musicServiceMessenger);
                        break;
                    case 2:
                        updateBtnAndImg(mRemoteViews, mMusics, musicServiceMessenger);
                        break;
                    case 3:
                        mRemoteViews.setProgressBar(R.id.widget_progress, intent.getIntExtra("widget_songLength", 0), intent.getIntExtra("widget_curPos", 0), false);
                        String progressText = PlayStateHelper.msTohms(intent.getIntExtra("widget_curPos", 0));
                        mRemoteViews.setTextViewText(R.id.widget_curPos, progressText);
                        break;
                }
            }else if (TextUtils.equals(intent.getAction(), ACTION_BUTTON_PLAY)){
                Log.i(MUSIC_WIDGET_PROVIDER, "ACTION_BUTTON_PLAY");
                if (PlayStateHelper.getCurPos() == -1) {
                    return;
                }
                Log.i(MUSIC_WIDGET_PROVIDER,"onReceive()，play button is clicked");
                int mPlayState = PlayStateHelper.getPlayState();
                mPlayState = (++mPlayState) % 2;
                PlayStateHelper.setPlayState(mPlayState);
                if (mPlayState == 1) {
                    if (PlayStateHelper.isJustStart()) {
                        String path = MusicLab.getsMusicLab(context).getmMusicItem().get(PlayStateHelper.getCurPos()).getPath();
                        requestSetMusicSrc(musicServiceMessenger, PLAY, path);
                        Log.e(MUSIC_WIDGET_PROVIDER,"onReceive()，just start, StartMusic after play button was called");
                        PlayStateHelper.setJustStart(false);
                    } else {
                        Log.e(MUSIC_WIDGET_PROVIDER,"onReceive()，resumeMusic after play button was called");
                        Message msgToService = Message.obtain();
                        msgToService.what = PlayMusicService.RESUME_MUSIC;
                        try {
                            musicServiceMessenger.send(msgToService);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            Log.e("setPreOrNext", "向service发送消息失败");
                        }
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
                }

                if (PlayStateHelper.getPlayState() == 0) {
                    mRemoteViews.setImageViewResource(R.id.widget_play_btn, R.drawable.ic_action_playback_play);
                }else {
                    mRemoteViews.setImageViewResource(R.id.widget_play_btn, R.drawable.ic_action_playback_pause);
                }

                broadCastIntent.putExtra("widget_operation", 6);
                localBroadcastManager.sendBroadcast(broadCastIntent);
            }else if (TextUtils.equals(intent.getAction(), ACTION_BUTTON_NEXT)) {
                Log.i(MUSIC_WIDGET_PROVIDER, "ACTION_BUTTON_NEXT");
                if (PlayStateHelper.getCurPos() == -1) {
                    return;
                }
                int pos = PlayStateHelper.getCurPos();
                Log.e(MUSIC_WIDGET_PROVIDER,"onReceive()，next button is clicked");
                Log.e(MUSIC_WIDGET_PROVIDER,"current position is " + PlayStateHelper.getCurPos());
                Log.e(MUSIC_WIDGET_PROVIDER,"next broadCast has been sent");
                if (PlayStateHelper.getCurPos() == (mMusics.size() - 1)) {
                    pos = 0;
                }else {
                    pos++;
                }
                if (getPreOrNextSong(context, mRemoteViews, mMusics, pos, musicServiceMessenger))
                {
                    return;
                }

                broadCastIntent.putExtra("widget_operation", 4);
                localBroadcastManager.sendBroadcast(broadCastIntent);
            }else if (TextUtils.equals(intent.getAction(), ACTION_BUTTON_PREV)) {
                Log.i(MUSIC_WIDGET_PROVIDER, "ACTION_BUTTON_PREV");
                if (PlayStateHelper.getCurPos() == -1) {
                    return;
                }
                int pos = PlayStateHelper.getCurPos();
                Log.e(MUSIC_WIDGET_PROVIDER,"onReceive()，prev button is clicked");
                Log.e(MUSIC_WIDGET_PROVIDER,"current position is " + PlayStateHelper.getCurPos());
                Log.e(MUSIC_WIDGET_PROVIDER,"pre broadCast has been sent");
                if (PlayStateHelper.getCurPos() == 0) {
                    pos = mMusics.size() - 1;
                }else {
                    pos--;
                }
                if (getPreOrNextSong(context, mRemoteViews, mMusics, pos, musicServiceMessenger))
                {
                    return;
                }

                broadCastIntent.putExtra("widget_operation", 5);
                localBroadcastManager.sendBroadcast(broadCastIntent);
            }
        }

        //获得appwidget管理实例，用于管理appwidget以便进行更新操作
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        //获得所有本程序创建的appwidget
        ComponentName componentName = new ComponentName(context,MusicWidgetProvider.class);
        //更新appwidget
        appWidgetManager.updateAppWidget(componentName, mRemoteViews);
    }

    private boolean getPreOrNextSong(Context context, RemoteViews remoteViews, ArrayList<MusicItem> musics, int pos, Messenger musicServiceMessenger) {
        PlayStateHelper.setCurPos(pos);
        String path = musics.get(PlayStateHelper.getCurPos()).getPath();
        requestSetMusicSrc(musicServiceMessenger, PRE_OR_NEXT, path);
        PlayStateHelper.setJustStart(false);
        PlayStateHelper.setPlayState(1);
        updateBtnAndImg(remoteViews, musics, musicServiceMessenger);
        return false;
    }

    private void updateBtnAndImg(RemoteViews remoteViews, ArrayList<MusicItem> musics, Messenger musicServiceMessenger) {
        if (musicServiceMessenger == null) {
            Log.i("updateBtnAndImg", "musicServiceMessenger == null is " + (musicServiceMessenger == null));
        }
        if (PlayStateHelper.getCurPos() != -1) {
            Log.i("updateBtnAndImg", "musicServiceMessenger.send-->PlayMusicService.WIDGET_UPDATE");
            Message msg = Message.obtain();
            msg.what = PlayMusicService.WIDGET_UPDATE;
            msg.replyTo = widgetMessenger;
            try {
                musicServiceMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        } else {
            remoteViews.setProgressBar(R.id.widget_progress, 100, 0, false);

            remoteViews.setTextViewText(R.id.widget_artist, "");
            remoteViews.setTextViewText(R.id.widget_title, "");
            remoteViews.setTextViewText(R.id.widget_song_length, "00:00");
            remoteViews.setTextViewText(R.id.widget_curPos, "00:00");
            remoteViews.setImageViewResource(R.id.widget_song_img, R.drawable.cd01);
        }

        Log.i("updateBtnAndImg", String.valueOf(PlayStateHelper.getPlayState()));

        if (PlayStateHelper.getPlayState() == 1) {
            remoteViews.setImageViewResource(R.id.widget_play_btn, R.drawable.ic_action_playback_pause);
        }else {
            remoteViews.setImageViewResource(R.id.widget_play_btn, R.drawable.ic_action_playback_play);
        }
    }

    private void setSongImg(RemoteViews remoteViews, ArrayList<MusicItem> musics, int position) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(musics.get(position).getPath());
        byte[] songImg = retriever.getEmbeddedPicture();
        if (songImg != null) {
            remoteViews.setImageViewBitmap(R.id.widget_song_img, BitmapFactory.decodeByteArray(songImg, 0, songImg.length));
        }else {
            remoteViews.setImageViewResource(R.id.widget_song_img, R.drawable.cd01);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.i(MUSIC_WIDGET_PROVIDER,"==onUpdate()==");
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        Intent intent = new Intent(context, MusicWidgetProvider.class);
        intent.setAction(ACTION_BUTTON_PLAY);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        remoteViews.setOnClickPendingIntent(R.id.widget_play_btn, pendingIntent);

        intent = new Intent(context, MusicWidgetProvider.class);
        intent.setAction(ACTION_BUTTON_NEXT);
        pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        remoteViews.setOnClickPendingIntent(R.id.widget_next_btn, pendingIntent);

        intent = new Intent(context, MusicWidgetProvider.class);
        intent.setAction(ACTION_BUTTON_PREV);
        pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        remoteViews.setOnClickPendingIntent(R.id.widget_prev_btn, pendingIntent);

        //更新appwidget
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        //删除一个AppWidget时调用
        Log.i(MUSIC_WIDGET_PROVIDER,"==onDelete()==");
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        //AppWidget的实例第一次被创建时调用
        Log.i(MUSIC_WIDGET_PROVIDER,"==onEnabled()==");
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        //删除一个AppWidget时调用
        Log.i(MUSIC_WIDGET_PROVIDER,"==onDisabled()==");
        super.onDisabled(context);
    }


    private class widgetHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            switch (msg.what) {
                case PLAY:

                    break;
                case PRE_OR_NEXT:

                    break;
                case UPDATE:
                    int curPos = data.getInt(PlayMusicService.SONG_CUR_POS);
                    int songLength = data.getInt(PlayMusicService.SONG_LENGTH);
                    String progressText = PlayStateHelper.msTohms(curPos);

                    mRemoteViews.setProgressBar(R.id.widget_progress, songLength, curPos, false);

                    mRemoteViews.setTextViewText(R.id.widget_artist, mMusics.get(PlayStateHelper.getCurPos()).getSinger());
                    mRemoteViews.setTextViewText(R.id.widget_title, mMusics.get(PlayStateHelper.getCurPos()).getName());
                    mRemoteViews.setTextViewText(R.id.widget_song_length, PlayStateHelper.msTohms(songLength));
                    mRemoteViews.setTextViewText(R.id.widget_curPos, progressText);

                    setSongImg(mRemoteViews, mMusics, PlayStateHelper.getCurPos());
                    break;
            }
        }
    }
}
