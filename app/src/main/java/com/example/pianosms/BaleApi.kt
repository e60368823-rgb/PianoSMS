package com.example.pianosms

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object BaleApi {
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun sendMessage(text: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (Constants.BALE_BOT_TOKEN.startsWith("YOUR_") || Constants.BALE_CHAT_ID.startsWith("YOUR_")) {
            onResult(false, "توکن یا شناسه چت تنظیم نشده")
            return
        }
        val payload = JSONObject().put("chat_id", Constants.BALE_CHAT_ID).put("text", text)
            .toString().toRequestBody(jsonType)
        val request = Request.Builder()
            .url("https://tapi.bale.ai/bot${Constants.BALE_BOT_TOKEN}/sendMessage")
            .post(payload).build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) { onResult(false, e.message) }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val msg = response.body?.string()
                val ok = response.isSuccessful
                response.close()
                onResult(ok, if (ok) null else msg)
            }
        })
    }
}
