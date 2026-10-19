package com.example.pianosms

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SmsItem(val address: String, val body: String, val date: Long)

object SmsHelper {
    fun formatSms(sms: SmsItem): String {
        val dateStr = SimpleDateFormat("yyyy/MM/dd - HH:mm:ss", Locale.getDefault()).format(Date(sms.date))
        return "📩 پیامک جدید\n──────────────\n👤 فرستنده: ${sms.address}\n📅 زمان: $dateStr\n──────────────\n${sms.body}"
    }
}
