package Model

// 단말기 현재 상태
data class DeviceStatus(val timeStamp: String, val batteryPercent: Int, val batteryAmpere: Int)
