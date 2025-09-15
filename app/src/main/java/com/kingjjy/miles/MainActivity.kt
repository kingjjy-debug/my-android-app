package com.kingjjy.miles

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private fun loadPoints(): List<String> {
        return try {
            val input = assets.open("points.json")
            val text = BufferedReader(InputStreamReader(input)).use { it.readText() }
            val arr = JSONArray(text)
            buildList {
                for (i in 0 until arr.length()) {
                    add(arr.getString(i))
                }
            }
        } catch (e: Exception) {
            listOf("데이터를 불러오지 못했습니다: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rv = findViewById<RecyclerView>(R.id.pointsRecycler)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = PointsAdapter(loadPoints())
    }
}
