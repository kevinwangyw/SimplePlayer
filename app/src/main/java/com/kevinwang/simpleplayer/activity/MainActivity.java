package com.kevinwang.simpleplayer.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.kevinwang.simpleplayer.R;
import com.kevinwang.simpleplayer.bean.MusicLab;
import com.kevinwang.simpleplayer.frag.FileChooseFragment;
import com.kevinwang.simpleplayer.frag.PlayerFragment;
import com.kevinwang.simpleplayer.helper.PlayStateHelper;
import com.kevinwang.simpleplayer.service.PlayMusicService;
import com.kevinwang.simpleplayer.widget.MusicWidgetProvider;
import com.viewpagerindicator.IconPagerAdapter;
import com.viewpagerindicator.TabPageIndicator;

public class MainActivity extends AppCompatActivity {

    public static final String MAIN_ACTIVITY = "MainActivity";
    private Fragment[] mFragment;
    private FragmentManager mFragmentManager;
    private static final int[] tab_icons = {R.mipmap.ic_action_music_1, R.mipmap.ic_action_folder_open};
    private ViewPager mViewPager;
    private TabPagerAdapter mTabAdapter;
    private PlayMusicService.PlayBinder mPlayBinder;
    private Intent mIntent;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            musciServiceMessenger = new Messenger(iBinder);
            Log.i("onServiceConnected", "musciServiceMessenger == null is " + (musciServiceMessenger == null));
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            musciServiceMessenger = null;
        }
    };
    public static Messenger musciServiceMessenger = null;
    private PlayerFragment.PlayReceiver mPlayReceiver;
    private MusicWidgetProvider mWidgetReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(MAIN_ACTIVITY, "========================");
        Log.i(MAIN_ACTIVITY, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFragment = new Fragment[]{PlayerFragment.newInstance(), FileChooseFragment.newInstance(this)};

        mFragmentManager = getSupportFragmentManager();

        //实例化ViewPager， 然后给ViewPager设置Adapter
        mViewPager = (ViewPager)findViewById(R.id.frag_pager);
        mTabAdapter = new TabPagerAdapter(mFragmentManager);
        mViewPager.setAdapter(mTabAdapter);
        //实例化TabPageIndicator，然后与ViewPager绑在一起（核心步骤）
        TabPageIndicator indicator = (TabPageIndicator)findViewById(R.id.tab_title);
        indicator.setViewPager(mViewPager);

        mPlayReceiver = (PlayerFragment.newInstance()).new PlayReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PlayerFragment.PlayReceiver.UPDATE_PLAY_FRAG_VIEW);
        intentFilter.addAction(PlayerFragment.PlayReceiver.UPDATE_FROM_WIDGET);
        intentFilter.addAction(PlayerFragment.PlayReceiver.UPDATE_FROM_NOTIFICATION);
        registerReceiver(mPlayReceiver, intentFilter);

        mWidgetReceiver = new MusicWidgetProvider();
        intentFilter = new IntentFilter();
        intentFilter.addAction(MusicWidgetProvider.ACTION_BUTTON_PLAY);
        intentFilter.addAction(MusicWidgetProvider.ACTION_BUTTON_PREV);
        intentFilter.addAction(MusicWidgetProvider.ACTION_BUTTON_NEXT);
        intentFilter.addAction(MusicWidgetProvider.WIDGET_ACTION);
        registerReceiver(mWidgetReceiver, intentFilter);

        mIntent = new Intent(this, PlayMusicService.class);
        startService(mIntent);
        bindService(mIntent, mConnection, Context.BIND_AUTO_CREATE);
        Log.i("after bindService", "musciServiceMessenger == null is " + (musciServiceMessenger == null));
    }

    class TabPagerAdapter extends FragmentPagerAdapter implements IconPagerAdapter {
        public TabPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragment[position];
        }

        @Override
        public int getIconResId(int index) {
            return tab_icons[index];
        }

        @Override
        public int getCount() {
            return mFragment.length;
        }
    }

    @Override
    protected void onStart() {
        Log.i(MAIN_ACTIVITY, "onStart");
        super.onStart();

    }

    @Override
    public void onPause() {
        Log.i(MAIN_ACTIVITY, "onPause");
        super.onPause();
        MusicLab.getsMusicLab(this).saveMusic();
    }

    @Override
    public void onStop() {
        Log.i(MAIN_ACTIVITY, "onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.i(MAIN_ACTIVITY, "onDestroy");
        super.onDestroy();

        if (PlayStateHelper.isPlaying) {
            Intent intent = new Intent();  //关闭软件时更新桌面插件
            intent.setAction(MusicWidgetProvider.WIDGET_ACTION);
            intent.putExtra("operation", 3);
            intent.putExtra("widget_curPos", 0);
            intent.putExtra("widget_songLength", 100);

            //mPlayMusicService.stopMusic();
        }
        stopService(mIntent);
        this.unbindService(mConnection);

        unregisterReceiver(mPlayReceiver);
        unregisterReceiver(mWidgetReceiver);
    }

    public static Messenger getMusicServiceMessenger() {
        Log.i("MainActivity", "getMusicServiceMessenger--->musciServiceMessenger == null is " + (musciServiceMessenger == null));
        return musciServiceMessenger;
    }
}
