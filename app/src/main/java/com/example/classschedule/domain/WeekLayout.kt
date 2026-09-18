package com.example.classschedule.domain

/** A course's vertical extent in the discrete timetable grid. */
data class SlotSpan(
    val firstIndex: Int,
    val lastIndex: Int,
    val topPosition: Float,
    val bottomPosition: Float
) {
    val rowCount: Int get() = lastIndex - firstIndex + 1
}

/** A placement that keeps overlapping courses visible in separate lanes. */
data class MeetingPlacement(
    val item: ResolvedMeeting,
    val lane: Int,
    val laneCount: Int
)

/**
 * Converts a clock time to a row-relative coordinate. Gaps between configured slots are
 * kept at the row boundary so a course still covers every intersecting lesson row.
 */
fun gridPosition(minutes: Int, slots: List<TimeSlot>): Float {
    if (slots.isEmpty()) return 0f
    val ordered = slots.sortedBy { it.number }
    if (minutes <= ordered.first().startMinutes) return 0f
    ordered.forEachIndexed { index, slot ->
        if (minutes < slot.endMinutes) {
            val duration = (slot.endMinutes - slot.startMinutes).coerceAtLeast(1)
            val fraction = (minutes - slot.startMinutes).toFloat() / duration
            return index + fraction.coerceIn(0f, 1f)
        }
        val next = ordered.getOrNull(index + 1)
        if (next != null && minutes < next.startMinutes) return (index + 1).toFloat()
    }
    return ordered.size.toFloat()
}

/** Returns the rows touched by a meeting, including partial first/last rows. */
fun slotSpanFor(startMinutes: Int, endMinutes: Int, slots: List<TimeSlot>): SlotSpan? {
    if (slots.isEmpty() || endMinutes <= startMinutes) return null
    val ordered = slots.sortedBy { it.number }
    val intersecting = ordered.indices.filter { index ->
        val slot = ordered[index]
        startMinutes < slot.endMinutes && endMinutes > slot.startMinutes
    }
    val first = intersecting.firstOrNull() ?: ordered.indexOfFirst { endMinutes <= it.endMinutes }.coerceAtLeast(0)
    val last = intersecting.lastOrNull() ?: ordered.indexOfFirst { startMinutes < it.startMinutes }.let { if (it < 0) ordered.lastIndex else it }
    val top = gridPosition(startMinutes, ordered).coerceIn(0f, ordered.size.toFloat())
    val bottom = gridPosition(endMinutes, ordered).coerceIn(0f, ordered.size.toFloat())
    return SlotSpan(
        firstIndex = first.coerceIn(0, ordered.lastIndex),
        lastIndex = maxOf(first, last).coerceIn(0, ordered.lastIndex),
        topPosition = minOf(top, ordered.size.toFloat()),
        bottomPosition = maxOf(bottom, top + 0.08f).coerceAtMost(ordered.size.toFloat())
    )
}

fun placeMeetingLanes(items: List<ResolvedMeeting>): List<MeetingPlacement> {
    if (items.isEmpty()) return emptyList()
    val laneEnds = mutableListOf<Int>()
    val assignments = items.sortedWith(compareBy<ResolvedMeeting> { it.startMinutes }.thenBy { it.endMinutes })
        .map { item ->
            val lane = laneEnds.indexOfFirst { it <= item.startMinutes }.let { if (it >= 0) it else laneEnds.size }
            if (lane == laneEnds.size) laneEnds += item.endMinutes else laneEnds[lane] = item.endMinutes
            item to lane
        }
    val laneCount = laneEnds.size.coerceAtLeast(1)
    return assignments.map { (item, lane) -> MeetingPlacement(item, lane, laneCount) }
}
