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

public class DeepseekApi {

    private static final String BASE_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client;
    private final String apiKey;

    public DeepseekApi(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public void getChatCompletion(String model, JSONArray messages, boolean stream, Callback callback) throws JSONException {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("model", model);
        jsonBody.put("messages", messages);
        jsonBody.put("stream", stream);

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL)
                .header("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        client.newCall(request).enqueue(callback);
    }

    public void getModels(Callback callback) {
        Request request = new Request.Builder()
                .url("https://api.deepseek.com/v1/models")
                .header("Authorization", "Bearer " + apiKey)
                .get()
                .build();
        client.newCall(request).enqueue(callback);
    }
}
