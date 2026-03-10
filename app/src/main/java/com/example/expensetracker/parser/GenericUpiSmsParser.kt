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
