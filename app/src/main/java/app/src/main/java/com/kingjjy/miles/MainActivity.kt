package com.kingjjy.miles

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "com.kingjjy.miles.PREFERENCES"
    private val LAST_VISIT_DATE_KEY = "last_visit_date"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        checkAndResetDailyStatus()

        val recyclerView: RecyclerView = findViewById(R.id.list)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = ItemAdapter(
            items = DataProvider.items,
            sharedPreferences = sharedPreferences,
            onUrlClick = { url -> openUrl(url) },
            onCompletionChanged = { key, isCompleted ->
                saveCompletionStatus(key, isCompleted)
            }
        )
        recyclerView.adapter = adapter
    }

    private fun checkAndResetDailyStatus() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastVisitDate = sharedPreferences.getString(LAST_VISIT_DATE_KEY, null)

        if (today != lastVisitDate) {
            val editor = sharedPreferences.edit()
            DataProvider.items.forEach { item ->
                editor.putBoolean(item.key, false)
            }
            editor.putString(LAST_VISIT_DATE_KEY, today)
            editor.apply()
        }
    }

    private fun saveCompletionStatus(key: String, isCompleted: Boolean) {
        sharedPreferences.edit().putBoolean(key, isCompleted).apply()
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show()
        }
    }
}
