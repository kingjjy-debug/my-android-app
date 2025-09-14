package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var data: List<PointData>
    private val gson = Gson()
    private val sharedPreferencesKey = "point_completion"

    data class PointData(val name: String, val earnUrl: String?, val convertUrl: String, val key: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.textView)
        data = loadPointData(this)
        loadData()
    }

    private fun loadData() {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val completionStatus = sharedPreferences.getString(sharedPreferencesKey, null)
        val map = gson.fromJson(completionStatus, object : TypeToken<HashMap<String, Boolean>>() {}.type)

        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val lastRecordedDay = sharedPreferences.getInt("lastRecordedDay", 0)

        if (lastRecordedDay != today) {
            sharedPreferences.edit().putString(sharedPreferencesKey, null).putInt("lastRecordedDay", today).apply()
        } 

        data.forEach { point ->
            val completion = map?.get(point.key) ?: false
            //UI 갱신 코드 추가
        }
    }

    private fun loadPointData(context: Context): List<PointData> {
        val jsonString = context.assets.open("points.json").bufferedReader().use { it.readText() }
        return gson.fromJson(jsonString, object : TypeToken<List<PointData>>() {}.type)
    }

    private fun openLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "연결할 수 없습니다. 네트워크 상태를 확인하세요.", Toast.LENGTH_SHORT).show()
        }
    }
}