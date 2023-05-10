package Model

data class WeatherData(
    val minTemp: String,
    val currentTemp: String,
    val maxTemp: String,
    val weather: String,
    val microDust: String,
    val ultraMicroDust: String
)
