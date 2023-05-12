package Model

import Utils.DateTimeHandler
import android.util.Log

/*
{
    "schedule": {
        "3": [
            {
                "date": "2023.03.02",
                "content": "2023학년도 1학기 개강"
            },
            ...
        ],
            ...
    }
},
* */

data class CampusSchedule(
    val schedule: Map<String, List<Map<String, String>>>
) {
    fun getThisMonthSchedule(): List<Map<String, String>> {
        val month = DateTimeHandler.getThisMonth()
        Log.d("monththis", "$month")
        return schedule["$month"] ?: emptyList()
    }

    fun getNextMonthSchedule(): List<Map<String, String>> {
        val month = DateTimeHandler.getNextMonth()
        Log.d("monthnext", "$month")
        return schedule["$month"] ?: emptyList()
    }
}
