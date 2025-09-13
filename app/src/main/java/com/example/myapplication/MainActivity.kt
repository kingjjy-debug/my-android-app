package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 반드시 activity_main.xml을 사용
        setContentView(R.layout.activity_main)

        // XML에 있는 id와 정확히 맞춰서 가져오기
        val button: Button = findViewById(R.id.button)
        val textView: TextView = findViewById(R.id.textView)

        var count = 0
        button.setOnClickListener {
            count++
            textView.text = "안녕하세요! ($count)"
        }
    }
}
