package ru.zont.mvc.core;

import android.support.annotation.NonNull;

import com.google.gson.Gson;

import java.util.HashMap;

public class Request {
    private HashMap<String, Object> data = new HashMap<>();

    private Request(String requestCode) {
        data.put("request_code", requestCode);
    }

    public Request put(String key, Object value) {
        data.put(key, value);
        return this;
    }

    @NonNull
    @Override
    public String toString() {
        String json;
        try { json = new Gson().toJson(data); }
        catch (Throwable e) {
            e.printStackTrace();
            return super.toString();
        }
        return json != null ? json : super.toString();
    }

    public static Request create(String requestCode) {
        return new Request(requestCode);
    }
}
