package com.kevinwang.simpleplayer.frag;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.kevinwang.simpleplayer.R;
import com.kevinwang.simpleplayer.activity.MainActivity;
import com.kevinwang.simpleplayer.bean.MusicItem;
import com.kevinwang.simpleplayer.bean.MusicLab;
import com.kevinwang.simpleplayer.helper.PlayStateHelper;
import com.kevinwang.simpleplayer.service.PlayMusicService;
import com.kevinwang.simpleplayer.widget.MusicWidgetProvider;

import java.io.File;
import java.util.ArrayList;

public class FileChooseFragment extends Fragment {

    public static final String FILE_CHOOSER_FRAGMENT = "FileChooserFragment";
    public static final String CURRENT_POS = "currentPos";
    public static final String FILE_FRAG = "fileFragment";
    public static final int RESPONSE1 = 21;
    public static final int RESPONSE2 = 22;
    public static final int RESPONSE3 = 23;
    private static Context mContext;
    private ImageView add_btn;
    private ListView mListView;
    private static FileChooseFragment sFragment;
    private ArrayList<MusicItem> mMusics;
    private SharedPreferences mSharedPreferences;
    private Messenger fileFragMessenger = new Messenger(new FileFragHandler());

    public FileChooseFragment() {
    }

    public static FileChooseFragment newInstance(Context context) {
        mContext = context;
        sFragment = new FileChooseFragment();
        return sFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(FILE_CHOOSER_FRAGMENT, "onCreate() --> new ServiceConnection");

        mMusics = MusicLab.getsMusicLab(mContext).getmMusicItem();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    private void requestSetMusicSrc(Messenger musicServiceMessenger, int whatOperation) {
        String path = mMusics.get(PlayStateHelper.getCurPos()).getPath();
        Log.e("requestSetMusicSrc", "PlayStateHelper.getCurPos() == " + PlayStateHelper.getCurPos());
        Log.e("requestSetMusicSrc", "the path is " + path);

        Message msg = Message.obtain();
        msg.what = PlayMusicService.SET_MUSIC_SRC;
        msg.arg1 = whatOperation;
        Bundle data = new Bundle();
        data.putString(PlayerFragment.PATH, path);
        data.putString(PlayerFragment.WHICH_COMPONENT, FILE_FRAG);
        msg.replyTo = fileFragMessenger;
        msg.setData(data);
        try {
            musicServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
            Log.e("requestSetMusicSrc", "向service发送消息失败");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(FILE_CHOOSER_FRAGMENT, "onStart");

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                int mCurPos = PlayStateHelper.getCurPos();
                Messenger musicServiceMessenger = MainActivity.getMusicServiceMessenger();
                int pos = i;
                Log.i("ListViewItemClick", "item " + pos + " was clicked");

                if (mCurPos != pos) {
                    mCurPos = pos;
                    PlayStateHelper.setCurPos(mCurPos);
                    //判断音乐文件是否存在
                    requestSetMusicSrc(musicServiceMessenger, RESPONSE1);
                }else {
                    Log.i("onItemClick", "点击的为当前位置");
                    if (!PlayStateHelper.isPlaying) {
                        Log.i("onItemClick", "没有音乐在播放");
                        if (PlayStateHelper.isJustStart()) {
                            requestSetMusicSrc(musicServiceMessenger, RESPONSE1);
                        }else {
                            Message msgToService = Message.obtain();
                            msgToService.what = PlayMusicService.RESUME_MUSIC;
                            try {
                                musicServiceMessenger.send(msgToService);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                                Log.e("onItemClick", "resume music, 向service发送消息失败");
                            }
                            updateWidget();
                            sendMusicPlayState(0);
                        }
                    }
                }

                Log.e("ListViewItemClick", "current position is " + PlayStateHelper.getCurPos());
            }
        });
    }

    private void updateWidget() {
        Intent intent = new Intent();
        intent.setAction(MusicWidgetProvider.WIDGET_ACTION);
        intent.putExtra("operation", 2); //0:play 1:pre 2:next
        mContext.sendBroadcast(intent);
    }

    private void sendMusicPlayState(int state) {
        Intent intent = new Intent();
        intent.setAction(PlayerFragment.PlayReceiver.UPDATE_PLAY_FRAG_VIEW);
        if (state == 2) {
            intent.putExtra("progress", (float)0);
        }
        intent.putExtra("playing_state", state); //0表示正在播放，1表示播放完毕, 2表示改变进度条
        mContext.sendBroadcast(intent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        Log.i(FILE_CHOOSER_FRAGMENT, "onCreatView");

        View view = inflater.inflate(R.layout.frag_file_choose, container, false);


        mListView = (ListView) view.findViewById(R.id.music_list);

        final MusicListAdapter musicListAdapter = new MusicListAdapter();

        mListView.setAdapter(musicListAdapter);

        add_btn = (ImageView) view.findViewById(R.id.add_music_img_btn);
        add_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogProperties properties = new DialogProperties();
                properties.selection_mode = DialogConfigs.MULTI_MODE;
                properties.selection_type = DialogConfigs.FILE_SELECT;
                properties.root = new File(DialogConfigs.DEFAULT_DIR);
                properties.extensions = null;
                FilePickerDialog dialog = new FilePickerDialog(getActivity(), properties);
                dialog.setDialogSelectionListener(new DialogSelectionListener() {
                    @Override
                    public void onSelectedFilePaths(String[] files) {
                        for (String path : files) {
                            File file = new File(path);
                            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
                            metadataRetriever.setDataSource(file.getPath());
                            String name = "", singer = "";
                            int length = 0;
                            try {
                                if((name = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)) == null) {
                                    name = "Unknown";
                                }
                                if((singer = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)) == null) {
                                    singer = "unknown";
                                }
                                length = Integer.valueOf(metadataRetriever.extractMetadata(MediaMetadataRetriever
                                        .METADATA_KEY_DURATION));
                                //放时长单位为毫秒
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            mMusics.add(new MusicItem(name, singer, file.getPath(), length));
                        }
                        musicListAdapter.notifyDataSetChanged();
                    }
                });
                dialog.show();
            }
        });

        return view;
    }

    class MusicListAdapter extends BaseAdapter {
        private LayoutInflater mInflater; //得到一个LayoutInfalter对象用来导入布局
        private ViewHolder viewHolder;

        public MusicListAdapter() {
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mMusics.size();
        }

        @Override
        public Object getItem(int i) {
            return mMusics.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = mInflater.inflate(R.layout.music_list_item, null);
                viewHolder = new ViewHolder();
                viewHolder.delete_btn = (ImageView) view.findViewById(R.id.delete_music_btn);
                viewHolder.musicName = (TextView) view.findViewById(R.id.music_name);
                viewHolder.singer = (TextView) view.findViewById(R.id.singer_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            viewHolder.musicName.setText(mMusics.get(i).getName());
            viewHolder.singer.setText(mMusics.get(i).getSinger());

            final int pos = i;
            viewHolder.delete_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final Messenger musicServiceMessenger = MainActivity.getMusicServiceMessenger();
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
                    String message = (PlayStateHelper.isPlaying && pos == PlayStateHelper.getCurPos()) ? "当前音乐正在播放，确定移除" :
                            "确定要从列表中移除音乐吗？";
                    AlertDialog alertDialog = alertDialogBuilder.setTitle("移除音乐").setMessage(message)
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    int mCurPos = PlayStateHelper.getCurPos();
                                    Log.i("deleteBtnClick", "点击删除音乐位置为：" + pos);
                                    Log.i("deleteBtnClick", "当前系统音乐位置为：" + mCurPos);
                                    //删除时有音乐正在播放
                                    if (PlayStateHelper.isPlaying) {
                                        mMusics.remove(pos);
                                        notifyDataSetChanged();
                                        if (pos == mCurPos) {  //删除当前播放音乐
                                            Log.i("deleteWhilePlaying", "删除当前播放音乐");
                                            Message msgToService = Message.obtain();
                                            msgToService.what = PlayMusicService.DELETE_WHILE_PLAYING;
                                            try {
                                                musicServiceMessenger.send(msgToService);
                                            } catch (RemoteException e) {
                                                e.printStackTrace();
                                                Log.e("deleteWhilePlaying", "向service发送消息失败");
                                            }
                                            //删除之后还有音乐
                                            if (mMusics.size() >= 1) {
                                                //删除当前播放音乐, 保持当前位置。如果删除之后当前位置超出音乐列表，将位置设置为0
                                                if (mCurPos > (mMusics.size() - 1)) {
                                                    mCurPos = 0;
                                                    PlayStateHelper.setCurPos(mCurPos);
                                                }
                                                requestSetMusicSrc(musicServiceMessenger, RESPONSE1);
                                            } else {
                                                //删除之后无音乐
                                                noMusicAfterDelete();
                                            }
                                        }else if (pos < mCurPos) {  //删除非当前播放音乐且位置小于当前播放音乐位置
                                            mCurPos--;
                                            PlayStateHelper.setCurPos(mCurPos);
                                        }
                                    }else {
                                        mMusics.remove(pos);
                                        notifyDataSetChanged();
                                        if (mMusics.size() >= 1) {
                                            if (pos == mCurPos) {
                                                if (mCurPos != 0) {
                                                    //暂停的时候删除当前位置音乐，且当前位置不为0
                                                    //保持当前位置。如果删除之后当前位置超出音乐列表，将位置设置为0
                                                    if (mCurPos > (mMusics.size() - 1)) {
                                                        mCurPos = 0;
                                                    }
                                                    PlayStateHelper.setCurPos(mCurPos);
                                                    Log.i("deleteBtnClick", "删除当前暂停音乐");
                                                    PlayStateHelper.setPlayState(0);
                                                    sendMusicPlayState(2);  //使进度条变为零
                                                    sendMusicPlayState(3);  //因为当前位置改变了，需要改变播放也界面
                                                }else {
                                                    //暂停的时候删除当前位置音乐，且当前位置为0
                                                    PlayStateHelper.setPlayState(0);
                                                    sendMusicPlayState(2);  //使进度条变为零
                                                    sendMusicPlayState(3);  //因为当前位置改变了，需要改变播放也界面
                                                }
                                                requestSetMusicSrc(musicServiceMessenger, RESPONSE2);

                                            } else if (pos < mCurPos) {
                                                //删除非当前位置音乐且位置小于当前音乐位置
                                                mCurPos--;
                                                PlayStateHelper.setCurPos(mCurPos);
                                                //程序刚启动，无暂停音乐
                                                Message msgToService = Message.obtain();
                                                msgToService.what = PlayMusicService.CUR_SONG_POS;
                                                msgToService.replyTo = fileFragMessenger;
                                                Bundle bundle = new Bundle();
                                                bundle.putString(PlayerFragment.PATH, mMusics.get(mCurPos).getPath());
                                                msgToService.setData(bundle);
                                                try {
                                                    musicServiceMessenger.send(msgToService);
                                                } catch (RemoteException e) {
                                                    e.printStackTrace();
                                                    Log.e("setPreOrNext", "向service发送消息失败");
                                                }
                                            }
                                        } else {
                                            //删除之后无音乐
                                            Log.i("deleteBtnClick", "删除之后无音乐");
                                            PlayStateHelper.setPlayState(0);
                                            noMusicAfterDelete();
                                        }
                                    }

                                }

                                private void noMusicAfterDelete() {
                                    int mCurPos;
                                    mCurPos = -1;
                                    PlayStateHelper.setCurPos(mCurPos);
                                    sendMusicPlayState(2);
                                    sendMusicPlayState(3);
                                    updateWidget();
                                }
                            }).setNegativeButton("取消", null).create();
                    alertDialog.show();
                }
            });
            return view;
        }

        class ViewHolder {
            ImageView delete_btn;
            TextView musicName;
            TextView singer;
        }
    }

    @Override
    public void onResume() {
        Log.i(FILE_CHOOSER_FRAGMENT, "onResume");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.i(FILE_CHOOSER_FRAGMENT, "onPause");
        super.onPause();
        mSharedPreferences.edit().putInt(CURRENT_POS, PlayStateHelper.getCurPos()).commit();
        Log.i(FILE_CHOOSER_FRAGMENT, "in onPause() method, the curPos from sharedPrefferences is : " +
                mSharedPreferences.getInt(CURRENT_POS, -1));
        MusicLab.getsMusicLab(mContext).saveMusic();
    }

    @Override
    public void onStop() {
        Log.i(FILE_CHOOSER_FRAGMENT, "onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.i(FILE_CHOOSER_FRAGMENT, "onDestroy");
        super.onDestroy();
//        mContext.unregisterReceiver(mPlayReceiver);
//        mContext.unregisterReceiver(mWidgetReceiver);
    }

    private class FileFragHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            Messenger musicServiceMessenger = MainActivity.getMusicServiceMessenger();
            switch (msg.what){
                case RESPONSE1:
                    Log.i("FileFragHandler", "response1, data.getBoolean(PlayerFragment.SET_SRC_SUCCESS) == " + data.getBoolean(PlayerFragment.SET_SRC_SUCCESS));

                    if (data.getBoolean(PlayerFragment.SET_SRC_SUCCESS)) {
                        startMusic(musicServiceMessenger);
                        PlayStateHelper.setJustStart(false);
                        sendMusicPlayState(0);
                        updateWidget();
                    } else {
                        Toast.makeText(mContext, "音乐不存在", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case RESPONSE2:
                    Log.i("FileFragHandler", "response2");
                    if (data.getBoolean(PlayerFragment.SET_SRC_SUCCESS)) {
                        updateWidget();
                    } else {
                        Toast.makeText(mContext, "音乐不存在", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case RESPONSE3:
                    Log.i("FileFragHandler", "response3");
                    if (data.getBoolean(PlayerFragment.SET_SRC_SUCCESS)) {
                        updateWidget();
                    } else {
                        Toast.makeText(mContext, "音乐不存在", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }

        private void startMusic(Messenger musicServiceMessenger) {
            Log.i("FileFragHandler", "startMusic");
            Message msgToService = Message.obtain();
            msgToService.what = PlayMusicService.START_MUSIC;
            try {
                Log.i("startMusic()", "发送消息给service");
                musicServiceMessenger.send(msgToService);
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e("setPreOrNext", "向service发送消息失败");
            }
        }
    }
}
