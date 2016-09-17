package com.kevinwang.simpleplayer.frag;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kevinwang.simpleplayer.R;
import com.kevinwang.simpleplayer.activity.MainActivity;
import com.kevinwang.simpleplayer.bean.MusicItem;
import com.kevinwang.simpleplayer.bean.MusicLab;
import com.kevinwang.simpleplayer.helper.PlayStateHelper;
import com.kevinwang.simpleplayer.service.NotificationService;
import com.kevinwang.simpleplayer.service.PlayMusicService;
import com.kevinwang.simpleplayer.widget.MusicWidgetProvider;

import java.util.ArrayList;
import java.util.Random;

import info.abdolahi.CircularMusicProgressBar;

public class PlayerFragment extends Fragment implements View.OnClickListener {

    public static final String PLAYER_FRAGMENT = "PlayerFragment";
    public static final String CURRENT_POS = "currentPos";
    public static final String PLAY_MODE = "play_mode";
    public static final int SET_PRE_OR_NEXT = 11;
    public static final int PLAY_BTN_OPERATION = 12;
    public static final int SET_PROGRESS_ONSTART = 13;
    public static final String PLAY_FRAG = "PlayFrag";
    public static final String WHICH_COMPONENT = "whichFrag";
    public static final String PATH = "path";
    public static final String SET_SRC_SUCCESS = "setSrcSuccess";
    public static final int PLAY_BTN_CLICKED = 0;
    public static final int PRE_BTN_CLICKED = 1;
    public static final int NEXT_BTN_CLICKED = 2;
    public static final String WHICH_BTN_CLICKED = "whichBtnClicked";
    private static CircularMusicProgressBar mCircularMusicProgressBar;
    private static ImageView mPre_btn;
    private static ImageView mNext_btn;
    private static ImageView mPlay_btn;
    private static ImageView mCycle_mode_btn;
    private static int[] mode_img_src;
    private static int[] play_state_img;
    private ArrayList<MusicItem> musics;
    private SharedPreferences mSharedPreferences;
    private static TextView mTitle;
    private static TextView mArtist;
    private static Messenger playFragMessenger = null;

    public static PlayerFragment newInstance() {
        PlayerFragment fragment = new PlayerFragment();
        return fragment;
    }

    public PlayerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(PLAYER_FRAGMENT, "onCreate()");
        super.onCreate(savedInstanceState);
        playFragMessenger = new Messenger(new PlayFragHandler(getActivity()));

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        PlayStateHelper.setJustStart(true);
        PlayStateHelper.setCurPos(mSharedPreferences.getInt(CURRENT_POS, -1));
        PlayStateHelper.setMode(mSharedPreferences.getInt(PLAY_MODE, 0));     //3种模式 0:列表循环，1:单曲循环，2:随机播放
        PlayStateHelper.setPlayState(0);  //0: 音乐未播放（图标显示play），1：正在播放（图标显示pause）
        Log.e(PLAYER_FRAGMENT, "PlayStateHelper.getCurPos() == " + PlayStateHelper.getCurPos());

        mode_img_src = new int[]{R.mipmap.ic_action_playback_repeat, R.mipmap.ic_action_playback_repeat_1, R.mipmap
                .ic_action_playback_schuffle};
        play_state_img = new int[]{R.mipmap.ic_action_playback_play, R.mipmap.ic_action_playback_pause};
        musics = MusicLab.getsMusicLab(getActivity()).getmMusicItem();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        Log.i(PLAYER_FRAGMENT, "onCreateView");
        View view = inflater.inflate(R.layout.frag_play, container, false);

        initComponent(view);

        return view;
    }

    private void initComponent(View view) {
        mCircularMusicProgressBar = (CircularMusicProgressBar) view.findViewById(R.id.music_progress);

        mTitle = (TextView) view.findViewById(R.id.music_title_text);
        mArtist = (TextView) view.findViewById(R.id.music_artist_text);
        Log.i(PLAYER_FRAGMENT, "in onCreateView method, loading curPos : " + mSharedPreferences.getInt(CURRENT_POS,
                -1));
        Log.i(PLAYER_FRAGMENT, "in onCreateView method, " + musics.size() + " songs in the list");
        int mCurPos = PlayStateHelper.getCurPos();
        if (mCurPos != -1) {  //非安装后第一次使用
            Log.i(PLAYER_FRAGMENT, "in conCreateView method, the mCurPos = " + mCurPos);
            mTitle.setText(musics.get(mCurPos).getName());
            mArtist.setText(musics.get(mCurPos).getSinger());

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(musics.get(mCurPos).getPath());
            byte[] songImg = retriever.getEmbeddedPicture();
            if (songImg != null) {
                mCircularMusicProgressBar.setImageBitmap(BitmapFactory.decodeByteArray(songImg, 0, songImg.length));
            } else {
                mCircularMusicProgressBar.setImageResource(R.drawable.cd01);
            }

        }

        mPre_btn = (ImageView) view.findViewById(R.id.prev_btn);
        mNext_btn = (ImageView) view.findViewById(R.id.next_btn);
        mPlay_btn = (ImageView) view.findViewById(R.id.play_btn);
        mCycle_mode_btn = (ImageView) view.findViewById(R.id.cycle_mode_btn);
        mCycle_mode_btn.setImageResource(mode_img_src[PlayStateHelper.getMode()]);

        mPre_btn.setOnClickListener(this);
        mNext_btn.setOnClickListener(this);
        mPlay_btn.setOnClickListener(this);
        mCycle_mode_btn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Log.i("PlayerFragment", "onClick");
        Messenger musicServiceMessenger = MainActivity.getMusicServiceMessenger();
        Log.i("PlayFrag", "onClick-->musicServiceMessenger == null is " + (musicServiceMessenger == null));

        switch (view.getId()) {
            case R.id.prev_btn:
                setPreOrNext(0, musicServiceMessenger, PRE_BTN_CLICKED);
                break;
            case R.id.next_btn:
                setPreOrNext(1, musicServiceMessenger, NEXT_BTN_CLICKED);
                break;
            case R.id.play_btn:
                play_btn_operation(musicServiceMessenger, PLAY_BTN_CLICKED);
                break;
            case R.id.cycle_mode_btn:
                PlayStateHelper.setMode((PlayStateHelper.getMode() + 1) % 3);
                mCycle_mode_btn.setImageResource(mode_img_src[PlayStateHelper.getMode()]);
                break;
        }
    }

    private void play_btn_operation(Messenger musicServiceMessenger, int whichBtnClicked) {
        if (PlayStateHelper.getCurPos() == -1) {
            Toast.makeText(getActivity(), "请添加音乐", Toast.LENGTH_SHORT).show();
            return;
        }

        int mPlayState = PlayStateHelper.getPlayState();
        mPlayState = (++mPlayState) % 2;
        PlayStateHelper.setPlayState(mPlayState);
        mPlay_btn.setImageResource(play_state_img[mPlayState]);
        Log.i("play_btn_operation", "点击PlayerFragment的播放按钮-->是否刚启动：" + PlayStateHelper.isJustStart());
        Log.e("play_btn_operation", "PlayStateHelper.getPlayState() == " + PlayStateHelper.getPlayState());
        Message msgToService = Message.obtain();
        if (mPlayState == 1) {
            if (PlayStateHelper.isJustStart()) {
                requestSetMusicSrc(musicServiceMessenger, PLAY_BTN_OPERATION, whichBtnClicked);
            } else {
                msgToService.what = PlayMusicService.RESUME_MUSIC;
                Intent intent = new Intent();
                intent.setAction(MusicWidgetProvider.WIDGET_ACTION);
                intent.putExtra("operation", 0); //0:play 1:pre 2:next
                getActivity().sendBroadcast(intent);
                intent = new Intent();
                intent.setAction(NotificationService.UPDATE_NOTIFICATION);
                getActivity().sendBroadcast(intent);
                try {
                    Log.e("play_btn_operation", "向service发送消息-->RESUME_MUSIC");
                    musicServiceMessenger.send(msgToService);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    Log.e("play_btn_operation", "向service发送消息失败");
                }
                PlayStateHelper.isPlaying = true;
            }
        } else {
            msgToService.what = PlayMusicService.PAUSE_MUSIC;
            Intent intent = new Intent();
            intent.setAction(MusicWidgetProvider.WIDGET_ACTION);
            intent.putExtra("operation", 0); //0:play 1:pre 2:next
            getActivity().sendBroadcast(intent);
            intent = new Intent();
            intent.setAction(NotificationService.UPDATE_NOTIFICATION);
            getActivity().sendBroadcast(intent);
            try {
                musicServiceMessenger.send(msgToService);
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e("setPreOrNext", "向service发送消息失败");
            }
            PlayStateHelper.isPlaying = false;
        }
    }

    private void requestSetMusicSrc(Messenger musicServiceMessenger, int whatOperation, int whichBtnClicked) {
        String path = MusicLab.getsMusicLab(getActivity()).getmMusicItem().get(PlayStateHelper.getCurPos()).getPath();

        Message msg = Message.obtain();
        msg.what = PlayMusicService.SET_MUSIC_SRC;
        msg.arg1 = whatOperation;
        Bundle data = new Bundle();
        data.putString(PATH, path);
        data.putInt(WHICH_BTN_CLICKED, whichBtnClicked);
        data.putString(WHICH_COMPONENT, PLAY_FRAG);
        msg.replyTo = playFragMessenger;
        msg.setData(data);
        try {
            Log.e("play_btn_operation", "向service发送消息-->SET_MUSIC_SRC");
            Log.e("play_btn_operation", "向service发送消息-->whichBtnClicked == " + whichBtnClicked);
            musicServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
            Log.e("requestSetMusicSrc", "向service发送消息失败");
        }
    }

    private void setPreOrNext(int preOrNext, Messenger musicServiceMessenger, int whichBtnClicked) {  //0:表示后退, 1:表示前进
        if (PlayStateHelper.getCurPos() == -1) {
            Toast.makeText(getActivity(), "请添加音乐", Toast.LENGTH_SHORT).show();
            return;
        }
        if (preOrNext == 0) {
            if (PlayStateHelper.getCurPos() == 0) {
                PlayStateHelper.setCurPos(musics.size() - 1);
            } else {
                PlayStateHelper.setCurPos(PlayStateHelper.getCurPos() - 1);
            }
            Log.i("pre_music_operation", "curPos is " + PlayStateHelper.getCurPos());
        } else {
            if (PlayStateHelper.getCurPos() == (musics.size() - 1)) {
                PlayStateHelper.setCurPos(0);
            } else {
                PlayStateHelper.setCurPos(PlayStateHelper.getCurPos() + 1);
            }
            Log.i("next_music_operation", "curPos is " + PlayStateHelper.getCurPos());
        }

        mCircularMusicProgressBar.setValue(0);

        requestSetMusicSrc(musicServiceMessenger, SET_PRE_OR_NEXT, whichBtnClicked);
    }

    @Override
    public void onStart() {
        Log.i(PLAYER_FRAGMENT, "onStart");
        super.onStart();
        if (PlayStateHelper.getCurPos() != -1) {
            if (PlayStateHelper.isJustStart()) {
                mCircularMusicProgressBar.setValue(0);
            }
            else {
                Messenger musicServiceMessenger = MainActivity.getMusicServiceMessenger();
                Message msg= Message.obtain();
                msg.what = PlayMusicService.UPDATE_PROGRESS;
                msg.replyTo = playFragMessenger;
                Bundle data = new Bundle();
                data.putString(WHICH_COMPONENT, PLAY_FRAG);
                msg.setData(data);
                try {
                    musicServiceMessenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            mTitle.setText(musics.get(PlayStateHelper.getCurPos()).getName());
            mArtist.setText(musics.get(PlayStateHelper.getCurPos()).getSinger());

            MediaMetadataRetriever Retriever = new MediaMetadataRetriever();
            Retriever.setDataSource(musics.get(PlayStateHelper.getCurPos()).getPath());
            byte[] songImg = Retriever.getEmbeddedPicture();
            if (songImg != null) {
                mCircularMusicProgressBar.setImageBitmap(BitmapFactory.decodeByteArray(songImg, 0,
                        songImg.length));
            } else {
                mCircularMusicProgressBar.setImageResource(R.drawable.cd01);
            }

            mPlay_btn.setImageResource(play_state_img[PlayStateHelper.getPlayState()]);
        }
    }

    @Override
    public void onResume() {
        Log.i(PLAYER_FRAGMENT, "onResume");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.i(PLAYER_FRAGMENT, "onPause");
        super.onPause();
        mSharedPreferences.edit().putInt(CURRENT_POS, PlayStateHelper.getCurPos()).commit();
        mSharedPreferences.edit().putInt(PLAY_MODE, PlayStateHelper.getMode()).commit();
        Log.i(PLAYER_FRAGMENT, "in onPause() method, the curPos from sharedPrefferences is : " + mSharedPreferences
                .getInt(CURRENT_POS, -1));
        MusicLab.getsMusicLab(getActivity()).saveMusic();
    }

    @Override
    public void onStop() {
        Log.i(PLAYER_FRAGMENT, "onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.i(PLAYER_FRAGMENT, "onDestroy");
        super.onDestroy();
    }

    public class PlayReceiver extends BroadcastReceiver {
        public static final String UPDATE_PLAY_FRAG_VIEW = "com.kevinwangyw.simpleplayer.PlayerFragment" +
                ".update_play_frag_view";
        public static final String UPDATE_FROM_WIDGET = "com.kevinwangyw.simpleplayer.PlayerFragment" +
                ".update_from_widget";
        public static final String UPDATE_FROM_NOTIFICATION = "com.kevinwangyw.simpleplayer.PlayerFragment" +
                "update_from_notification";

        @Override
        public void onReceive(Context context, Intent intent) {
            musics = MusicLab.getsMusicLab(context).getmMusicItem();
            if (intent != null) {
                if (TextUtils.equals(intent.getAction(), UPDATE_PLAY_FRAG_VIEW)) {
                    int mCurPos = PlayStateHelper.getCurPos();
                    switch (intent.getIntExtra("playing_state", 0)) {
                        //0表示正在播放，1表示播放完毕, 2表示改变进度条,
                        case 0:
                            Log.i(PLAYER_FRAGMENT, "onReceive case " + 0);
                            Log.i(PLAYER_FRAGMENT, "cur pos in list is " + mCurPos);
                            PlayStateHelper.setPlayState(1);
                            updateFrag(musics, mCurPos);
                            break;
                        case 1:
                            Log.i(PLAYER_FRAGMENT, "onReceive case " + 1);
                            Messenger musicServiceMessenger = MainActivity.getMusicServiceMessenger();
                            int curPos = PlayStateHelper.getCurPos();
                            int size = musics.size();

                            if (size != 0) {
                                switch (PlayStateHelper.getMode()) {  //3种模式 0:列表循环，1:单曲循环，2:随机播放
                                    case 0:
                                        if (++curPos > (size - 1)) {
                                            curPos = 0;
                                        }
                                        break;
                                    case 1:  //位置不变
                                        break;
                                    case 2:
                                        Random random = new Random();
                                        curPos = random.nextInt(size);
                                        break;
                                }
                                PlayStateHelper.setCurPos(curPos);
                                requestSetMusicSrc(musicServiceMessenger, SET_PRE_OR_NEXT, 1);
                            } else {
                                PlayStateHelper.setCurPos(-1);
                            }

                            Log.i(PLAYER_FRAGMENT, "cur pos in list is " + mCurPos);

                            PlayStateHelper.setPlayState(1);  //0: 音乐未播放（图标显示play），1：正在播放（图标显示pause）
                            updateFrag(musics, mCurPos);
                            break;
                        case 2:
                            Log.i(PLAYER_FRAGMENT, "onReceive case " + 2);
                            Log.i(PLAYER_FRAGMENT, "cur pos in list is " + mCurPos);
                            float progress = intent.getFloatExtra("progress", 0);
                            mCircularMusicProgressBar.setValue(progress);
                            break;
                        case 3:
                            Log.i(PLAYER_FRAGMENT, "onReceive case " + 3);
                            updateFrag(musics, mCurPos);
                            break;
                    }
                } else if (TextUtils.equals(intent.getAction(), UPDATE_FROM_WIDGET)) {
                    Intent updateWidgetIntent = new Intent();
                    updateWidgetIntent.setAction(MusicWidgetProvider.WIDGET_ACTION);
                    switch (intent.getIntExtra("widget_operation", 0)) {  //4:next 5:pre 6:play
                        case 4:
                            Log.i("PlayReceiver", "onReceive, case next");
                            //setPreOrNext(1, MainActivity.getMusicServiceMessenger(), NEXT_BTN_CLICKED);
                            break;
                        case 5:
                            Log.i("PlayReceiver", "onReceive, case pre");
                            //setPreOrNext(0, MainActivity.getMusicServiceMessenger(), PRE_BTN_CLICKED);
                            break;
                        case 6:
                            Log.i("PlayReceiver", "onReceive, case play");
                            mPlay_btn.setImageResource(play_state_img[PlayStateHelper.getPlayState()]);
                            break;
                    }
                } else if (TextUtils.equals(intent.getAction(), UPDATE_FROM_NOTIFICATION)) {
                    switch (intent.getIntExtra("notification_operation", 0)) {  //4:next 5:pre 6:play
                        case 4:
                            Log.i("PlayReceiver", "onReceive, case next");
                            updateFrag(musics, PlayStateHelper.getCurPos());
                            break;
                        case 5:
                            Log.i("PlayReceiver", "onReceive, case pre");
                            updateFrag(musics, PlayStateHelper.getCurPos());
                            break;
                        case 6:
                            Log.i("PlayReceiver", "onReceive, case play");
                            mPlay_btn.setImageResource(play_state_img[PlayStateHelper.getPlayState()]);
                            break;
                    }
                }
            }
        }

        private void updateFrag(ArrayList<MusicItem> container, int mCurPos) {  //点击音乐文件列表时更新播放界面
            if (mCurPos != -1) {
                Log.i("updateFrag", "current music pos in list is " + mCurPos);
                mTitle.setText(container.get(mCurPos).getName());
                mArtist.setText(container.get(mCurPos).getSinger());

                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(container.get(mCurPos).getPath());
                byte[] songImg = retriever.getEmbeddedPicture();
                if (songImg != null) {
                    mCircularMusicProgressBar.setImageBitmap(BitmapFactory.decodeByteArray(songImg, 0, songImg.length));
                } else {
                    mCircularMusicProgressBar.setImageResource(R.drawable.cd01);
                }
            } else {
                mTitle.setText("");
                mArtist.setText("");
                mCircularMusicProgressBar.setImageResource(R.drawable.cd01);
            }

            mPlay_btn.setImageResource(play_state_img[PlayStateHelper.getPlayState()]);
        }
    }

    private class PlayFragHandler extends Handler {
        private Context mContext;

        PlayFragHandler(Context context) {
            mContext = context;
        }

        @Override
        public void handleMessage(Message msg) {
            ArrayList<MusicItem> musics = MusicLab.getsMusicLab(mContext).getmMusicItem();
            Bundle data = msg.getData();
            Messenger musicServiceMessenger = MainActivity.getMusicServiceMessenger();
            Boolean setSrcSuccess = data.getBoolean(SET_SRC_SUCCESS);
            switch (msg.what) {
                case SET_PRE_OR_NEXT:
                    Log.i("PlayFragHandler", "case SET_PRE_OR_NEXT");
                    if (setSrcSuccess) {

                        PlayStateHelper.setJustStart(false);
                        mTitle.setText(musics.get(PlayStateHelper.getCurPos()).getName());
                        mArtist.setText(musics.get(PlayStateHelper.getCurPos()).getSinger());

                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        retriever.setDataSource(musics.get(PlayStateHelper.getCurPos()).getPath());
                        byte[] songImg = retriever.getEmbeddedPicture();
                        if (songImg != null) {
                            mCircularMusicProgressBar.setImageBitmap(BitmapFactory.decodeByteArray(songImg, 0, songImg.length));
                        } else {
                            mCircularMusicProgressBar.setImageResource(R.drawable.cd01);
                        }

                        sendBroadCastToWidget(data);
                        updateNotification();

                        startMusic(musicServiceMessenger);

                        PlayStateHelper.setPlayState(1);
                        mPlay_btn.setImageResource(play_state_img[PlayStateHelper.getPlayState()]);
                    } else {
                        Toast.makeText(getActivity(), "音乐不存在", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case PLAY_BTN_OPERATION:
                    Log.i("PlayFragHandler", "case PLAY_BTN_OPERATION");
                    if (setSrcSuccess) {
                        Log.i("点击播放按钮", "初始化音乐路径完成");
                    } else {
                        Toast.makeText(getActivity(), "音乐不存在", Toast.LENGTH_SHORT).show();
                    }

                    sendBroadCastToWidget(data);
                    updateNotification();

                    startMusic(musicServiceMessenger);
                    PlayStateHelper.setJustStart(false);
                    break;
                case SET_PROGRESS_ONSTART:
                    int songCurPos = data.getInt(PlayMusicService.SONG_CUR_POS);
                    int songLength = data.getInt(PlayMusicService.SONG_LENGTH);
                    mCircularMusicProgressBar.setValue(100 * (float) songCurPos /
                            (float) songLength);
                    break;
            }
        }

        private void sendBroadCastToWidget(Bundle data) {
            Log.e("PlayFragHandler", "sendBroadCastToWidget-->WHICH_BTN_CLICKED == " + data.getInt(WHICH_BTN_CLICKED));
            Intent intent = new Intent();
            intent.setAction(MusicWidgetProvider.WIDGET_ACTION);
            intent.putExtra("operation", data.getInt(WHICH_BTN_CLICKED)); //0:play 1:pre 2:next
            mContext.sendBroadcast(intent);
        }

        private void updateNotification() {
            Intent intent = new Intent();
            intent.setAction(NotificationService.UPDATE_NOTIFICATION);
            mContext.sendBroadcast(intent);
        }

        private void startMusic(Messenger musicServiceMessenger) {
            Log.i("PlayFragHandler", "case PLAY_BTN_OPERATION--->startMusic, musicServiceMessenger == null is " + (musicServiceMessenger == null));
            Message msgToService = Message.obtain();
            msgToService.what = PlayMusicService.START_MUSIC;
            PlayStateHelper.isPlaying = true;
            try {
                musicServiceMessenger.send(msgToService);
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e("setPreOrNext", "向service发送消息失败");
            }
        }
    }
}
