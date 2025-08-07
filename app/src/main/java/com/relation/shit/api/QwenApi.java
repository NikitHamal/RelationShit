package com.relation.shit.api;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;

public class QwenApi {

    private static final String BASE_URL = "https://chat.qwen.ai/api/v2";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client;

    // Hardcoded values from qwen_thinking_search.txt
    private static final String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjhiYjQ1NjVmLTk3NjUtNDQwNi04OWQ5LTI3NmExMTIxMjBkNiIsImxhc3RfcGFzc3dvcmRfY2hhbmdlIjoxNzUwNjYwODczLCJleHAiOjE3NTU4NDg1NDh9.pb0IybY9tQkriqMUOos72FKtZM3G4p1_aDzwqqh5zX4";
    private static final String COOKIE = "_gcl_au=1.1.1766988768.1752579263; _bl_uid=nXm6qd3q4zpg6tgm02909kvmFUpm; acw_tc=0a03e54317532565292955177e493bd17cb6ab0297793d17257e4afc7bf42b; x-ap=ap-southeast-1; token=" + TOKEN + "; tfstk=g26ZgsaVhdo2fDL9s99qzj0MwtJ9pKzS_tTXmijDfFYgWt92meSq51sfmKy2-dAA1ET6uKSAzka7F8sOXKpgPzw7wJZtXLHiIn0Xxpxv3h4mOM1lXKp0RbHESRSOW-xr1Gv0YpxXDjvDjhm3Yn-yoKYMi2cHqevDnIDmx2xJqhDDmKqFxeKXnEbDskRHJnxt_a_0zhdgx9OWGMnuVCYljekmEV-9sUeJ5xDfIIBvrGVxnxXebCBHdIJMEK5c2sJDLrlvo9LVIsSUJfYGB9IW5ta-GFjCtBX99mZ9o1jCLQ63qX8fw9W26TzI3E55A9RFOgWqkHXCttBYHjAMvH87Yko6Tuw5pVSFyjhv6C-ePkcoMjdMvH87YklxMCyeYUZnZ; isg=BP7-CDNoGikWBk775LCGxejTTxZAP8K5TbnYJKgHacE8S5klEs5CyL4txkkhzbrR; ssxmod_itna=eq0xcDgCGQYDq4e9igDmhxnD3q7u40dGM9Deq7tdGcD8Ox0PGOzojD5DU2Yz2Ak52qqGRmgKD/KQCqDy7xA3DTx+ajQq5nxvqq35mCxteqDPLwwweCngAOnBKmgY8nUTXUZgw0=KqeDIDY=IDAtD0qDi+DDgDA=DjwDD7qe4DxDADB=bFeDLDi3eVQTDtw0=ieGwDAY4BOhwDYEKwGnxwDDS4QTIieDf9DG2DD=IRWRbqCwTDOxgCKe589bS3Th0BR3VRYIjSYq4SgIA5H8D8+lxm9YUqocQdabWwpEGsERk7wUgILQCFBQ/GD+xe7r5l05oQKiAGxgkVuDhi+YiDD; ssxmod_itna2=eq0xcDgCGQYDq4e9igDmhxnD3q7u40dGM9Deq7tdGcD8Ox0PGOzojD5DU2Yz2Ak52qqGRmxeGIDgDn6Pq+Ee03t1Q6TnxtwxGXxT5W12cxqQj6SG+THGZOQ412fzxk4BtN=FjAO01cDxOPy4S2vsrri5BxIH1iD8Bj01z27Wt4g1aEyaODFW2DAq26osz+i53rvxinaO+Si+6/er3aMigjTNVlTQiWMbqOmq4D";
    private static final String BX_UA = "231\u0021E3/3FAmU8Mz+joZDE+3YnMEjUq/YvqY2leOxacSC80vTPuB9lMZY9mRWFzrwLEV0PmcfY4rL2JFHeKTCyUQdACIzKZBlsbAyXt6Iz8TQ7ck9CIZgeOBOVaRK66GQqw/G9pMRBb28c0I6pXxeCmhbzzfCtEk+xPyXnSGpo+LsaU/+OPHDQCrVM2z4ya7TrTguCmR87np6YdSH3DIn3jhgnFcEQHlSogvwTYlxfUid3koX0QD3yk8jHFx4EMK5QlFHH3v+++3+qAS+oPts+DQWqi++68uR+K6y+8ix9NvuqCz++I4A+4mYN/A46wSw+KgU++my9k3+2XgETIrb8AIWYA++78Ej+yzIk464F3uHo+4oSn28DfRCb7bb4RdlvedIjC5f8MUt1jGNx1IaH10EiLcJPTR6LPJWUj+1hA2bQgJ5wThA3dmWf7dsh4bWmR1rcU3OV14ljhHOENSBKjzoqihnuxql9adxbf7qHFc6ERi7pfFSMd/92mFibzH2549YNTjfOFvgo+FS1/uN+QpL0WxeXRvcFOwCFuku+u1WTAzJmXLU2obdBrZmsVL+GISL5RDin6H1n6RnV2iLE0SOZlAQT/ccm2CtJ9AhpCquek0adxkY3+TOhSPkW/r2RN+U5SbMBBFWpRqQGE0G8uG8gdRiGM+DhV5nzxB+VDkJpZTnF2C/bS8Lkogquz3Mv9hboXZORvx7WxTEhU3rXpCaVGNHzWIPFXp5shUkyscUlWQq9ZgzkhuFHR8vAwNqWLDCiab6sVoOIP1C9gwo+jAGoxgtAXU0xOWuURnWGG7aemef+Fu4s7FfkGO9kMIal6ScRRKJq+YgiTC6oj6rhJYPEgY9xX+JNv2Cp9TratLC5/7bQCpgO4+BFqW25tBh61NeNVNMS9JTFLysevVVQcfxugYJCGMv6wJ1FYvUgqX/Ag4Y4evHRbWKHp88RhqHXOYNPuBenD1xlAMyNTEOvVCDdCxeDHOzMR7cRSlKUiyGcgA7Kg/Xb9gfN/cu6ve82uefIrQg1b1zfpYgl9lExsVQv6dJPUduyTT3sUwzjlkVPkIxZ0Se5PweURQwVPEAtHYlbPAKjTEmDZ65nvieN96Z/hGl8sTm5YpgeHmDZKK4Qi/4LYK5KIpTEgONMcOqQTWReopT00zJiYw7jcNchb8t9GOTdU0RQLAZnDV8YszRmcd8gSTXrCueqrqdxxmjm1OLnNdSOjczQeyG1h/FRUXgsog9WEp1ggdbuFm3xGcHPcYaA95f6szELKvjRGPEu1gdlUYxBPQ3sWMBE152VWjWNd8SVFUrmWDizlmc0QzlmnzXa2CpNJJMMibqYd3bZ2aOENvhhXgjuRgDv5K46hVP/N2xaM/GYJgPfP1D+JnS7LPhnIUCSoTvrKwabbVOisan8s7AGz1Xse5ocJiEsXhsqSQqTaDNTWLvHxkgQYmOIRuKAeAdyUx0SfwgawTqNMC/mnbGQi/RUKwg69RqfJBYFI3SChkgl9xX9mp+ni1XrPFGSonRl4V4LuUsE7XIs5U4EDAhSJfzh+5KhRk=";
    private static final String BX_UMIDTOKEN = "T2gAJllnldFiRL-u8mC_CoRJu4UkeSmmDyAGdRWHSDDtxGpwCLykAm6gn7JppTggooY=";
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 12; itel A662LM) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Mobile Safari/537.36";

    public QwenApi() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    private Request.Builder createBaseRequest(String url) {
        return new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + TOKEN)
                .header("Cookie", COOKIE)
                .header("User-Agent", USER_AGENT)
                .header("bx-ua", BX_UA)
                .header("bx-umidtoken", BX_UMIDTOKEN)
                .header("Origin", "https://chat.qwen.ai")
                .header("Referer", "https://chat.qwen.ai/")
                .header("source", "h5")
                .header("accept", "application/json")
                .header("content-type", "application/json");
    }

    public void getModels(Callback callback) {
        Request request = createBaseRequest(BASE_URL + "/models")
                .get()
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void newChat(String model, Callback callback) throws JSONException {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("title", "New Chat");
        jsonBody.put("models", new JSONArray().put(model));
        jsonBody.put("chat_mode", "normal");
        jsonBody.put("chat_type", "t2t");
        jsonBody.put("timestamp", System.currentTimeMillis());

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = createBaseRequest(BASE_URL + "/chats/new")
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void getChatCompletion(String chatId, String parentId, String model, JSONArray messages, Callback callback) throws JSONException {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("stream", true);
        jsonBody.put("incremental_output", true);
        jsonBody.put("chat_id", chatId);
        jsonBody.put("model", model);
        jsonBody.put("parent_id", parentId);
        jsonBody.put("messages", messages);
        jsonBody.put("timestamp", System.currentTimeMillis());

        JSONObject featureConfig = new JSONObject();
        featureConfig.put("thinking_enabled", true);
        featureConfig.put("output_schema", "phase");
        featureConfig.put("thinking_budget", 38912);
        jsonBody.put("feature_config", featureConfig);

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = createBaseRequest(BASE_URL + "/chat/completions?chat_id=" + chatId)
                .header("x-accel-buffering", "no")
                .post(body)
                .build();

        client.newCall(request).enqueue(callback);
    }
}
