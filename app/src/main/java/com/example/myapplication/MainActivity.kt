package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    private lateinit var textView: TextView
    private var countJunyoung = 0
    private var countJihye = 0
    private var countYaju = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)

        val buttonJunyoung = findViewById<Button>(R.id.buttonJunyoung)
        val buttonJihye = findViewById<Button>(R.id.buttonJihye)
        val buttonYaju = findViewById<Button>(R.id.buttonYaju)

        buttonJunyoung.setOnClickListener {
            countJunyoung++
            textView.text = "박지혜 사랑해\n버튼 ${countJunyoung}번 누름"
        }
        buttonJihye.setOnClickListener {
            countJihye++
            textView.text = "장준영 사랑해\n버튼 ${countJihye}번 누름"
        }
        buttonYaju.setOnClickListener {
            countYaju++
            textView.text = "코야하기 싫어어\n버튼 ${countYaju}번 누름"
        }
    }
}