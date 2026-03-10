package com.example.expensetracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.expensetracker.parser.GenericUpiSmsParser
import com.example.expensetracker.util.DraftIntents

class BankSmsReceiver : BroadcastReceiver() {
    private val parser = GenericUpiSmsParser()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val body = smsMessages.joinToString(separator = "") { it.messageBody.orEmpty() }
        val timestamp = smsMessages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

        val draft = parser.parse(body, timestamp) ?: return
        context.startActivity(DraftIntents.quickAddIntent(context, draft))
    }
}
