package ru.zont.mvc;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;

import ru.zont.mvc.core.Client;
import ru.zont.mvc.core.Request;

class AsyncGetImages extends AsyncTask<Void, Void, String[]> {
    private String query;
    private int rtcount;
    private int offset;

    private OnPostExec onPostExec;
    private Exception exception;

    static void execute(String query, int rtcount, OnPostExec onPostExec) {
        new AsyncGetImages(query, rtcount, onPostExec).execute();
    }

    static void execute(String query, int rtcount, int offset, OnPostExec onPostExec) {
        new AsyncGetImages(query, rtcount, offset, onPostExec).execute();
    }

    private AsyncGetImages(String query, int rtcount, OnPostExec onPostExec) {
        this(query, rtcount, 0, onPostExec);
    }

    private AsyncGetImages(String query, int rtcount, int offset, OnPostExec onPostExec) {
        this.query = query;
        this.rtcount = rtcount;
        this.offset = offset;
        this.onPostExec = onPostExec;
    }

    @Override
    protected String[] doInBackground(Void... voids) {
        String[] result = null;
        try {
            String responseStr = Client.sendJsonForResult(
                    Request.create("reqimg")
                            .put("query", query)
                            .put("rtcount", rtcount)
                            .put("offset", offset)
                            .toString(), rtcount < 10 ? 60000 : rtcount * 20000);
            HashMap<Object, Object>[] metadata = new Gson().fromJson(responseStr, Response.class).metadata;
            result = new String[metadata.length];
            for (int i = 0; i < result.length; i++)
                result[i] = (String) metadata[i].get("image_link");
        } catch (IOException e) {
            e.printStackTrace();
            exception = e;
        }
        return result;
    }

    @Override
    protected void onPostExecute(String[] strings) {
        if (onPostExec != null)
            onPostExec.onPostExec(query, strings, exception);
    }

    @SuppressWarnings({"unused", "MismatchedReadAndWriteOfArray"})
    private static class Response {
        private String response_code;
        private HashMap<Object, Object>[] metadata;
    }

    interface OnPostExec {
        void onPostExec(@NonNull String query, @Nullable String[] urls, @Nullable Exception e);
    }
}
