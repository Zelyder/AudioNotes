package com.zelyder.audionotes.data.model

import android.net.Uri
import com.zelyder.audionotes.MainActivity
import java.text.SimpleDateFormat
import java.util.*

data class Audio(
    val uri: Uri,
    val displayName: String,
    val id: Long,
    val date: Long,
    val duration: Long,
    val title: String
) {
    fun asRecording(): MainActivity.Recording = MainActivity.Recording(
        title = title,
        date = formatDate(date),
        duration = duration,
    )

    // TODO: add support other languages
    private fun formatDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MM.dd.yyyy в HH:mm", Locale.getDefault())
        val todayFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = Date(timestamp * 1000)
        val today = Calendar.getInstance().apply { time = Date() }
        val timestampDay = Calendar.getInstance().apply { time = date }
        return if (today.get(Calendar.DAY_OF_YEAR) == timestampDay.get(Calendar.DAY_OF_YEAR)) {
            "Сегодня в ${todayFormat.format(date)}"
        } else {
            dateFormat.format(date)
        }
    }
}
