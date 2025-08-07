package com.relation.shit.api;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GeminiApi {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client;
    private final String apiKey;

    public GeminiApi(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public void getChatCompletion(String model, JSONArray contents, Callback callback) throws JSONException {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("contents", contents);

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + model + ":generateContent?key=" + apiKey)
                .post(body)
                .build();

        client.newCall(request).enqueue(callback);
    }

    public void getModels(Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "?key=" + apiKey)
                .get()
                .build();
        client.newCall(request).enqueue(callback);
    }
}
