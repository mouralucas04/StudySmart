package com.lucasmoura.studysmart.util

import android.annotation.SuppressLint
import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.graphics.Color
import com.lucasmoura.studysmart.ui.theme.Green
import com.lucasmoura.studysmart.ui.theme.Orange
import com.lucasmoura.studysmart.ui.theme.Red
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

enum class Priority(val title: String, val color: Color, val value: Int){
    LOW(title = "Low", color = Green, value = 0),
    MEDIUM(title = "Medium", color = Orange, value = 1),
    HIGH(title = "High", color = Red, value = 2);

    companion object {
        fun fromInt(value: Int) : Priority = entries.firstOrNull{ it.value == value } ?: MEDIUM
    }
}

fun Long?.changeMillisToDateString() : String{
    val date: LocalDate = this?.let {
        Instant
            .ofEpochMilli(it)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
    } ?: LocalDate.now()

    return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
}

fun Long.toHours() : Float {
    val hours = this.toFloat() / 3600f
    return (hours * 100).roundToInt() / 100f
}

sealed class SnackbarEvent{
    data class ShowSnackbar(
        val message: String,
        val duration: SnackbarDuration = SnackbarDuration.Short
    ) : SnackbarEvent()

    data object NavigateUp : SnackbarEvent()
}

fun Int.pad() : String {
    return this.toString().padStart(length = 2, padChar = '0')
}