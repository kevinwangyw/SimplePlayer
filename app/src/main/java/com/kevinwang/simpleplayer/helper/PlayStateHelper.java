package com.kevinwang.simpleplayer.helper;

import android.util.Log;

import java.util.concurrent.TimeUnit;

public class PlayStateHelper {
    private static int curPos;
    private static int mode;
    private static boolean justStart;
    private static int mPlayState; //0: 音乐未播放（图标显示play），1：正在播放（图标显示pause）
    public static boolean isPlaying = false;

    public static boolean isJustStart() {
        return justStart;
    }

    public static void setJustStart(boolean justStart) {
        PlayStateHelper.justStart = justStart;
    }

    public static int getMode() {
        return mode;
    }

    public static void setMode(int mode) {
        PlayStateHelper.mode = mode;
    }

    public static int getCurPos() {
        return curPos;
    }

    public static void setCurPos(int curPos) {
        Log.e("PlayStateHelper", "setCurePos-->parameter curPos == " + curPos);
        PlayStateHelper.curPos = curPos;
    }

    public static int getPlayState() {
        return mPlayState;
    }

    public static void setPlayState(int mPlayState) {
        PlayStateHelper.mPlayState = mPlayState;
    }

    public static String msTohms(int milisecond) {
        if (TimeUnit.MICROSECONDS.toHours(milisecond) == 0) {
            return String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(milisecond),
                    TimeUnit.MILLISECONDS.toSeconds(milisecond) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milisecond)));
        }
        return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(milisecond),
                TimeUnit.MILLISECONDS.toMinutes(milisecond) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milisecond)),
                TimeUnit.MILLISECONDS.toSeconds(milisecond) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milisecond)));
    }
}
