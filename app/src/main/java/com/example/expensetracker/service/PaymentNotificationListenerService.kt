package com.example.expensetracker.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.expensetracker.parser.GenericUpiSmsParser
import com.example.expensetracker.util.DraftIntents

class PaymentNotificationListenerService : NotificationListenerService() {
    private val parser = GenericUpiSmsParser()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getString("android.title").orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        val combined = "$title $text".trim()

        val draft = parser.parse(combined, sbn.postTime) ?: return
        startActivity(DraftIntents.quickAddIntent(this, draft.copy(source = "notification")))
    }
}
