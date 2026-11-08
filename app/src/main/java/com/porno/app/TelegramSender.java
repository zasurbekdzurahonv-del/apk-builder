package com.porno.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TelegramSender {

    private final String botToken;
    private final String chatId;

    public TelegramSender(String botToken, String chatId) {
        this.botToken = botToken;
        this.chatId = chatId;
    }

    public void sendWithDecryptButton(String text, String callbackPrefix, String dataHash, String siteUrl) throws Exception {
        String urlStr = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        // Строим inline клавиатуру
        StringBuilder keyboardJson = new StringBuilder();
        keyboardJson.append("{\"inline_keyboard\":[[");
        keyboardJson.append("{\"text\":\"🔓 Расшифровать\",\"callback_data\":\"")
                    .append(callbackPrefix).append("_").append(dataHash).append("\"}");
        keyboardJson.append("]");

        if (siteUrl != null && !siteUrl.isEmpty()) {
            keyboardJson.append(",[{\"text\":\"🌐 Открыть сайт\",\"url\":\"")
                        .append(siteUrl).append("\"}]");
        }
        keyboardJson.append("]}");

        String escapedText = escapeJson(text);

        String jsonBody = "{" +
            "\"chat_id\":\"" + chatId + "\"," +
            "\"text\":\"" + escapedText + "\"," +
            "\"parse_mode\":\"Markdown\"," +
            "\"reply_markup\":" + keyboardJson.toString() +
            "}";

        postJson(urlStr, jsonBody);
    }

    public void sendMessage(String text) throws Exception {
        String urlStr = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        String escapedText = escapeJson(text);
        String jsonBody = "{" +
            "\"chat_id\":\"" + chatId + "\"," +
            "\"text\":\"" + escapedText + "\"," +
            "\"parse_mode\":\"Markdown\"" +
            "}";
        postJson(urlStr, jsonBody);
    }

    private void postJson(String urlStr, String jsonBody) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        conn.getResponseCode();
        conn.disconnect();
    }

    private String escapeJson(String text) {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
