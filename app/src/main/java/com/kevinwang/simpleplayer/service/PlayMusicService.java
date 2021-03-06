package com.kevinwang.simpleplayer.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.kevinwang.simpleplayer.frag.FileChooseFragment;
import com.kevinwang.simpleplayer.frag.PlayerFragment;
import com.kevinwang.simpleplayer.helper.PlayStateHelper;
import com.kevinwang.simpleplayer.widget.MusicWidgetProvider;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class PlayMusicService extends Service {
    public static final int SET_MUSIC_SRC = 0;
    public static final int START_MUSIC = 1;
    public static final int RESUME_MUSIC = 2;
    public static final int PAUSE_MUSIC = 3;
    public static final int STOP_MUSIC = 4;
    public static final int DELETE_WHILE_PLAYING = 5;
    public static final int WIDGET_UPDATE = 6;
    public static final int CUR_SONG_POS = 7;
    public static final int UPDATE_PROGRESS = 8;
    public static final String PLAY_MUSIC_SERVICE = "PlayMusicService";
    public static final String SONG_CUR_POS = "songCurPos";
    public static final String SONG_LENGTH = "songLength";
    private final IBinder mPlayBinder = new PlayBinder();
    private MediaPlayer mMediaPlayer;
    private boolean srcFound;
    private static int mCurPlayPos;
    private PlayerFragment.PlayReceiver mPlayReceiver;
    private Timer mTimer;
    private MusicWidgetProvider mWidgetReceiver;
    private Messenger musciServiceMessenger = new Messenger(new ServiceHandler());
    private Messenger playFragMessenger = null;
    private Messenger fileFragMessenger = null;

    private class ServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            System.out.println("ServiceHandler-->handleMessage");
            Bundle data = msg.getData();
            switch (msg.what) {
                case SET_MUSIC_SRC:
                    Boolean setSrcSuccess = setMusicSrc(data.getString(PlayerFragment.PATH));
                    Log.i("ServiceHandler", "SET_MUSIC_SRC--->setSrcSuccess == " + setSrcSuccess);
                    if (TextUtils.equals(data.getString(PlayerFragment.WHICH_COMPONENT), PlayerFragment.PLAY_FRAG)) {
                        playFragMessenger = msg.replyTo;
                        Bundle bundle = new Bundle();
                        bundle.putBoolean(PlayerFragment.SET_SRC_SUCCESS, setSrcSuccess);
                        bundle.putInt(PlayerFragment.WHICH_BTN_CLICKED, data.getInt(PlayerFragment.WHICH_BTN_CLICKED));
                        Message msgToPlayFrag = Message.obtain();
                        msgToPlayFrag.setData(bundle);
                        if (msg.arg1 == PlayerFragment.SET_PRE_OR_NEXT) {
                            msgToPlayFrag.what = PlayerFragment.SET_PRE_OR_NEXT;
                        } else {
                            msgToPlayFrag.what = PlayerFragment.PLAY_BTN_OPERATION;
                        }
                        try {
                            Log.i("SET_MUSIC_SRC", "发送信息给Fragment");
                            playFragMessenger.send(msgToPlayFrag);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            Log.e("PlayMusicService", "case SET_MUSIC_SRC-->发送消息给PlayerFragment失败");
                        }
                    } else if (TextUtils.equals(data.getString(PlayerFragment.WHICH_COMPONENT), MusicWidgetProvider.WIDGET)){
                        Messenger widgetMessenger = msg.replyTo;
                        startMusic();
                        PlayStateHelper.isPlaying = true;
                        Message msgToWidget = Message.obtain();
                        switch (data.getInt(PlayerFragment.WHICH_BTN_CLICKED)){
                            case PlayerFragment.PLAY_BTN_CLICKED:
                                break;
                            case PlayerFragment.NEXT_BTN_CLICKED:
                                msgToWidget.what =  MusicWidgetProvider.NEXT_BTN;
                                break;
                            case PlayerFragment.PRE_BTN_CLICKED:
                                msgToWidget.what = MusicWidgetProvider.PRE_BTN;
                                break;
                        }
                        try {
                            widgetMessenger.send(msgToWidget);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }else if(TextUtils.equals(data.getString(PlayerFragment.WHICH_COMPONENT), NotificationService.NOTIFICATION)){
                        Messenger notificationMessenger = msg.replyTo;
                        startMusic();
                        PlayStateHelper.isPlaying = true;
                        Message msgToNotification = Message.obtain();
                        switch (data.getInt(PlayerFragment.WHICH_BTN_CLICKED)){
                            case PlayerFragment.PLAY_BTN_CLICKED:
                                break;
                            case PlayerFragment.NEXT_BTN_CLICKED:
                                msgToNotification.what =  NotificationService.HANDLER_NEXT;
                                break;
                            case PlayerFragment.PRE_BTN_CLICKED:
                                msgToNotification.what = NotificationService.HANDLER_PRE;
                                break;
                        }
                        try {
                            notificationMessenger.send(msgToNotification);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    } else {
                        fileFragMessenger = msg.replyTo;
                        Message msgToFileFrag = Message.obtain();
                        Bundle bundle = new Bundle();
                        bundle.putBoolean(PlayerFragment.SET_SRC_SUCCESS, setSrcSuccess);
                        msgToFileFrag.setData(bundle);
                        if (msg.arg1 == FileChooseFragment.RESPONSE1){
                            msgToFileFrag.what = FileChooseFragment.RESPONSE1;
                        } else {
                            msgToFileFrag.what = FileChooseFragment.RESPONSE2;
                        }
                        try {
                            Log.i("SET_MUSIC_SRC", "发送信息给FileChooseFragment, setSrcSuccess == " + setSrcSuccess);
                            fileFragMessenger.send(msgToFileFrag);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            Log.e("PlayMusicService", "case SET_MUSIC_SRC-->发送消息给PlayerFragment失败");
                        }
                    }
                    break;
                case START_MUSIC:
                    Log.i("handleMessage", "case START_MUSIC");
                    startMusic();
                    PlayStateHelper.isPlaying = true;
                    break;
                case RESUME_MUSIC:
                    resumeMusic();
                    PlayStateHelper.isPlaying = true;
                    break;
                case PAUSE_MUSIC:
                    pauseMusic();
                    PlayStateHelper.isPlaying = false;
                    break;
                case STOP_MUSIC:
                    startMusic();
                    PlayStateHelper.isPlaying = false;
                    break;
                case DELETE_WHILE_PLAYING:
                    deleteWhilePlaying();
                    break;
                case CUR_SONG_POS:
                    if(getCurrentPos() == 0) {
                        //程序刚启动，无暂停音乐
                        Bundle bundle = new Bundle();
                        fileFragMessenger = msg.replyTo;
                        Message msgToFileFrag = Message.obtain();
                        msgToFileFrag.what = FileChooseFragment.RESPONSE3;
                        if (setMusicSrc(data.getString(PlayerFragment.PATH))) {
                            bundle.putBoolean(PlayerFragment.SET_SRC_SUCCESS, true);
                        } else {
                            bundle.putBoolean(PlayerFragment.SET_SRC_SUCCESS, false);
                        }
                        msgToFileFrag.setData(bundle);
                        try {
                            fileFragMessenger.send(msgToFileFrag);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case WIDGET_UPDATE:
                    Bundle bundle = new Bundle();
                    Messenger widgetMessenger = msg.replyTo;
                    bundle.putInt(SONG_CUR_POS, getCurrentPos());
                    bundle.putInt(SONG_LENGTH, getSongLength());
                    Message msgToWidget = Message.obtain();
                    msgToWidget.what = MusicWidgetProvider.UPDATE;
                    msgToWidget.setData(bundle);
                    try {
                        widgetMessenger.send(msgToWidget);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case UPDATE_PROGRESS:
                    if (TextUtils.equals(data.getString(PlayerFragment.WHICH_COMPONENT), PlayerFragment.PLAY_FRAG)) {
                        Bundle bundle1 = new Bundle();
                        Messenger playMessenger = msg.replyTo;
                        bundle1.putInt(SONG_CUR_POS, getCurrentPos());
                        bundle1.putInt(SONG_LENGTH, getSongLength());
                        Message msgToPlay = Message.obtain();
                        msgToPlay.what = PlayerFragment.SET_PROGRESS_ONSTART;
                        msgToPlay.setData(bundle1);
                        try {
                            playMessenger.send(msgToPlay);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(PLAY_MUSIC_SERVICE, "onCreate()");
        Log.e(PLAY_MUSIC_SERVICE, "PlayStateHelper.getCurPos() == " + PlayStateHelper.getCurPos());
        mPlayReceiver = PlayerFragment.newInstance().new PlayReceiver();
        mWidgetReceiver = new MusicWidgetProvider();

        mCurPlayPos = 0;
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mTimer.cancel();
                mMediaPlayer.reset();

                Intent intent = new Intent();
                intent.setAction(PlayerFragment.PlayReceiver.UPDATE_PLAY_FRAG_VIEW);
                intent.putExtra("playing_state", 1); //0表示正在播放，1表示播放完毕, 2表示改变进度条
                sendBroadcast(intent);
            }
        });
        srcFound = false;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(PLAY_MUSIC_SERVICE, "onStartCommand()");
        Log.e(PLAY_MUSIC_SERVICE, "PlayStateHelper.getCurPos() == " + PlayStateHelper.getCurPos());
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(PLAY_MUSIC_SERVICE, "onOnBind()");
        Log.e(PLAY_MUSIC_SERVICE, "PlayStateHelper.getCurPos() == " + PlayStateHelper.getCurPos());
        return musciServiceMessenger.getBinder();
    }

    @Override
    public void onDestroy() {
        Log.i(PLAY_MUSIC_SERVICE, "onDestroy()");
        Log.e(PLAY_MUSIC_SERVICE, "PlayStateHelper.getCurPos() == " + PlayStateHelper.getCurPos());
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            mTimer.cancel();
        }
        mMediaPlayer.release();
    }

    public class PlayBinder extends Binder {
        public PlayMusicService getService() {
            return PlayMusicService.this;
        }
    }

    public boolean setMusicSrc(String path) {
        Log.i(PLAY_MUSIC_SERVICE, "setMusicSrc()--> the path is " + path);
        if (mMediaPlayer.isPlaying()) {
            Log.e(PLAY_MUSIC_SERVICE, "setMusicSrc()-->用于当前音乐正在播放，需要重新设置新音乐的情况");
            mTimer.cancel();
        }
        mMediaPlayer.stop();
        mMediaPlayer.reset();  //用release方法不行
        Log.i("After reset", "mMediaPlayer == null return " + String.valueOf(mMediaPlayer == null));
        srcFound = false;
        PlayStateHelper.isPlaying = false;
        try {
            mCurPlayPos = 0;
            mMediaPlayer.setDataSource(path);
            Log.e("setMusicSrc", "Music Data has been set");
            srcFound = true;
            mMediaPlayer.prepare();
            return true;
        } catch (IOException e) {
            Log.e("setMusicSrc", "未找到音乐文件");
            return false;
        }
    }

    public void startMusic() {
        Log.i(PLAY_MUSIC_SERVICE, "startMusic()");
        mMediaPlayer.start();
        mTimer = new Timer();
        mTimer.schedule(new Progress(), 0, 500);
    }


    public void resumeMusic() {
        Log.i(PLAY_MUSIC_SERVICE, "resumeMusic()");
        mMediaPlayer.seekTo(mCurPlayPos);
        mMediaPlayer.start();
        mTimer = new Timer();
        mTimer.schedule(new Progress(), 0, 500);
    }

    public void pauseMusic () {
        Log.i(PLAY_MUSIC_SERVICE, "pauseMusic()--->mMediaPlayer.getCurrentPosition() == " + mMediaPlayer.getCurrentPosition());
        mMediaPlayer.pause();
        mCurPlayPos = mMediaPlayer.getCurrentPosition();
        mTimer.cancel();
    }

    public void stopMusic () {
        Log.i(PLAY_MUSIC_SERVICE, "stopMusic()");

        mMediaPlayer.stop();
        mMediaPlayer.reset();
        mTimer.cancel();

/*        cancelling the timer terminates its thread so you can't use it again.
        The timer doesn't have any built-in methods for pausing. You can cancel the timer
        when you want to "pause" and make a new one when you want to "resume".*/
    }

    public boolean isPlaying() {
        Log.i(PLAY_MUSIC_SERVICE, "isPlaying()");
        return mMediaPlayer.isPlaying();
    }

    public void deleteWhilePlaying() {
        Log.i(PLAY_MUSIC_SERVICE, "deleteWhilePlaying()");
        mMediaPlayer.stop();
        mMediaPlayer.reset();
    }

    public int getCurrentPos() {
        return mMediaPlayer.getCurrentPosition();
    }

    public int getSongLength() {
        Log.e(PLAY_MUSIC_SERVICE, "getLength() method used getDuration");
        return mMediaPlayer.getDuration();
    }

    private class Progress extends TimerTask {
        @Override
        public void run() {
            Log.i(PLAY_MUSIC_SERVICE, "TimerTask used getDuration");
            float progress = 100 * ((float)getCurrentPos() / (float) mMediaPlayer.getDuration());
            Intent intent = new Intent();
            intent.setAction(PlayerFragment.PlayReceiver.UPDATE_PLAY_FRAG_VIEW);
            intent.putExtra("playing_state", 2); //0表示正在播放，1表示播放完毕, 2表示改变进度条
            intent.putExtra("progress", progress);
            sendBroadcast(intent);
            intent = new Intent();
            intent.setAction(MusicWidgetProvider.WIDGET_ACTION);
            intent.putExtra("operation", 3);
            intent.putExtra("widget_curPos", getCurrentPos());
            intent.putExtra("widget_songLength", mMediaPlayer.getDuration());
            Log.i("PlayMusicService", "Progress TimerTask-->sendBroadcast to widget");
            sendBroadcast(intent);
        }
    }
}
