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
