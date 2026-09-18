package com.example.classschedule.domain

/** A restrained, theme-independent palette for course identity markers. */
val CourseColorPalette: List<String> = listOf(
    "#2F7D6D", // teal
    "#4D6F9F", // blue
    "#A36E2F", // amber
    "#805C94", // plum
    "#7A5A4E", // umber
    "#3B7B84", // cyan
    "#B45A5A", // brick
    "#6B7F39", // olive
    "#9A5F8F", // mauve
    "#567D8C", // steel
    "#8A6F35", // ochre
    "#4F8060"  // green
)

const val DefaultCourseColor = "#39766B"

/**
 * Picks a deterministic unused palette entry. Hashing the name keeps the result stable
 * across process restarts; probing the palette avoids avoidable collisions with existing
 * courses.
 */
fun allocateCourseColor(courseName: String, usedColors: Collection<String>): String {
    val normalizedUsed = usedColors.map { it.trim().uppercase() }.toSet()
    val seed = courseName.trim().lowercase().hashCode().toUInt().toInt().ushr(1)
    val start = seed % CourseColorPalette.size
    CourseColorPalette.indices.forEach { offset ->
        val candidate = CourseColorPalette[(start + offset) % CourseColorPalette.size]
        if (candidate.uppercase() !in normalizedUsed) return candidate
    }
    return CourseColorPalette[start]
}
