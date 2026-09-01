package com.ridelead.app;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.os.Bundle;
import android.app.PendingIntent;
import android.content.Intent;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import org.json.JSONObject;
import org.json.JSONArray;

public class WhatsAppNotificationService extends NotificationListenerService {

    public static HashMap<String, PendingIntent> intentMap = new HashMap<>();
    
    // Paste your copied key inside the quotes below
    private static final String GEMINI_API_KEY = "GEMINI_KEY_PLACEHOLDER";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();

        if ("com.whatsapp".equals(packageName) || "com.whatsapp.w4b".equals(packageName)) {
            Bundle extras = sbn.getNotification().extras;
            CharSequence textChar = extras.getCharSequence("android.text");
            CharSequence titleChar = extras.getCharSequence("android.title");

            if (textChar != null) {
                String messageText = textChar.toString();
                String senderOrGroup = titleChar != null ? titleChar.toString() : "WhatsApp Lead";
                String leadId = String.valueOf(System.currentTimeMillis());

                PendingIntent contentIntent = sbn.getNotification().contentIntent;
                if (contentIntent != null) {
                    intentMap.put(leadId, contentIntent);
                }

                new Thread(() -> parseWithGemini(messageText, senderOrGroup, leadId)).start();
            }
        }
    }

    private void parseWithGemini(String rawText, String groupName, String leadId) {
        try {
            // Updated to Gemini 2.0 Flash REST endpoint
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + GEMINI_API_KEY;
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String prompt = "You are a ride booking extractor. Analyze this text and respond ONLY with raw JSON: " +
                    "{\"is_lead\": true/false, \"pickup\": \"...\", \"drop\": \"...\", \"km\": \"...\", \"toll\": \"...\", \"parking\": \"...\", \"phone\": \"...\"}. " +
                    "Set is_lead to false if not a booking. Input: " + rawText;

            JSONObject textPart = new JSONObject().put("text", prompt);
            JSONObject parts = new JSONObject().put("parts", new JSONArray().put(textPart));
            JSONObject content = new JSONObject().put("contents", new JSONArray().put(parts));

            OutputStream os = conn.getOutputStream();
            os.write(content.toString().getBytes("UTF-8"));
            os.close();

            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line);
                br.close();

                JSONObject resJson = new JSONObject(response.toString());
                String aiText = resJson.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");

                Intent broadcast = new Intent("com.ridelead.app.NEW_LEAD");
                broadcast.putExtra("lead_id", leadId);
                broadcast.putExtra("group", groupName);
                broadcast.putExtra("raw_json", aiText);
                sendBroadcast(broadcast);
            }
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
