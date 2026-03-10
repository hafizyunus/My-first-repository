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
        titleInput.setText(intent.getStringExtra("title").orEmpty())

        saveButton.setOnClickListener {
            categoryInput.text.toString()
            finish()
        }
    }
}
