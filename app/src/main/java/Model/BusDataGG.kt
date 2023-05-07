package Model

/*
* 24번 / 720-3번 버스 도착정보
* 마을버스는 경기도 버스 도착정보 확인 사이트에서 AJAX 크롤링으로 가져올 거임
* 크롤링을 수행하는 별도의 서버리스 API 구축 후, 여기에 요청을 보내어 도착정보를 가져온다.
*
* timeRemaining: Int
*   - 남은 도착시간
*   - 초 단위로 제공 -> 이를 분으로 환산하여 View에 적용
*
* currentStation: String
*   - 현재 버스가 위치한 정류장, 마지막으로 출발한 정류장
*
* */

data class BusDataGG(
    val timeRemaining: Int,
    val currentStation: String
)