package Model

/*
* 셔틀버스 예상 도착시간
* estimateArrivalTime: Int
*   - 예상 도착시간 (죽전역 출발 시간 기준 + 10분
*
* estimateDepartureTime: Int
*   - 예상 출발시간 (죽전역 도착 시간 기준 + 5분)
*
*   - 이후 버스 예상 출발, 도착 시간은 누적하여 더하는 것으로 반영
*   - 운행시간까지 누적하며, 운행시간이 지나면 다시 출발시간 (08시?) 기준으로 초기화
*
* */

data class BusDataShuttle(
    val estimateArrivalTime: Int,
    val estimateDepartureTime: Int
)
