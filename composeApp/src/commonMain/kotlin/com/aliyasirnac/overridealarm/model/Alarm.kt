package com.aliyasirnac.overridealarm.model

import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
enum class ChallengeType {
    NONE,
    MATH,
    SHAKE,
    TYPING
}

@Serializable
data class Alarm(
    val id: Long = Random.nextLong(0, Long.MAX_VALUE),
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val isEnabled: Boolean = true,
    val repeatDays: Set<Int> = emptySet(), // 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun (ISO)
    val vibrate: Boolean = true,
    val snoozeEnabled: Boolean = true,
    val snoozeMinutes: Int = 5,
    val challengeType: ChallengeType = ChallengeType.NONE,
    val ringtoneUri: String? = null,       // null = system default alarm tone
    val ringtoneName: String? = null       // display name for the selected tone
)

fun Alarm.formattedTimeParts(): Pair<String, String> {
    val h = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val amPm = if (hour < 12) "AM" else "PM"
    return "${h}:${minute.toString().padStart(2, '0')}" to amPm
}

// ISO day values: 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun
val DAY_LABELS = mapOf(
    1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu",
    5 to "Fri", 6 to "Sat", 7 to "Sun"
)

val ALL_DAYS = listOf(1, 2, 3, 4, 5, 6, 7)
