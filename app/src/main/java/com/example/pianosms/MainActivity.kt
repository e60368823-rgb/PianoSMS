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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var tokenInput: EditText
    private lateinit var chatIdInput: EditText
    private lateinit var selectedContactText: TextView
    private lateinit var messageInput: EditText
    private lateinit var autoForwardSwitch: SwitchCompat
    private var toneGenerator: ToneGenerator? = null
    private var selectedPhone = ""
    private var selectedName = ""

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
        autoForwardSwitch = findViewById(R.id.autoForwardSwitch)

        val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        tokenInput.setText(prefs.getString(Constants.KEY_BALE_TOKEN, ""))
        chatIdInput.setText(prefs.getString(Constants.KEY_BALE_CHAT_ID, ""))
        autoForwardSwitch.isChecked = prefs.getBoolean(Constants.KEY_AUTO_FORWARD, false)

        toneGenerator = try { ToneGenerator(AudioManager.STREAM_MUSIC, 65) } catch (_: Exception) { null }
        listOf(R.id.key1, R.id.key2, R.id.key3, R.id.key4, R.id.key5, R.id.key6, R.id.key7)
            .forEachIndexed { index, id ->
                findViewById<Button>(id).setOnClickListener {
                    try { toneGenerator?.startTone(ToneGenerator.TONE_DTMF_1 + index, 110) } catch (_: Exception) { }
                }
            }

        findViewById<Button>(R.id.saveButton).setOnClickListener { saveSettings(showToast = true) }
        findViewById<Button>(R.id.testButton).setOnClickListener { testBaleConnection() }
        findViewById<Button>(R.id.pickContactButton).setOnClickListener { pickContactWithPermission() }
        findViewById<Button>(R.id.sendContactMessageButton).setOnClickListener { shareMessage() }
        autoForwardSwitch.setOnCheckedChangeListener { _, enabled ->
            if (enabled) {
                val activateForwarding = {
                    if (!hasSmsPermission()) {
                        autoForwardSwitch.isChecked = false
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), REQUEST_SMS_PERMISSION)
                    } else if (saveSettings(showToast = false)) {
                        prefs.edit().putBoolean(Constants.KEY_AUTO_FORWARD, true).apply()
                        statusText.text = "آماده"
                    } else {
                        autoForwardSwitch.isChecked = false
                    }
                }
                if (prefs.getBoolean(Constants.KEY_FORWARD_CONSENT, false)) {
                    activateForwarding()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("فعال‌سازی ارسال خودکار")
                        .setMessage("اگر فعالش کنی، متن پیامک‌های جدید به چت بله‌ای که تنظیم کرده‌ای ارسال می‌شود. ممکن است پیامک‌ها اطلاعات خصوصی داشته باشند.")
                        .setNegativeButton("لغو") { _, _ -> autoForwardSwitch.isChecked = false }
                        .setPositiveButton("متوجه‌ام، ادامه") { _, _ ->
                            prefs.edit().putBoolean(Constants.KEY_FORWARD_CONSENT, true).apply()
                            activateForwarding()
                        }
                        .setOnCancelListener { autoForwardSwitch.isChecked = false }
                        .show()
                }
            } else {
                prefs.edit().putBoolean(Constants.KEY_AUTO_FORWARD, false).apply()
                statusText.text = "آماده"
            }
        }

        statusText.text = "آماده"
        // Ask for SMS permission only when the user explicitly enables the feature.
    }

    private fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

    private fun saveSettings(showToast: Boolean): Boolean {
        val token = tokenInput.text.toString().trim()
        val chatId = chatIdInput.text.toString().trim()
        if (token.isBlank() || chatId.isBlank()) {
            statusText.text = "برای ذخیره تنظیمات بله، توکن و شناسه چت را وارد کن"
            if (showToast) Toast.makeText(this, "توکن و شناسه چت را وارد کن", Toast.LENGTH_LONG).show()
            return false
        }
        getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(Constants.KEY_BALE_TOKEN, token)
            .putString(Constants.KEY_BALE_CHAT_ID, chatId)
            .apply()
        if (showToast) Toast.makeText(this, "تنظیمات روی همین گوشی ذخیره شد", Toast.LENGTH_SHORT).show()
        statusText.text = "تنظیمات بله ذخیره شد"
        return true
    }

    private fun testBaleConnection() {
        if (!saveSettings(showToast = false)) return
        statusText.text = "در حال بررسی اتصال به بله…"
        findViewById<Button>(R.id.testButton).isEnabled = false
        BaleApi.sendMessage(this, "🎹 پیام آزمایشی Piano SMS") { ok, error ->
            runOnUiThread {
                findViewById<Button>(R.id.testButton).isEnabled = true
                statusText.text = if (ok) "اتصال موفق بود؛ پیام آزمایشی ارسال شد" else "ارسال ناموفق: ${error ?: "خطای نامشخص"}"
                Toast.makeText(this, statusText.text, Toast.LENGTH_LONG).show()
            }
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
            startActivityForResult(
                Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI),
                PICK_CONTACT
            )
        } catch (_: Exception) {
            Toast.makeText(this, "برنامهٔ مخاطبین در دسترس نیست", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Retained for compatibility with the current project setup")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_CONTACT || resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                uri,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null
            )
            if (cursor != null && cursor.moveToFirst()) {
                selectedName = cursor.getString(0).orEmpty().ifBlank { "مخاطب" }
                selectedPhone = cursor.getString(1).orEmpty()
                selectedContactText.text = "مخاطب انتخاب‌شده\n$selectedName\n$selectedPhone"
            } else {
                Toast.makeText(this, "شماره‌ای برای این مخاطب پیدا نشد", Toast.LENGTH_LONG).show()
            }
        } catch (_: Exception) {
            Toast.makeText(this, "خواندن اطلاعات مخاطب ممکن نشد", Toast.LENGTH_LONG).show()
        } finally {
            cursor?.close()
        }
    }

    private fun shareMessage() {
        if (selectedPhone.isBlank()) {
            Toast.makeText(this, "اول یک مخاطب انتخاب کن", Toast.LENGTH_LONG).show()
            return
        }
        val message = messageInput.text.toString().trim()
        if (message.isBlank()) {
            messageInput.error = "متن پیام را بنویس"
            messageInput.requestFocus()
            return
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        startActivity(Intent.createChooser(sendIntent, "ارسال پیام برای $selectedName"))
        statusText.text = "متن آماده است؛ در بله گفت‌وگوی $selectedName را انتخاب و ارسال را تأیید کن"
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_SMS_PERMISSION -> {
                val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    autoForwardSwitch.isChecked = false
                    statusText.text = "مجوز پیامک داده نشد؛ ارسال خودکار خاموش است"
                } else {
                    statusText.text = "مجوز پیامک داده شد؛ ارسال خودکار را فقط در صورت نیاز روشن کن"
                }
            }
            REQUEST_CONTACTS_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) openContactPicker()
                else Toast.makeText(this, "برای انتخاب مخاطب باید مجوز را بدهی", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator?.release()
        toneGenerator = null
    }
}
