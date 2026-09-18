package com.example.classschedule.ui

import android.icu.util.ChineseCalendar
import android.icu.util.GregorianCalendar
import android.icu.util.TimeZone
import com.nlf.calendar.Solar
import java.time.LocalDate
import java.util.Locale

data class AlmanacInfo(
    val date: LocalDate,
    val lunarDateLabel: String,
    val ganzhiYear: String,
    val zodiac: String,
    val dayYi: List<String> = emptyList(),
    val dayJi: List<String> = emptyList(),
    val detailAvailable: Boolean = false
)

/**
 * Uses lunar-java's local calculation for traditional daily yi/ji data. ICU remains as a
 * lightweight fallback for dates outside the library's supported calculation range.
 */
fun almanacForDate(date: LocalDate): AlmanacInfo = runCatching {
    val lunar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth).getLunar()
    val yi = lunar.getDayYi().normalizedActivities()
    val ji = lunar.getDayJi().normalizedActivities()
    AlmanacInfo(
        date = date,
        lunarDateLabel = "农历${lunar.getMonthInChinese()}月${lunar.getDayInChinese()}",
        ganzhiYear = "${lunar.getYearInGanZhi()}年",
        zodiac = "${lunar.getYearShengXiao()}年",
        dayYi = yi,
        dayJi = ji,
        detailAvailable = yi.isNotEmpty() || ji.isNotEmpty()
    )
}.getOrElse {
    fallbackAlmanacForDate(date)
}

private fun List<String>.normalizedActivities(): List<String> = asSequence()
    .map(String::trim)
    .filter { it.isNotBlank() && it != "无" }
    .distinct()
    .toList()

private fun fallbackAlmanacForDate(date: LocalDate): AlmanacInfo {
    val utc = TimeZone.getTimeZone("UTC")
    val gregorian = GregorianCalendar(utc, Locale.CHINA).apply {
        clear()
        set(date.year, date.monthValue - 1, date.dayOfMonth, 12, 0, 0)
    }
    val chinese = ChineseCalendar(utc, Locale.CHINA).apply {
        timeInMillis = gregorian.timeInMillis
    }
    val extendedYear = chinese.get(ChineseCalendar.EXTENDED_YEAR).takeIf { it in 1800..2200 } ?: date.year
    val month = (chinese.get(ChineseCalendar.MONTH) + 1).coerceIn(1, 12)
    val day = chinese.get(ChineseCalendar.DAY_OF_MONTH).coerceIn(1, 30)
    val leap = chinese.get(ChineseCalendar.IS_LEAP_MONTH) != 0
    val monthName = lunarMonths[month - 1]
    val dayName = lunarDays[day - 1]
    val branchIndex = Math.floorMod(extendedYear - 4, 12)
    val cycleIndex = Math.floorMod(extendedYear - 4, 60)
    return AlmanacInfo(
        date = date,
        lunarDateLabel = "农历${if (leap) "闰" else ""}$monthName$dayName",
        ganzhiYear = heavenlyStems[cycleIndex % 10] + earthlyBranches[cycleIndex % 12] + "年",
        zodiac = zodiacNames[branchIndex] + "年",
        detailAvailable = false
    )
}

private val lunarMonths = listOf(
    "正月", "二月", "三月", "四月", "五月", "六月",
    "七月", "八月", "九月", "十月", "冬月", "腊月"
)

private val lunarDays = listOf(
    "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
    "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
    "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
)

private val heavenlyStems = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
private val earthlyBranches = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
private val zodiacNames = listOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")
