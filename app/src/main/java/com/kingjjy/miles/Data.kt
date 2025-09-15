package com.kingjjy.miles

data class MileageItem(
    val key: String,
    val name: String,
    val earnUrl: String? = null,
    val convertUrl: String,
    val note: String,
    var isCompleted: Boolean = false
)

object MileageRepository {
    fun getItems(): List<MileageItem> {
        return listOf(
            MileageItem(
                name = "네이버페이",
                earnUrl = "https://pay.naver.com/",
                convertUrl = "https://m.help.pay.naver.com/faq/list.help?faqId=12979",
                note = "일부 제휴 포인트만 대한항공 마일리지 전환 가능. 조건은 네이버페이 고객센터 공지 확인 필요.",
                key = "naverpay"
            ),
            MileageItem(
                name = "OK캐시백",
                earnUrl = "https://www.okcashbag.com/",
                convertUrl = "https://www.koreanair.com/contents/skypass/earn-miles/travel-and-life/shopping-points",
                note = "대한항공 공식 사이트에서 OK캐시백 → SKYPASS 전환 안내 제공. 최소 단위 및 연간 한도 있음.",
                key = "okcashbag"
            ),
            MileageItem(
                name = "L.POINT",
                earnUrl = "https://www.lpoint.com/",
                convertUrl = "https://www.lpoint.com/",
                note = "블로그/커뮤니티 기준 전환 가능하다고 알려져 있음. 공식 사이트에서 유효 여부 확인 필요.",
                key = "lpoint"
            ),
            MileageItem(
                name = "신한카드 (마이신한포인트)",
                convertUrl = "https://www.shinhancard.com/",
                note = "카드 사용 적립 포인트를 대한항공 마일리지로 전환 가능. 전환 비율 및 조건은 카드사 확인 필요.",
                key = "shinhan"
            ),
            MileageItem(
                name = "삼성카드 (보너스포인트)",
                convertUrl = "https://www.samsungcard.com/",
                note = "보너스포인트를 대한항공 마일리지로 전환 가능. 최소 단위/비율은 삼성카드 확인 필요.",
                key = "samsung"
            ),
            MileageItem(
                name = "KB국민카드 (포인트리)",
                convertUrl = "https://card.kbcard.com/",
                note = "포인트리를 대한항공 마일리지로 전환 가능. 전환 비율 및 조건은 KB국민카드 확인 필요.",
                key = "kb"
            ),
            MileageItem(
                name = "현대카드 (M포인트)",
                convertUrl = "https://www.hyundaicard.com/",
                note = "M포인트를 대한항공 마일리지로 전환 가능. 조건은 현대카드 확인 필요.",
                key = "hyundai"
            )
        )
    }
}