package Model

// 단말기 현재 상태
data class DeviceStatus(
    val type: String,
    val timeStamp: String,
    val ssid: String,
    val brightness: String,
    val signalPower: String,
    val batteryPercent: String,
    val batteryAmpere: String,
    val logMessage: String,
)