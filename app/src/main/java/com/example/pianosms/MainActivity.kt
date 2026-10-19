package com.example.pianosms

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var tokenInput: EditText
    private lateinit var chatIdInput: EditText
    private lateinit var selectedContactText: TextView
    private lateinit var messageInput: EditText
    private var toneGenerator: ToneGenerator? = null
    private var selectedPhone: String = ""
    private var selectedName: String = ""

    companion object {
        private const val REQUEST_SMS_PERMISSION = 1001
        private const val REQUEST_CONTACTS_PERMISSION = 1002
        private const val PICK_CONTACT = 1003
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusText = findViewById(R.id.statusText)
        tokenInput = findViewById(R.id.tokenInput)
        chatIdInput = findViewById(R.id.chatIdInput)
        selectedContactText = findViewById(R.id.selectedContactText)
        messageInput = findViewById(R.id.messageInput)

        val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        tokenInput.setText(prefs.getString(Constants.KEY_BALE_TOKEN, ""))
        chatIdInput.setText(prefs.getString(Constants.KEY_BALE_CHAT_ID, ""))

        toneGenerator = try { ToneGenerator(AudioManager.STREAM_MUSIC, 70) } catch (_: Exception) { null }
        listOf(R.id.key1, R.id.key2, R.id.key3, R.id.key4, R.id.key5, R.id.key6, R.id.key7).forEachIndexed { index, id ->
            findViewById<Button>(id).setOnClickListener {
                try { toneGenerator?.startTone(ToneGenerator.TONE_DTMF_1 + index, 120) } catch (_: Exception) { }
            }
        }

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            val token = tokenInput.text.toString().trim()
            val chatId = chatIdInput.text.toString().trim()
            if (token.isBlank() || chatId.isBlank()) {
                Toast.makeText(this, "توکن و شناسه چت را وارد کن", Toast.LENGTH_LONG).show()
            } else {
                prefs.edit().putString(Constants.KEY_BALE_TOKEN, token)
                    .putString(Constants.KEY_BALE_CHAT_ID, chatId).apply()
                statusText.text = "تنظیمات روی همین گوشی ذخیره شد"
                Toast.makeText(this, "تنظیمات ذخیره شد", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.testButton).setOnClickListener {
            val token = tokenInput.text.toString().trim()
            val chatId = chatIdInput.text.toString().trim()
            if (token.isBlank() || chatId.isBlank()) {
                statusText.text = "اول توکن و شناسه چت را وارد کن"
                return@setOnClickListener
            }
            prefs.edit().putString(Constants.KEY_BALE_TOKEN, token)
                .putString(Constants.KEY_BALE_CHAT_ID, chatId).apply()
            statusText.text = "در حال ارسال پیام تست…"
            BaleApi.sendMessage(this, "🎹 تست اتصال Piano SMS") { ok, error -> runOnUiThread {
                statusText.text = if (ok) "✅ تست ارسال موفق بود" else "❌ تست ناموفق: ${error ?: "خطای نامشخص"}"
                Toast.makeText(this, statusText.text, Toast.LENGTH_LONG).show()
            } }
        }

        findViewById<Button>(R.id.pickContactButton).setOnClickListener { pickContactWithPermission() }
        findViewById<Button>(R.id.sendContactMessageButton).setOnClickListener { shareMessageToBale() }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
            statusText.text = "آماده • پیامک‌های جدید پس از تنظیم بله ارسال می‌شوند"
        } else {
            statusText.text = "برای شروع، مجوز دریافت پیامک را بده"
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), REQUEST_SMS_PERMISSION)
        }
    }

    private fun pickContactWithPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            openContactPicker()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), REQUEST_CONTACTS_PERMISSION)
        }
    }

    private fun openContactPicker() {
        try {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            startActivityForResult(intent, PICK_CONTACT)
        } catch (_: Exception) {
            Toast.makeText(this, "دفترچه مخاطبین در دسترس نیست", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Deprecated by Android, retained for compatibility with this app")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_CONTACT && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            var cursor: Cursor? = null
            try {
                cursor = contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                    null, null, null
                )
                if (cursor != null && cursor.moveToFirst()) {
                    selectedName = cursor.getString(0) ?: "مخاطب"
                    selectedPhone = cursor.getString(1) ?: ""
                    selectedContactText.text = "مخاطب انتخاب‌شده: $selectedName\n$selectedPhone"
                }
            } catch (_: Exception) {
                Toast.makeText(this, "خواندن اطلاعات مخاطب ممکن نشد", Toast.LENGTH_LONG).show()
            } finally {
                cursor?.close()
            }
        }
    }

    private fun shareMessageToBale() {
        if (selectedPhone.isBlank()) {
            Toast.makeText(this, "اول یک مخاطب انتخاب کن", Toast.LENGTH_LONG).show()
            return
        }
        val message = messageInput.text.toString().trim()
        if (message.isBlank()) {
            Toast.makeText(this, "متن پیام را بنویس", Toast.LENGTH_LONG).show()
            return
        }
        val shareText = "برای $selectedName ($selectedPhone):\n$message"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(sendIntent, "ارسال متن با بله یا برنامهٔ دیگر"))
        statusText.text = "متن آمادهٔ اشتراک‌گذاری است؛ گیرنده را در بله انتخاب و ارسال را تأیید کن"
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_SMS_PERMISSION -> statusText.text =
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    "آماده • پیامک‌های جدید پس از تنظیم بله ارسال می‌شوند"
                else "مجوز پیامک داده نشد"
            REQUEST_CONTACTS_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openContactPicker()
                } else {
                    Toast.makeText(this, "بدون مجوز مخاطبین امکان انتخاب مخاطب نیست", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator?.release()
        toneGenerator = null
    }
}
