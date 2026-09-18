package com.example.classschedule.domain

fun reminderLine(item: ResolvedMeeting): String {
    val place = item.course.location.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
    return "${formatMinutes(item.startMinutes)}-${formatMinutes(item.endMinutes)} ${item.course.name}$place"
}

fun reminderSummary(stage: Stage, dateLabel: String, items: List<ResolvedMeeting>): List<String> {
    if (items.isEmpty()) return listOf("暂无课程，按时完成今日计划")
    return items.map(::reminderLine)
}
