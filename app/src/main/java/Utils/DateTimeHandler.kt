package Utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeHandler {
    fun getCurrentTimeStamp(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = Date()
        return dateFormat.format(date)
    }
}