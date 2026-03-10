package com.example.expensetracker.parser

import com.example.expensetracker.data.ExpenseDraft

interface SmsParser {
    fun parse(message: String, timestampMillis: Long): ExpenseDraft?
}
