package com.example.busarrivalalram

import Model.BannerMessage
import Model.CampusSchedule
import Model.WeatherData
import Utils.DateTimeHandler
import ViewModel.APIServiceBannerMessage
import ViewModel.APIServiceCampusSchedule
import ViewModel.APIServiceWeather
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import com.example.busarrivalalram.databinding.ActivityBsideBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BSideActivity : AppCompatActivity() {
    val binding by lazy { ActivityBsideBinding.inflate(layoutInflater) }

    // 현재 시간 변경 주기 (1초)
    val nowTimeInterval: Long = 1000

    // 격려 메시지 변경 주기 (기본깂 21600000 -> 30분)
    val encourageInterval: Long = 30 * 60 * 1000

    // 날씨 변경 주기 (기본깂 3600000 -> 1시간)
    val weatherInterval: Long = 60 * 60 * 1000

    // 학사일정 가져오는 주기 (24시간)
    val campusScheduleInterval: Long = 24 * 60 * 60 * 1000

    // 학사일정 뷰 순환 주기 (10초)
    val campusScheduleViewChangeInterval: Long = 7 * 1000

    // 학사일정 페이지당 보여줄 항목 개수
    val campusScheduleShowItemCount: Int = 10

    // 학사일정 데이터
    lateinit var campusSchedule: CampusSchedule

    // 사용할 폰트
    val font = R.font.ibm_plex_sans_kr_medium

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
                        toast.setText("${e.code()}, ${e.message()}")
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
                        toast.setText("${e.code()}, ${e.message()}")
                        toast.show()
                    }
                }
            }
        }

        // 학사일정 데이터 가져오는 코루틴
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                // ProgressBar 보이기
                binding.progressCircularThisMonthSchedule.visibility = View.VISIBLE
                binding.progressCircularNextMonthSchedule.visibility = View.VISIBLE

                try {
                    val retrofit = Retrofit.Builder()
                        .baseUrl("https://wmnmn0x75g.execute-api.ap-northeast-2.amazonaws.com/")
                        .addConverterFactory(GsonConverterFactory.create()).build()
                    val apiService = retrofit.create(APIServiceCampusSchedule::class.java)
                    campusSchedule = apiService.getCampusSchedule()

                } catch (e: HttpException) {
                    val toast = Toast(this@BSideActivity)
                    toast.setText("${e.code()}, ${e.message()}")
                }

                delay(campusScheduleInterval)
            }
        }

        // 학사일정 뷰 업데이트
        CoroutineScope(Dispatchers.Main).launch {
            var isShowingFirstThisMonthView: Boolean = false
            var isShowingFirstNextMonthView: Boolean = false

            while (true) {
                if (::campusSchedule.isInitialized && campusSchedule != null) {
                    if (campusSchedule.getThisMonthSchedule().size < campusScheduleShowItemCount) {
                        createFirstThisMonthView()
                    } else {
                        if (!isShowingFirstThisMonthView) {
                            createFirstThisMonthView()
                        } else {
                            createSecondThisMonthView()
                        }
                        isShowingFirstThisMonthView = !isShowingFirstThisMonthView
                    }

                    if (campusSchedule.getNextMonthSchedule().size < campusScheduleShowItemCount) {
                        createFirstNextMonthView()
                    } else {
                        if (!isShowingFirstNextMonthView) {
                            createFirstNextMonthView()
                        } else {
                            createSecondNextMonthView()
                        }
                        isShowingFirstNextMonthView = !isShowingFirstNextMonthView
                    }

                    if (binding.progressCircularThisMonthSchedule.visibility == View.VISIBLE && binding.progressCircularNextMonthSchedule.visibility == View.VISIBLE) {
                        // ProgressBar 숨기기
                        binding.progressCircularThisMonthSchedule.visibility = View.GONE
                        binding.progressCircularNextMonthSchedule.visibility = View.GONE
                    }

                    delay(campusScheduleViewChangeInterval)
                }

                delay(campusScheduleViewChangeInterval)
            }
        }
    }

    private fun createFirstThisMonthView() {
        if (::campusSchedule.isInitialized && campusSchedule != null) {
            val length = campusSchedule.getThisMonthSchedule().size / 2
            val thisSchedule = campusSchedule.getThisMonthSchedule().subList(0, length)

            // 업데이트하기 전에, Layout 내부의 뷰 모두 제거
            binding.campusScheduleThisMonthLayout.removeAllViews()

            // 1) 이번 달 학사일정 뷰 반영
            for (schedule in thisSchedule) {
                // Layout 생성
                val linearLayout = LinearLayout(this@BSideActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        // marginBottom 값을 R.dimen.dp_8에서 가져와 dp값으로 변환하여 할당
                        val margin = resources.getDimensionPixelSize(R.dimen.dimen_8dp)
                        this.setMargins(0, 0, 0, margin)
                    }
                    orientation = LinearLayout.HORIZONTAL
                }

                // Layout 내부에 들어갈 TextView 생성
                val dateTextView = TextView(this@BSideActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        resources.getDimensionPixelSize(R.dimen.dimen_b_side_schedule_date),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    text = schedule["date"]
                    setTextColor(
                        ActivityCompat.getColor(
                            this@BSideActivity, R.color.color_shuttle
                        )
                    )
                    setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        resources.getDimension(R.dimen.dimen_b_side_message_text_small)
                    )

                    val appFont = ResourcesCompat.getFont(this@BSideActivity, font)
                    typeface = appFont
                }
                linearLayout.addView(dateTextView)

                val titleTextView = TextView(this@BSideActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.END
                    }
                    text = schedule["content"]
                    setTextColor(ActivityCompat.getColor(this@BSideActivity, R.color.black))
                    setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        resources.getDimension(R.dimen.dimen_b_side_message_text_small)
                    )

                    val appFont = ResourcesCompat.getFont(this@BSideActivity, font)
                    typeface = appFont
                }
                linearLayout.addView(titleTextView)

                // 최종적으로 생성된 LinearLayout을 부모 LinearLayout에 추가
                binding.campusScheduleThisMonthLayout.addView(linearLayout)
            }
        }
    }

    private fun createFirstNextMonthView() {
        if (::campusSchedule.isInitialized && campusSchedule != null) {
            val length = campusSchedule.getNextMonthSchedule().size / 2
            val nextSchedule = campusSchedule.getNextMonthSchedule().subList(0, length)

            // 업데이트하기 전에, Layout 내부의 뷰 모두 제거
            binding.campusScheduleNextMonthLayout.removeAllViews()

            // 다음 달 학사일정 뷰 반영
            for (schedule in nextSchedule) {
                // Layout 생성
                val linearLayout = LinearLayout(this@BSideActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        // marginBottom 값을 R.dimen.dp_8에서 가져와 dp값으로 변환하여 할당
                        val margin = resources.getDimensionPixelSize(R.dimen.dimen_8dp)
                        this.setMargins(0, 0, 0, margin)
                    }
                    orientation = LinearLayout.HORIZONTAL
                }

                // Layout 내부에 들어갈 TextView 생성
                val dateTextView = TextView(this@BSideActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        resources.getDimensionPixelSize(R.dimen.dimen_b_side_schedule_date),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    text = schedule["date"]
                    setTextColor(
                        ActivityCompat.getColor(
                            this@BSideActivity, R.color.color_shuttle
                        )
                    )
                    setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        resources.getDimension(R.dimen.dimen_b_side_message_text_small)
                    )

                    val appFont = ResourcesCompat.getFont(this@BSideActivity, font)
                    typeface = appFont
                }
                linearLayout.addView(dateTextView)

                val titleTextView = TextView(this@BSideActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.END
                    }
                    text = schedule["content"]
                    setTextColor(ActivityCompat.getColor(this@BSideActivity, R.color.black))
                    setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        resources.getDimension(R.dimen.dimen_b_side_message_text_small)
                    )
                    val appFont = ResourcesCompat.getFont(this@BSideActivity, font)
                    typeface = appFont
                }
                linearLayout.addView(titleTextView)

                // 최종적으로 생성된 LinearLayout을 부모 LinearLayout에 추가
                binding.campusScheduleNextMonthLayout.addView(linearLayout)
            }
        }
    }

    private fun createSecondThisMonthView() {
        if (::campusSchedule.isInitialized && campusSchedule != null) {
            val size = campusSchedule.getThisMonthSchedule().size
            val thisSchedule = campusSchedule.getThisMonthSchedule().subList(size / 2, size)

            // 업데이트하기 전에, Layout 내부의 뷰 모두 제거
            binding.campusScheduleThisMonthLayout.removeAllViews()

            // 1) 이번 달 학사일정 뷰 반영
            for (schedule in thisSchedule) {
                // Layout 생성
                val linearLayout = LinearLayout(this@BSideActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        // marginBottom 값을 R.dimen.dp_8에서 가져와 dp값으로 변환하여 할당
                        val margin = resources.getDimensionPixelSize(R.dimen.dimen_8dp)
                        this.setMargins(0, 0, 0, margin)
                    }
                    orientation = LinearLayout.HORIZONTAL
                }

                // Layout 내부에 들어갈 TextView 생성
                val dateTextView = TextView(this@BSideActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        resources.getDimensionPixelSize(R.dimen.dimen_b_side_schedule_date),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    text = schedule["date"]
                    setTextColor(
                        ActivityCompat.getColor(
                            this@BSideActivity, R.color.color_shuttle
                        )
                    )
                    setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        resources.getDimension(R.dimen.dimen_b_side_message_text_small)
                    )
                    val appFont = ResourcesCompat.getFont(this@BSideActivity, font)
                    typeface = appFont
                }
                linearLayout.addView(dateTextView)

                val titleTextView = TextView(this@BSideActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.END
                    }
                    text = schedule["content"]
                    setTextColor(ActivityCompat.getColor(this@BSideActivity, R.color.black))
                    setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        resources.getDimension(R.dimen.dimen_b_side_message_text_small)
                    )
                    val appFont = ResourcesCompat.getFont(this@BSideActivity, font)
                    typeface = appFont
                }
                linearLayout.addView(titleTextView)

                // 최종적으로 생성된 LinearLayout을 부모 LinearLayout에 추가
                binding.campusScheduleThisMonthLayout.addView(linearLayout)
            }
        }
    }

    private fun createSecondNextMonthView() {
        if (::campusSchedule.isInitialized && campusSchedule != null) {
            val size = campusSchedule.getNextMonthSchedule().size
            val nextSchedule = campusSchedule.getNextMonthSchedule().subList(size / 2, size)

            // 업데이트하기 전에, Layout 내부의 뷰 모두 제거
            binding.campusScheduleNextMonthLayout.removeAllViews()

            // 다음 달 학사일정 뷰 반영
            for (schedule in nextSchedule) {
                // Layout 생성
                val linearLayout = LinearLayout(this@BSideActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        // marginBottom 값을 R.dimen.dp_8에서 가져와 dp값으로 변환하여 할당
                        val margin = resources.getDimensionPixelSize(R.dimen.dimen_8dp)
                        this.setMargins(0, 0, 0, margin)
                    }
                    orientation = LinearLayout.HORIZONTAL
                }

                // Layout 내부에 들어갈 TextView 생성
                val dateTextView = TextView(this@BSideActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        resources.getDimensionPixelSize(R.dimen.dimen_b_side_schedule_date),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    text = schedule["date"]
                    setTextColor(
                        ActivityCompat.getColor(
                            this@BSideActivity, R.color.color_shuttle
                        )
                    )
                    setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        resources.getDimension(R.dimen.dimen_b_side_message_text_small)
                    )
                    val appFont = ResourcesCompat.getFont(this@BSideActivity, font)
                    typeface = appFont
                }
                linearLayout.addView(dateTextView)

                val titleTextView = TextView(this@BSideActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.END
                    }
                    text = schedule["content"]
                    setTextColor(ActivityCompat.getColor(this@BSideActivity, R.color.black))
                    setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        resources.getDimension(R.dimen.dimen_b_side_message_text_small)
                    )
                    val appFont = ResourcesCompat.getFont(this@BSideActivity, font)
                    typeface = appFont
                }
                linearLayout.addView(titleTextView)

                // 최종적으로 생성된 LinearLayout을 부모 LinearLayout에 추가
                binding.campusScheduleNextMonthLayout.addView(linearLayout)
            }
        }
    }
}