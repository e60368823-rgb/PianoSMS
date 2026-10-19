package com.example.pianosms

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

    fun sendMessage(token: String, chatId: String, text: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (token.isBlank() || chatId.isBlank()) {
            onResult(false, "اول توکن ربات و شناسه چت را در تنظیمات وارد و ذخیره کن")
            return
        }
        val payload = JSONObject().put("chat_id", chatId).put("text", text)
            .toString().toRequestBody(jsonType)
        val request = Request.Builder()
            .url("https://tapi.bale.ai/bot$token/sendMessage")
            .post(payload)
            .build()
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
