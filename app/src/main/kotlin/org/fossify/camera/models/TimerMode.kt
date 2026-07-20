package org.fossify.camera.models

import org.fossify.camera.R

enum class TimerMode(val prefValue: Int, val millisInFuture: Long) {
    OFF(0, 0),
    TIMER_3(1, 3000),
    TIMER_10(3, 10000);

    fun next(): TimerMode {
        return when (this) {
            OFF -> TIMER_3
            TIMER_3 -> TIMER_10
            TIMER_10 -> OFF
        }
    }

    fun getTimerModeDrawableRes(): Int {
        return when (this) {
            OFF -> R.drawable.ic_timer_off_vector
            TIMER_3 -> R.drawable.ic_timer_3_vector
            TIMER_10 -> R.drawable.ic_timer_10_vector
        }
    }

    fun getTimerModeLabelRes(): Int {
        return when (this) {
            OFF -> R.string.timer_off
            TIMER_3 -> R.string.timer_3s
            TIMER_10 -> R.string.timer_10s
        }
    }

    companion object {
        // raw pref value 2 was the removed TIMER_5 (5s); migrate it down to TIMER_3
        fun fromPrefValue(prefValue: Int): TimerMode {
            return when (prefValue) {
                TIMER_3.prefValue -> TIMER_3
                2 -> TIMER_3
                TIMER_10.prefValue -> TIMER_10
                else -> OFF
            }
        }
    }
}
