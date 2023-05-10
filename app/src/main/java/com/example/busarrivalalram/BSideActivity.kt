package com.example.busarrivalalram

import Model.BannerMessage
import Model.BusData
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

    // 버스 시간 가져오는 주기 (45초)
    val busTimeInterval: Long = 45000

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

                    binding.highestTemperature.text = "${weatherData.maxTemp}°"
                    binding.currentTemperature.text = "${weatherData.currentTemp}°"
                    binding.lowestTemperature.text = "${weatherData.minTemp}°"

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

        // 하단 배너 (좌측 기준 2/3 부분)
        // 정류장 내릴 떄 각 건물별 기준 몇 분 정도 걸리는지,
        // 그래서 건물 입구 기준으로 도착하는 최종 시간은 언제쯤인지 (이게 제일 좋아 보인다.)
        /*
        * 강의실까지 이동하는 시간, 예상 도착 시간 알려주기
            종합실험동, 평화의광장, 인문관
            종합실험동 : 제1공학관, 제2공학관, 제3공학관, 국제관
            평화의광장: 예술관, 국제관, 인문관, 상경관, 사범관
            인문관: 인문관, 상경관, 사범관, S/W관
        * */
        CoroutineScope(Dispatchers.Main).launch {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://2ot8ocxpaf.execute-api.ap-northeast-2.amazonaws.com/")
                .addConverterFactory(GsonConverterFactory.create()).build()
            val apiServiceBus = retrofit.create(APIServiceBus::class.java)

            while (true) {
                try {
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

                binding.bSideRecentRefreshTimestamp.text =
                    "최근 새로고침: ${DateTimeHandler.getCurrentTimeStamp()}"

                delay(busTimeInterval)
            }

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