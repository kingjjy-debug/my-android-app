package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView

    private val data = listOf(
        PointData("네이버페이", "https://pay.naver.com/", "https://m.help.pay.naver.com/faq/list.help?faqId=12979", "naverpay"),
        PointData("OK캐시백", "https://www.okcashbag.com/", "https://www.koreanair.com/contents/skypass/earn-miles/travel-and-life/shopping-points", "okcashbag"),
        PointData("L.POINT", "https://www.lpoint.com/", "https://www.lpoint.com/", "lpoint"),
        PointData("신한카드 (마이신한포인트)", "", "https://www.shinhancard.com/", "shinhan"),
        PointData("삼성카드 (보너스포인트)", "", "https://www.samsungcard.com/", "samsung"),
        PointData("KB국민카드 (포인트리)", "", "https://card.kbcard.com/", "kb"),
        PointData("현대카드 (M포인트)", "", "https://www.hyundaicard.com/", "hyundai")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.textView)

        val sharedPref = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val lastDate = sharedPref.getLong("lastDate", -1L)
        val today = Calendar.getInstance().timeInMillis

        if (lastDate != -1L && TimeUnit.DAYS.convert(today - lastDate, TimeUnit.MILLISECONDS) > 0) {
            sharedPref.edit().clear().apply()
        }
        data.forEach { addToggle(it) }
    }

    private fun addToggle(data: PointData) {
        val toggle = ToggleButton(this)
        toggle.textOff = "미완료"
        toggle.textOn = "완료"

        toggle.text = if(getSharedPreferences("myPrefs", Context.MODE_PRIVATE).getBoolean(data.key, false)) "완료" else "미완료"
        toggle.isChecked = getSharedPreferences("myPrefs", Context.MODE_PRIVATE).getBoolean(data.key, false)

        toggle.setOnCheckedChangeListener { _, isChecked -><
            getSharedPreferences("myPrefs", Context.MODE_PRIVATE).edit().putBoolean(data.key, isChecked).apply()
        }
    }

    data class PointData(val name: String, val earnUrl: String, val convertUrl: String, val key: String)
}