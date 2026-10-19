package com.example.pianosms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(Constants.KEY_AUTO_FORWARD, false)) {
            Log.i("PianoSMS", "SMS received; automatic forwarding is disabled")
            return
        }

        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parts.isEmpty()) return
        val address = parts.firstOrNull()?.originatingAddress ?: "نامشخص"
        val body = parts.joinToString(separator = "") { it.messageBody.orEmpty() }
        val timestamp = parts.maxOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()
        val pending = goAsync()

        BaleApi.sendMessage(context, SmsHelper.formatSms(SmsItem(address, body, timestamp))) { success, error ->
            if (success) Log.i("PianoSMS", "SMS forwarding succeeded")
            else Log.e("PianoSMS", "SMS forwarding failed: $error")
            pending.finish()
        }
    }
}
