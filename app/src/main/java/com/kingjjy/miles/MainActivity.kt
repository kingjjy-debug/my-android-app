package com.kingjjy.miles

import com.kingjjy.miles.R

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPrefs: SharedPreferences
    private val PREFS_NAME = "MileagePrefs"
    private val KEY_LAST_DATE = "last_saved_date"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        checkForDateReset()

        val items = loadDataWithState()
        val recyclerView: RecyclerView = findViewById(R.id.list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ItemAdapter(
            items = items,
            onUrlClick = { url -> openUrl(url) },
            onCompletionChanged = { item, isCompleted ->
                saveItemState(item.key, isCompleted)
            }
        )
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun checkForDateReset() {
        val today = getTodayDateString()
        val lastDate = sharedPrefs.getString(KEY_LAST_DATE, null)

        if (today != lastDate) {
            val editor = sharedPrefs.edit()
            MileageRepository.getItems().forEach { item ->
                editor.remove(getPreferenceKey(item.key))
            }
            editor.putString(KEY_LAST_DATE, today)
            editor.apply()
        }
    }

    private fun loadDataWithState(): List<MileageItem> {
        return MileageRepository.getItems().map { item ->
            val isCompleted = sharedPrefs.getBoolean(getPreferenceKey(item.key), false)
            item.copy(isCompleted = isCompleted)
        }
    }

    private fun saveItemState(key: String, isCompleted: Boolean) {
        sharedPrefs.edit().putBoolean(getPreferenceKey(key), isCompleted).apply()
    }

    private fun getPreferenceKey(key: String) = "completed_$key"

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.network_error_toast), Toast.LENGTH_SHORT).show()
        }
    }
}
