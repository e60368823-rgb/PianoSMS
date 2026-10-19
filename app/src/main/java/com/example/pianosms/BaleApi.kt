package com.example.pianosms

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object BaleApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .writeTimeout(18, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun isConfigured(context: Context): Boolean =
        context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            .getString(Constants.KEY_BALE_TOKEN, "").orEmpty().isNotBlank() &&
            Constants.DEFAULT_BALE_CHAT_ID.isNotBlank()

    fun sendMessage(
        context: Context,
        text: String,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val prefs = context.applicationContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(Constants.KEY_BALE_TOKEN, "").orEmpty().trim()
        val chatId = Constants.DEFAULT_BALE_CHAT_ID

        if (token.isBlank() || chatId.isBlank()) {
            onResult(false, "اول توکن ربات بله را در تنظیمات وارد کن")
            return
        }

        val payload = JSONObject()
            .put("chat_id", chatId)
            .put("text", text)
            .toString()
            .toRequestBody(jsonType)

        val request = Request.Builder()
            .url("https://tapi.bale.ai/bot$token/sendMessage")
            .post(payload)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onResult(false, e.message ?: "اتصال اینترنت برقرار نشد")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use { res ->
                    val body = res.body?.string().orEmpty()
                    val parsed = try { JSONObject(body) } catch (_: Exception) { null }
                    val apiOk = parsed?.optBoolean("ok", res.isSuccessful) ?: res.isSuccessful
                    val success = res.isSuccessful && apiOk
                    val description = parsed?.optString("description")?.takeIf { it.isNotBlank() }
                    onResult(
                        success,
                        if (success) null else description ?: "ارسال انجام نشد؛ اتصال بله را بررسی کن"
                    )
                }
            }
        })
    }
}
