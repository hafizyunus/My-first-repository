package com.example.expensetracker.data

data class ExpenseDraft(
    val amount: Double?,
    val timestampMillis: Long,
    val payee: String?,
    val title: String?,
    val category: String? = null,
    val source: String,
    val rawText: String
)
