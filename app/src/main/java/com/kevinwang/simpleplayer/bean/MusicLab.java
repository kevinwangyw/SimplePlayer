package com.kevinwang.simpleplayer.bean;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

public class MusicLab {
    public static final String MUSIC_LAB = "MusicLab";
    private ArrayList<MusicItem> mMusicItem;
    private static MusicLab sMusicLab;
    private Context mAppContext;
    private static final String TAG = "musicLab";
    private static final String FILENAME = "musics.json";
    private MusicJSONSerializer mSerializer;

    public MusicLab(Context mAppContext) {
        this.mAppContext = mAppContext;
        mSerializer = new MusicJSONSerializer(FILENAME, mAppContext);
        try {
            mMusicItem = mSerializer.loadMusic();
        }catch (Exception e) {
            mMusicItem = new ArrayList<MusicItem>();
        }
    }

    public static MusicLab getsMusicLab(Context context) {
        if (sMusicLab == null) {
            sMusicLab = new MusicLab(context);
        }
        return sMusicLab;
    }

    public ArrayList<MusicItem> getmMusicItem() {
        return mMusicItem;
    }

    public void addmusic(MusicItem music) {
        mMusicItem.add(music);
    }

    public boolean saveMusic() {
        try {
            mSerializer.saveMusic(mMusicItem);
            Log.i(MUSIC_LAB, "MusicsItem saved to file");
            return true;
        }catch (Exception e) {
            Log.e(MUSIC_LAB, "Error saving musicItem", e);
            return false;
        }
    }

    public void deleteMusicItem(MusicItem musicItem) {
        mMusicItem.remove(musicItem);
    }
}
