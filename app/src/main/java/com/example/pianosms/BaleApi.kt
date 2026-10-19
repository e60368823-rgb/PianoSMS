package com.example.pianosms

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object BaleApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun sendMessage(context: Context, text: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(Constants.KEY_BALE_TOKEN, "").orEmpty()
        val chatId = prefs.getString(Constants.KEY_BALE_CHAT_ID, "").orEmpty()
        if (token.isBlank() || chatId.isBlank()) {
            onResult(false, "اول توکن ربات و شناسه چت را در تنظیمات برنامه ذخیره کن")
            return
        }
        val payload = JSONObject().put("chat_id", chatId).put("text", text)
            .toString().toRequestBody(jsonType)
        val request = Request.Builder()
            .url("https://tapi.bale.ai/bot$token/sendMessage")
            .post(payload).build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                onResult(false, e.message ?: "خطای شبکه")
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string()
                val ok = response.isSuccessful
                response.close()
                onResult(ok, if (ok) null else body ?: "پاسخ ناموفق از بله")
            }
        })
    }
}
