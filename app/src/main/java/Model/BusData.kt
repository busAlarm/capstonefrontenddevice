package Model

/*
* 모두 String 타입
* firstTime: hh:mm
* lastTime: hh:mm
* predictTime1: 정수 (분 단위)
* stationNm1: 정류장 이름
* predictTime1: 정수 (분 단위)
* stationNm2: 정류장 이름
* */

data class BusData(
    val firstTime: String,
    val lastTime: String,
    var predictTime1: String,
    var stationNm1: String,
    var predictTime2: String,
    var stationNm2: String,
    val isRunning: Boolean,
    var arrivalSoon: Boolean
) {
    fun checkArrival(): BusData {
        if (predictTime1 == "") {
            predictTime1 = "-1"
            stationNm1 = "도착 정보 없음"
        }

        if (predictTime2 == "") {
            predictTime2 = "-1"
            stationNm2 = "도착 정보 없음"
        }

        return this
    }
}
