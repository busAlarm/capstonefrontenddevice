package Model

// 24, 720-3, 셔틀 노선 도착 관련 통합정보
data class BusDataTotal(
    val timeRemaining24: Int,
    val currentStation24: String,
    val timeRemaining720_3: Int,
    val currentStation720_3: String,
    val estimateArrivalTimeShuttle: Int,
    val estimateDepartureTimeShuttle: Int
)
