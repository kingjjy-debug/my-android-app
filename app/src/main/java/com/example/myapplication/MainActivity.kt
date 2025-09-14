package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val pointData = listOf(
        PointData("네이버페이", "https://pay.naver.com/", "https://m.help.pay.naver.com/faq/list.help?faqId=12979", "네이버페이"),
        PointData("OK캐시백", "https://www.okcashbag.com/", "https://www.koreanair.com/contents/skypass/earn-miles/travel-and-life/shopping-points", "OK캐시백"),
        PointData("L.POINT", "https://www.lpoint.com/", "https://www.lpoint.com/", "L.POINT"),
        PointData("신한카드 (마이신한포인트)", "", "https://www.shinhancard.com/", "신한"),
        PointData("삼성카드 (보너스포인트)", "", "https://www.samsungcard.com/", "삼성"),
        PointData("KB국민카드 (포인트리)", "", "https://card.kbcard.com/", "KB"),
        PointData("현대카드 (M포인트)", "", "https://www.hyundaicard.com/", "현대")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences = getSharedPreferences("app_data", Context.MODE_PRIVATE)

        resetCompletedIfNecessary()

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = PointAdapter(pointData, sharedPreferences, this::onCompletedChanged, this::openUrl)
    }

    private fun resetCompletedIfNecessary(){
        val lastDate = sharedPreferences.getString("lastDate", "")
        if(lastDate != dateFormat.format(Date())){
            sharedPreferences.edit().clear().apply()
        }
        sharedPreferences.edit().putString("lastDate", dateFormat.format(Date())).apply()
    }

    private fun onCompletedChanged(key:String, completed: Boolean){
        sharedPreferences.edit().putBoolean(key, completed).apply()
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

data class PointData(val name: String, val earnUrl: String, val convertUrl: String, val key: String) 