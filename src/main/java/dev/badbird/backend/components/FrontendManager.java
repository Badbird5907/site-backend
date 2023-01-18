package dev.badbird.backend.components;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

@Component
public class FrontendManager {
    private static final Logger logger = Logger.getLogger(FrontendManager.class.getName());
    @Value("${frontendURL}")
    private String frontendURL;

    @Value("${siteKey}")
    private String siteKey;

    public URL getFrontendURL() {
        if (!frontendURL.startsWith("http")) {
            frontendURL = "https://" + frontendURL;
        }
        try {
            return new URL(frontendURL);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @SneakyThrows
    public void revalidateFrontend(String path) {
        if (frontendURL == null || frontendURL.isEmpty() || siteKey == null || siteKey.isEmpty()) {
            logger.warning("Frontend URL or Site Key is not set, skipping revalidation");
            return;
        }
        // post /api/revalidate to frontend, set the 'key' header to siteKey
        URL url = new URL(getFrontendURL() + "/api/revalidate");

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\r\n    \"id\": \"/blog\"\r\n}");
        Request request = new Request.Builder()
                .url(url)
                .method("POST", body)
                .addHeader("key", siteKey)
                .addHeader("Content-Type", "application/json")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                logger.severe("Failed to revalidate frontend page " + path + "!");
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                ResponseBody b = response.body();
                String json = b.string();
                b.close();
                JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                if (!jsonObject.get("success").getAsBoolean()) {
                    logger.severe("Failed to revalidate frontend page " + path + "!");
                    logger.severe("Json: " + json);
                } else {
                    logger.info("Successfully revalidated frontend page " + path + "!");
                }
            }
        });
    }
}
