package com.kevinwang.simpleplayer.bean;

import org.json.JSONException;
import org.json.JSONObject;

public class MusicItem {
    public static final String JSON_NAME = "name";
    public static final String JSON_PATH = "path";
    public static final String SINGER = "singer";
    public static final String LENGTH = "length";
    private String name, singer, path;
    private int mLength;

    public MusicItem (String name, String singer, String path, int length) {
        this.name = name;
        this.path = path;
        this.singer = singer;
        mLength = length;
    }

    public MusicItem (JSONObject json) throws JSONException {
        name = json.getString(JSON_NAME);
        path = json.getString(JSON_PATH);
        singer = json.getString(SINGER);
        mLength = json.getInt(LENGTH);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSinger() {
        return singer;
    }

    public void setSinger(String singer) {
        this.singer = singer;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(JSON_NAME, name);
        json.put(JSON_PATH, path);
        json.put(SINGER, singer);
        json.put(LENGTH, mLength);
        return json;
    }
}
