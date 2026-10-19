package com.example.pianosms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parts.isEmpty()) return
        val address = parts.firstOrNull()?.originatingAddress ?: "نامشخص"
        val body = parts.joinToString(separator = "") { it.messageBody ?: "" }
        val timestamp = parts.maxOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()
        val pending = goAsync()
        BaleApi.sendMessage(context, SmsHelper.formatSms(SmsItem(address, body, timestamp))) { success, error ->
            if (success) Log.i("PianoSMS", "New SMS forwarded")
            else Log.e("PianoSMS", "Forward failed: $error")
            pending.finish()
        }
    }
}
