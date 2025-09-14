package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val pointList = listOf(
        PointItem("네이버페이", "https://pay.naver.com/", "https://m.help.pay.naver.com/faq/list.help?faqId=12979", "네이버페이",
            "일부 제휴 포인트만 대한항공 마일리지 전환 가능. 조건은 네이버페이 고객센터 공지 확인 필요."),
        PointItem("OK캐시백", "https://www.okcashbag.com/", "https://www.koreanair.com/contents/skypass/earn-miles/travel-and-life/shopping-points", "OK캐시백",
            "대한항공 공식 사이트에서 OK캐시백 → SKYPASS 전환 안내 제공. 최소 단위 및 연간 한도 있음."),
        PointItem("L.POINT", "https://www.lpoint.com/", "https://www.lpoint.com/", "L.POINT",
            "블로그/커뮤니티 기준 전환 가능하다고 알려져 있음. 공식 사이트에서 유효 여부 확인 필요."),
        PointItem("신한카드 (마이신한포인트)", "", "https://www.shinhancard.com/", "신한카드", "카드 사용 적립 포인트를 대한항공 마일리지로 전환 가능. 전환 비율 및 조건은 카드사 확인 필요."),
        PointItem("삼성카드 (보너스포인트)", "", "https://www.samsungcard.com/", "삼성카드", "보너스포인트를 대한항공 마일리지로 전환 가능. 최소 단위/비율은 삼성카드 확인 필요."),
        PointItem("KB국민카드 (포인트리)", "", "https://card.kbcard.com/", "KB국민카드", "포인트리를 대한항공 마일리지로 전환 가능. 전환 비율 및 조건은 KB국민카드 확인 필요."),
        PointItem("현대카드 (M포인트)", "", "https://www.hyundaicard.com/", "현대카드", "M포인트를 대한항공 마일리지로 전환 가능. 조건은 현대카드 확인 필요.")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences = getSharedPreferences("mileage_app", Context.MODE_PRIVATE)

        resetIfNecessary()

        val adapter = PointAdapter(pointList, sharedPreferences) {
            openUrl(it.earnUrl)
        } { key, isChecked ->
            sharedPreferences.edit().putBoolean(key, isChecked).apply()
            adapter.notifyDataSetChanged()
        } { key ->
            openUrl(pointList.find { it.key == key }?.convertUrl ?: "")
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun resetIfNecessary() {
        val lastDate = sharedPreferences.getString("lastDate", null)
        if (lastDate != dateFormat.format(Date())) {
            sharedPreferences.edit().clear().apply()
            sharedPreferences.edit().putString("lastDate", dateFormat.format(Date())).apply()
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "연결할 수 없습니다. 네트워크 상태를 확인하세요.", Toast.LENGTH_SHORT).show()
        }
    }
}

data class PointItem(
    val name: String,
    val earnUrl: String,
    val convertUrl: String,
    val key: String,
    val note: String
)
