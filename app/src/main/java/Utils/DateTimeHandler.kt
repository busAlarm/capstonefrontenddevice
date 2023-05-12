package Utils

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object DateTimeHandler {
    fun getCurrentTimeStamp(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = Date()
        return dateFormat.format(date)
    }

    fun getCurrentDate(): String {
        val now = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
        return now.format(formatter)
    }

    fun getCurrentTime(): String {
        val now = LocalTime.now()
        val formatter = DateTimeFormatter.ofPattern("a hh:mm")
        return now.format(formatter) // "오후 12:34"
    }

    fun getThisMonth(): Int {
        return LocalDate.now().month.value
    }

    fun getNextMonth(): Int {
        var month = LocalDate.now().month.value

        if (month == 12) {
            month = 1
        } else {
            month += 1
        }

        return month
    }

    // 현재 시간의 시, 분을 분으로 계산하여 반환
    fun getCurrentTimeAsMinute(): Int {
        val now = LocalTime.now()
        return now.hour * 60 + now.minute
    }

    fun convertMinuteToHMFormat(minute: Int): String {
        val hour = minute / 60
        val minute = minute % 60
        return "${hour}:${minute}"
    }
}