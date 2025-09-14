package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.style.StyleSpan
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.bold
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private val pointData = loadPointData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPreferences = getSharedPreferences("mileage_app_data", Context.MODE_PRIVATE)

        resetCompletedIfNecessary()
        renderViews()
    }

    private fun resetCompletedIfNecessary() {
        val lastResetDate = sharedPreferences.getString("lastResetDate", null)
        val currentDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

        if (lastResetDate != currentDate) {
            sharedPreferences.edit().clear().apply()
            sharedPreferences.edit().putString("lastResetDate", currentDate).apply()
        }
    }

    private fun renderViews() {
        pointData.forEach { data ->
            val card = layoutInflater.inflate(R.layout.point_card, null)

            card.findViewById<TextView>(R.id.pointName).text = data.name.bold()
            card.findViewById<Button>(R.id.earnButton)?.let { button ->
                button.text = "포인트 적립하기"
                button.setOnClickListener {
                    openWebPage(data.earnUrl)
                }
            }
            card.findViewById<Button>(R.id.convertButton)?.let { button ->
                button.text = "마일리지 전환하기"
                button.setOnClickListener {
                    openWebPage(data.convertUrl)
                }
            }
            val checkBox = card.findViewById<CheckBox>(R.id.completedCheckBox)
            checkBox.isChecked = sharedPreferences.getBoolean(data.key, false)
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                sharedPreferences.edit().putBoolean(data.key, isChecked).apply()
                updateCompletedText(card, isChecked)
            }
            updateCompletedText(card, checkBox.isChecked)
            findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.cardContainer).addView(card)
        }
    }

    private fun updateCompletedText(card: View, isChecked: Boolean) {
        val completedTextView = card.findViewById<TextView>(R.id.completedTextView)
        if (isChecked) {
            completedTextView.text = "일일미션 수행 완료!".bold().also { it.setTextColor(getColor(R.color.green)) }
        } else {
            completedTextView.text = ""
        }
    }

    private fun openWebPage(url: String) {
        val webpage: Uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            showNetworkError()
        }
    }

    private fun showNetworkError() {
        Toast.makeText(this, "연결할 수 없습니다. 네트워크 상태를 확인하세요.", Toast.LENGTH_SHORT).show()
    }

    private data class PointData(
        val name: String,
        val earnUrl: String,
        val convertUrl: String,
        val note: String,
        val key: String
    )

    private fun loadPointData(): List<PointData> {
        return listOf(
            PointData("네이버페이", "https://pay.naver.com/", "https://m.help.pay.naver.com/faq/list.help?faqId=12979", "", "naverpay"),
            PointData("OK캐시백", "https://www.okcashbag.com/", "https://www.koreanair.com/contents/skypass/earn-miles/travel-and-life/shopping-points", "", "okcashbag"),
            PointData("L.POINT", "https://www.lpoint.com/", "https://www.lpoint.com/", "", "lpoint"),
            PointData("신한카드 (마이신한포인트)", "", "https://www.shinhancard.com/", "", "shinhan"),
            PointData("삼성카드 (보너스포인트)", "", "https://www.samsungcard.com/", "", "samsung"),
            PointData("KB국민카드 (포인트리)", "", "https://card.kbcard.com/", "", "kb"),
            PointData("현대카드 (M포인트)", "", "https://www.hyundaicard.com/", "", "hyundai")
        )
    }
}