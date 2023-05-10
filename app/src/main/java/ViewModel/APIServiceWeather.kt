package ViewModel

import Model.WeatherData
import retrofit2.http.GET

interface APIServiceWeather {
    @GET("weather")
    suspend fun getWeatherInfo(): WeatherData
}