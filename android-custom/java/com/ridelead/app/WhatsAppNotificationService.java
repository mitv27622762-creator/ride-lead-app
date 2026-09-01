package com.ridelead.app;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.os.Bundle;
import android.app.PendingIntent;
import java.util.HashMap;

public class WhatsAppNotificationService extends NotificationListenerService {

    public static HashMap<String, PendingIntent> intentMap = new HashMap<>();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();

        if ("com.whatsapp".equals(packageName) || "com.whatsapp.w4b".equals(packageName)) {
            Bundle extras = sbn.getNotification().extras;
            CharSequence textChar = extras.getCharSequence("android.text");

            if (textChar != null) {
                String messageText = textChar.toString();
                String leadId = String.valueOf(System.currentTimeMillis());

                PendingIntent contentIntent = sbn.getNotification().contentIntent;
                if (contentIntent != null) {
                    intentMap.put(leadId, contentIntent);
                }

                processIncomingNotification(messageText, leadId);
            }
        }
    }

    private void processIncomingNotification(String text, String leadId) {
        // Gemini API logic will be inserted here
    }
}
