package com.example.myapplication

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.setMargins
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PointProvider(
    val name: String,
    val earnUrl: String? = null,
    val convertUrl: String,
    val note: String,
    val key: String
)

class MainActivity : AppCompatActivity() {

    private val PREFS_NAME = "koreanair_mileage_prefs"
    private val KEY_LAST_DATE = "last_recorded_date"
    private val STATUS_PREFIX = "status_"

    private lateinit var sharedPreferences: SharedPreferences

    private val pointProviders = listOf(
        PointProvider(
            name = "네이버페이",
            earnUrl = "https://pay.naver.com/",
            convertUrl = "https://m.help.pay.naver.com/faq/list.help?faqId=12979",
            note = "일부 제휴 포인트만 대한항공 마일리지 전환 가능. 조건은 네이버페이 고객센터 공지 확인 필요.",
            key = "naverpay"
        ),
        PointProvider(
            name = "OK캐시백",
            earnUrl = "https://www.okcashbag.com/",
            convertUrl = "https://www.koreanair.com/contents/skypass/earn-miles/travel-and-life/shopping-points",
            note = "대한항공 공식 사이트에서 OK캐시백 → SKYPASS 전환 안내 제공. 최소 단위 및 연간 한도 있음.",
            key = "okcashbag"
        ),
        PointProvider(
            name = "L.POINT",
            earnUrl = "https://www.lpoint.com/",
            convertUrl = "https://www.lpoint.com/",
            note = "블로그/커뮤니티 기준 전환 가능하다고 알려져 있음. 공식 사이트에서 유효 여부 확인 필요.",
            key = "lpoint"
        ),
        PointProvider(
            name = "신한카드 (마이신한포인트)",
            convertUrl = "https://www.shinhancard.com/",
            note = "카드 사용 적립 포인트를 대한항공 마일리지로 전환 가능. 전환 비율 및 조건은 카드사 확인 필요.",
            key = "shinhan"
        ),
        PointProvider(
            name = "삼성카드 (보너스포인트)",
            convertUrl = "https://www.samsungcard.com/",
            note = "보너스포인트를 대한항공 마일리지로 전환 가능. 최소 단위/비율은 삼성카드 확인 필요.",
            key = "samsung"
        ),
        PointProvider(
            name = "KB국민카드 (포인트리)",
            convertUrl = "https://card.kbcard.com/",
            note = "포인트리를 대한항공 마일리지로 전환 가능. 전환 비율 및 조건은 KB국민카드 확인 필요.",
            key = "kb"
        ),
        PointProvider(
            name = "현대카드 (M포인트)",
            convertUrl = "https://www.hyundaicard.com/",
            note = "M포인트를 대한항공 마일리지로 전환 가능. 조건은 현대카드 확인 필요.",
            key = "hyundai"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        checkAndResetDailyStatus()

        val container = findViewById<LinearLayout>(R.id.card_container)
        populateCards(container)
    }

    private fun checkAndResetDailyStatus() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDate = sharedPreferences.getString(KEY_LAST_DATE, "")

        if (today != lastDate) {
            val editor = sharedPreferences.edit()
            pointProviders.forEach { provider ->
                editor.putBoolean(STATUS_PREFIX + provider.key, false)
            }
            editor.putString(KEY_LAST_DATE, today)
            editor.apply()
        }
    }

    private fun populateCards(container: LinearLayout) {
        container.removeAllViews()
        pointProviders.forEach { provider ->
            val card = createPointCard(provider)
            container.addView(card)
        }
    }

    private fun createPointCard(provider: PointProvider): View {
        val context = this
        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8)) }
            radius = dpToPx(12).toFloat()
            cardElevation = dpToPx(4).toFloat()
        }

        val cardContentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }

        val title = TextView(context).apply {
            text = provider.name
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val buttonLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dpToPx(12)
            }
        }

        provider.earnUrl?.let {
            val earnButton = createMaterialButton("포인트 적립하기") { openUrl(it) }
            buttonLayout.addView(earnButton)
        }

        val convertButton = createMaterialButton("마일리지 전환하기") { openUrl(provider.convertUrl) }
        buttonLayout.addView(convertButton)

        val completionLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dpToPx(16)
            }
        }

        val completedText = TextView(context).apply {
            text = "일일미션 수행 완료!"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            setTextColor(Color.parseColor("#4CAF50"))
            setTypeface(typeface, Typeface.BOLD)
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val completionSwitch = SwitchMaterial(context).apply {
            text = "완료"
            isChecked = sharedPreferences.getBoolean(STATUS_PREFIX + provider.key, false)
            setOnCheckedChangeListener { _, isChecked ->
                sharedPreferences.edit().putBoolean(STATUS_PREFIX + provider.key, isChecked).apply()
                completedText.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
        }
        if(completionSwitch.isChecked) completedText.visibility = View.VISIBLE

        completionLayout.addView(completedText)
        completionLayout.addView(completionSwitch)

        cardContentLayout.addView(title)
        cardContentLayout.addView(buttonLayout)
        cardContentLayout.addView(completionLayout)
        card.addView(cardContentLayout)

        return card
    }

    private fun createMaterialButton(text: String, onClick: () -> Unit): MaterialButton {
        return MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            this.text = text
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dpToPx(8)
            }
            setOnClickListener { onClick() }
            minHeight = dpToPx(48)
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "연결할 수 없습니다. 네트워크 상태를 확인하세요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}