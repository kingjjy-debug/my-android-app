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
import androidx.core.content.edit
import androidx.core.text.bold
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var sharedPreferences: androidx.preference.PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
        sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        resetCompletedIfNewDay()
        updateView()
    }

    private fun resetCompletedIfNewDay() {
        val lastRecordDate = sharedPreferences.getString("lastRecordDate", null)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if(lastRecordDate != today){
            sharedPreferences.edit { clear() }
        }
        sharedPreferences.edit { putString("lastRecordDate", today) }

    }

    private fun updateView() {
        val points = listOf(
            PointItem("네이버페이", "https://pay.naver.com/", "https://m.help.pay.naver.com/faq/list.help?faqId=12979", "naverpay"),
            PointItem("OK캐시백", "https://www.okcashbag.com/", "https://www.koreanair.com/contents/skypass/earn-miles/travel-and-life/shopping-points", "okcashbag"),
            PointItem("L.POINT", "https://www.lpoint.com/", "https://www.lpoint.com/", "lpoint"),
            PointItem("신한카드 (마이신한포인트)", "", "https://www.shinhancard.com/", "shinhan"),
            PointItem("삼성카드 (보너스포인트)", "", "https://www.samsungcard.com/", "samsung"),
            PointItem("KB국민카드 (포인트리)", "", "https://card.kbcard.com/", "kb"),
            PointItem("현대카드 (M포인트)", "", "https://www.hyundaicard.com/", "hyundai")
        )

        var text = ""
        points.forEach { point ->
            text += "${point.name}\n"
            if(point.earnUrl.isNotEmpty()) {
                val earnBtn = createButton(point.earnUrl, "포인트 적립하기")
                text += "${earnBtn.toString()}\n"
            }
            val convertBtn = createButton(point.convertUrl, "마일리지 전환하기")
            text += "${convertBtn.toString()}\n"
            text += "\n"
        }

        textView.text = text
    }

    private fun createButton(url: String, label: String): String {
        return "<button onclick='openURL(\"$url\")'>$label</button>"
    }

    private fun openURL(url: String) {
        try{
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch(e: Exception) {
            Toast.makeText(this, "연결할 수 없습니다. 네트워크 상태를 확인하세요.", Toast.LENGTH_SHORT).show()
        }
    }

    data class PointItem(val name: String, val earnUrl: String, val convertUrl: String, val key: String)
}
