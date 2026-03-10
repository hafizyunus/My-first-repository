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
