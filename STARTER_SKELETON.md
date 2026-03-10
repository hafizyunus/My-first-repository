# Expense Tracker Android Starter Skeleton (SMS-first)

This starter keeps **SMS as the primary transaction source** and includes a **NotificationListener test path**.

## 1) Create project in Android Studio

1. Open Android Studio → **New Project** → **Empty Activity**.
2. Name: `ExpenseTracker`.
3. Package: `com.example.expensetracker`.
4. Language: **Kotlin**.
5. Min SDK: **26+** (recommended).
6. Finish and let Gradle sync.

## 2) Suggested package structure

```text
app/src/main/java/com/example/expensetracker/
  data/
    ExpenseDraft.kt
    ExpenseEntity.kt
    ExpenseDao.kt
    AppDatabase.kt
  parser/
    SmsParser.kt
    GenericUpiSmsParser.kt
  receiver/
    BankSmsReceiver.kt
  service/
    PaymentNotificationListenerService.kt
  ui/
    MainActivity.kt
    QuickAddActivity.kt
  util/
    DraftIntents.kt
```

## 3) Gradle dependencies (`app/build.gradle.kts`)

```kotlin
dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.2")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    testImplementation("junit:junit:4.13.2")
}
```

Also enable kapt plugin in the same file:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}
```

## 4) Manifest setup (`app/src/main/AndroidManifest.xml`)

```xml
<manifest ...>
    <uses-permission android:name="android.permission.RECEIVE_SMS" />
    <uses-permission android:name="android.permission.READ_SMS" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application ...>
        <activity
            android:name=".ui.QuickAddActivity"
            android:theme="@style/Theme.ExpenseTracker.Popup" />

        <receiver
            android:name=".receiver.BankSmsReceiver"
            android:exported="true"
            android:permission="android.permission.BROADCAST_SMS">
            <intent-filter>
                <action android:name="android.provider.Telephony.SMS_RECEIVED" />
            </intent-filter>
        </receiver>

        <service
            android:name=".service.PaymentNotificationListenerService"
            android:label="Expense Tracker Listener"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
            android:exported="false">
            <intent-filter>
                <action android:name="android.service.notification.NotificationListenerService" />
            </intent-filter>
        </service>
    </application>
</manifest>
```

> Production note: SMS permissions are Play-policy sensitive; this setup is intended for local prototyping.

## 5) Core model classes

### `ExpenseDraft.kt`

```kotlin
package com.example.expensetracker.data

data class ExpenseDraft(
    val amount: Double?,
    val timestampMillis: Long,
    val payee: String?,
    val title: String?,
    val category: String? = null,
    val source: String, // "sms" or "notification"
    val rawText: String
)
```

### `ExpenseEntity.kt`

```kotlin
package com.example.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val timestampMillis: Long,
    val title: String,
    val category: String,
    val source: String,
    val rawText: String
)
```

## 6) SMS parser skeleton

### `SmsParser.kt`

```kotlin
package com.example.expensetracker.parser

import com.example.expensetracker.data.ExpenseDraft

interface SmsParser {
    fun parse(message: String, timestampMillis: Long): ExpenseDraft?
}
```

### `GenericUpiSmsParser.kt`

```kotlin
package com.example.expensetracker.parser

import com.example.expensetracker.data.ExpenseDraft

class GenericUpiSmsParser : SmsParser {
    private val amountRegex = Regex("(?:₹|Rs\\.?|INR)\\s?([0-9]+(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
    private val payeeRegex = Regex("(?:to|towards)\\s+([A-Za-z0-9 .&_-]{2,40})", RegexOption.IGNORE_CASE)

    override fun parse(message: String, timestampMillis: Long): ExpenseDraft? {
        val lower = message.lowercase()
        val hasDebitSignal = listOf("debited", "spent", "sent", "upi", "paid").any { it in lower }
        if (!hasDebitSignal) return null

        val amount = amountRegex.find(message)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
        val payee = payeeRegex.find(message)?.groupValues?.getOrNull(1)?.trim()

        return ExpenseDraft(
            amount = amount,
            timestampMillis = timestampMillis,
            payee = payee,
            title = payee ?: "UPI Expense",
            source = "sms",
            rawText = message
        )
    }
}
```

## 7) SMS receiver skeleton

### `BankSmsReceiver.kt`

```kotlin
package com.example.expensetracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import com.example.expensetracker.parser.GenericUpiSmsParser
import com.example.expensetracker.util.DraftIntents

class BankSmsReceiver : BroadcastReceiver() {
    private val parser = GenericUpiSmsParser()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val bundle: Bundle = intent.extras ?: return
        val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val body = smsMessages.joinToString(separator = "") { it.messageBody ?: "" }
        val timestamp = smsMessages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

        val draft = parser.parse(body, timestamp) ?: return
        context.startActivity(DraftIntents.quickAddIntent(context, draft))
    }
}
```

## 8) Quick-add popup screen skeleton

### `DraftIntents.kt`

```kotlin
package com.example.expensetracker.util

import android.content.Context
import android.content.Intent
import com.example.expensetracker.data.ExpenseDraft
import com.example.expensetracker.ui.QuickAddActivity

object DraftIntents {
    fun quickAddIntent(context: Context, draft: ExpenseDraft): Intent {
        return Intent(context, QuickAddActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("amount", draft.amount)
            putExtra("timestamp", draft.timestampMillis)
            putExtra("payee", draft.payee)
            putExtra("title", draft.title)
            putExtra("source", draft.source)
            putExtra("rawText", draft.rawText)
        }
    }
}
```

### `QuickAddActivity.kt`

```kotlin
package com.example.expensetracker.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.expensetracker.R

class QuickAddActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_add)

        val amountInput = findViewById<EditText>(R.id.amountInput)
        val titleInput = findViewById<EditText>(R.id.titleInput)
        val categoryInput = findViewById<EditText>(R.id.categoryInput)
        val saveButton = findViewById<Button>(R.id.saveButton)

        amountInput.setText(intent.getDoubleExtra("amount", 0.0).toString())
        titleInput.setText(intent.getStringExtra("title") ?: "")

        saveButton.setOnClickListener {
            // TODO: Save with Room DAO and finish.
            finish()
        }
    }
}
```

## 9) Notification listener test path

### `PaymentNotificationListenerService.kt`

```kotlin
package com.example.expensetracker.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.expensetracker.parser.GenericUpiSmsParser
import com.example.expensetracker.util.DraftIntents

class PaymentNotificationListenerService : NotificationListenerService() {
    private val parser = GenericUpiSmsParser()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val combined = "$title $text".trim()

        val draft = parser.parse(combined, sbn.postTime) ?: return
        startActivity(DraftIntents.quickAddIntent(this, draft.copy(source = "notification")))
    }
}
```

## 10) Popup layout + theme

Create `res/layout/activity_quick_add.xml` with 3 inputs + save button and a simple vertical layout.

In `res/values/themes.xml`, add:

```xml
<style name="Theme.ExpenseTracker.Popup" parent="Theme.Material3.DayNight.DialogWhenLarge" />
```

## 11) Run and test

1. Install app on physical Android device.
2. Open app once and request runtime permissions for SMS/notifications.
3. Grant notification access manually in system settings for your app.
4. Send a test SMS from another phone with content like:
   - `INR 240.00 debited via UPI to Swiggy Ref 12345`
5. Verify QuickAdd popup opens with prefilled amount/title.
6. Optional: post a test notification from your own app and verify listener route.

## 12) Debugging checklist

- Receiver not firing: confirm SMS permission granted and app installed as debug build.
- No popup: Android background-start restriction can block direct launch; fallback to a high-priority notification that opens `QuickAddActivity` on tap.
- Wrong parse: log raw SMS and add bank-specific parser class.

## 13) What to build next

- Room persistence wired to `saveButton`
- Expense list screen in `MainActivity`
- Dedupe hash (amount + minute bucket + payee)
- Category suggestion rules
