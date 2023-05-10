package com.example.busarrivalalram

import Model.BannerMessage
import Model.WeatherData
import Utils.DateTimeHandler
import ViewModel.APIServiceBannerMessage
import ViewModel.APIServiceBus
import ViewModel.APIServiceWeather
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.example.busarrivalalram.databinding.ActivityAsideBinding
import com.example.busarrivalalram.databinding.ActivityBsideBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

class BSideActivity : AppCompatActivity() {
    val binding by lazy { ActivityBsideBinding.inflate(layoutInflater) }

    // 현재 시간 변경 주기 (1초)
    val nowTimeInterval: Long = 1000

    // 격려 메시지 변경 주기 (기본깂 21600000 -> 6시간)
    val encourageInterval: Long = 30000

    // 날씨 변경 주기 (기본깂 3600000 -> 1시간)
    val weatherInterval: Long = 3600000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(binding.root)

        // title bar 가리기
        supportActionBar?.hide()

        // full screen
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        // 1. 상단 배너 - 시간
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                binding.currentDate.text = DateTimeHandler.getCurrentDate()
                binding.currentTime.text = DateTimeHandler.getCurrentTime()

                delay(nowTimeInterval)
            }
        }

        // 2. 상단 배너 - 격려 메시지
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                try {
                    val retrofit = Retrofit.Builder()
                        .baseUrl("https://3bnsysiqig.execute-api.ap-northeast-2.amazonaws.com/")
                        .addConverterFactory(GsonConverterFactory.create()).build()
                    val apiService = retrofit.create(APIServiceBannerMessage::class.java)
                    val bannerMessage: BannerMessage = apiService.getBusArrivalInfo()

                    binding.bannerMessage.text = bannerMessage.message

                    delay(encourageInterval)

                } catch (e: HttpException) {
                    if (e.code() != 200) {
                        // API 연결 오류 시 Toast 출력
                        val toast = Toast(this@BSideActivity)
                        toast.setText("${e.code()}")
                        toast.show()
                    }
                }
            }
        }

        // 3. 상단 배너 - 날씨
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                try {
                    val retrofit = Retrofit.Builder()
                        .baseUrl("https://b6m7et9sdl.execute-api.ap-northeast-2.amazonaws.com/")
                        .addConverterFactory(GsonConverterFactory.create()).build()
                    val apiService = retrofit.create(APIServiceWeather::class.java)
                    val weatherData: WeatherData = apiService.getWeatherInfo()

                    binding.highestTemperature.text = "${weatherData.highTemp}°"
                    binding.currentTemperature.text = "${weatherData.currentTemp}°"
                    binding.lowestTemperature.text = "${weatherData.lowTemp}°"

                    // 공백 제거 ex) 구름 많음 -> 구름많음
                    weatherData.weather.replace(" ", "")
                    binding.climateText.text = weatherData.weather

                    binding.microDustIndex.text = weatherData.microDust
                    binding.ultraMicroDustIndex.text = weatherData.ultraMicroDust

                    // 이미지 변경
                    when (weatherData.weather) {
                        "맑음" -> {
                            binding.climateImage.setImageResource(R.drawable.sun)
                        }
                        "구름많음" -> {
                            binding.climateImage.setImageResource(R.drawable.cloudy)
                        }
                        "흐림" -> {
                            binding.climateImage.setImageResource(R.drawable.clouds)
                        }
                        "비" -> {
                            binding.climateImage.setImageResource(R.drawable.rain)
                        }
                        "눈" -> {
                            binding.climateImage.setImageResource(R.drawable.snowflake)
                        }
                    }

                    delay(weatherInterval)

                } catch (e: HttpException) {
                    if (e.code() != 200) {
                        // API 연결 오류 시 Toast 출력
                        val toast = Toast(this@BSideActivity)
                        toast.setText("${e.code()}")
                        toast.show()
                    }
                }
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            // 상단 배너
            // 2. 격려메시지
            // 3. 날씨

            // 하단 배너 (좌측 기준 2/3 부분)
            // 1. 정류장 내릴 떄 각 건물별 기준 몇 분 정도 걸리는지,
            // 그래서 건물 입구 기준으로 도착하는 최종 시간은 언제쯤인지 (이게 제일 좋아 보인다.)
            // 버스 도착 시간 값을 받아와야 하는데..? 람다에서 또 가져오지 뭐...

            // 종합실험동 하차 시
            // (24, 720-3, 셔틀에서 하차)

            // 평화의광장 (곰상 앞) 하차 시
            // (24, 720-3, 셔틀에서 하차)

            // 인문관 하차 시
            // (720-3에서 하차)

            // 2. 뉴스 (아마 공간 부족할 듯?)
            // 3. 학식 메뉴 (이것도 마찬가지)

            while (true) {
                try {
                    // 1. API에 요청하여 값 가져오기
                    val retrofitBus = Retrofit.Builder()
                        .baseUrl("https://2ot8ocxpaf.execute-api.ap-northeast-2.amazonaws.com/")
                        .addConverterFactory(GsonConverterFactory.create()).build()
                    val apiServiceBus = retrofitBus.create(APIServiceBus::class.java)
                    val arrivalInfo24 = apiServiceBus.getBusArrivalInfo("24").checkArrival()
                    val arrivalInfo720_3 = apiServiceBus.getBusArrivalInfo("720-3").checkArrival()
                    val arrivalInfoShuttle =
                        apiServiceBus.getBusArrivalInfo("shuttle").checkArrival()


                } catch (e: HttpException) {
                    if (e.code() != 200) {
                        // API 연결 오류 시 Toast 출력
                        val toast = Toast(this@BSideActivity)
                        toast.setText("${e.code()}")
                        toast.show()
                    }
                }

                delay(45000) // temp. actual interval time is 30000 / 45000 / 60000 mills (30s / 45s / 1 min.)
            }
        }
    }
}