package com.kevinwang.simpleplayer.bean;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

public class MusicJSONSerializer {
    public static final String MUSIC_JSON_SERIALIZER = "musicJSONSerializer";
    private Context mContext;
    private String mFileName;

    public MusicJSONSerializer(String mFileName, Context mContext) {
        this.mFileName = mFileName;
        this.mContext = mContext;
    }

    public void saveMusic(ArrayList<MusicItem> musics) throws JSONException,IOException {
        JSONArray array = new JSONArray();  //获取一个JSONArray对象
        for (MusicItem music : musics) {
            array.put(music.toJSON());  //将musicItem数据转换成Jason数据然后添加到JSONArray中
        }

        Writer writer = null;
        try {  //保存Jason数据到文件中
            OutputStream out = mContext.openFileOutput(mFileName,Context.MODE_PRIVATE);
            //Open a private file associated with this Context's application package for writing. Creates the file if it doesn't already exist.
            writer = new OutputStreamWriter(out);
            writer.write(array.toString());
        }finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public ArrayList<MusicItem> loadMusic() throws IOException,JSONException {
        ArrayList<MusicItem> musics = new ArrayList<MusicItem>();
        BufferedReader reader = null;
        try {
            InputStream in = mContext.openFileInput(mFileName);
            reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder jsonString = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }

            //通过JSONTokener的nextValue()来获得JSONObject对象，然后再通过JSONObject对象来做进一步的解析。
            JSONArray array = (JSONArray)new JSONTokener(jsonString.toString()).nextValue();
            for (int i = 0; i < array.length(); i++) {
                musics.add(new MusicItem(array.getJSONObject(i)));
            }
        }catch (FileNotFoundException e) {
            Log.i(MUSIC_JSON_SERIALIZER,"未找到相关的Jason文件");
        }finally {
            if (reader != null) {
                reader.close();
            }
        }
        return musics;
    }
}
