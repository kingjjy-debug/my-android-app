package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val pointData = Gson().fromJson<List<Point>>(applicationContext.assets.open("points.json").bufferedReader().use { it.readText() }, object : TypeToken<List<Point>>() {}.type)

    private val sharedPreferences by lazy { getSharedPreferences("app_data", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView = findViewById<TextView>(R.id.textView)
        val button = findViewById<Button>(R.id.button)

        var count = 0
        button.setOnClickListener { 
            count++
            textView.text = "안녕하세요! ($count)"
        }

        resetIfNewDay()
    }

    private fun resetIfNewDay() {
        val lastDate = sharedPreferences.getString("last_date", null)
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

        if (lastDate == null || lastDate != today) {
            sharedPreferences.edit().clear().apply()
        }
        sharedPreferences.edit().putString("last_date", today).apply()
    }

}

data class Point(
    val name: String,
    val earnUrl: String?,
    val convertUrl: String,
    val note: String,
    val key: String
)
