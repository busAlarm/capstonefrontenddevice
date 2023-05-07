package com.example.busarrivalalram

import Model.BusDataGG
import Utils.DateTimeHandler
import ViewModel.APIServiceGG
import ViewModel.APIServiceShuttle
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore.Audio.Media
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.example.busarrivalalram.databinding.ActivityAsideBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ASideActivity : AppCompatActivity() {
    val binding by lazy { ActivityAsideBinding.inflate(layoutInflater) }

    // 오디오 재생을 위한 객체
    lateinit var mediaPlayer: MediaPlayer

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(binding.root)

        // title bar 가리기
        supportActionBar?.hide()

        // full screen
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        // 곧도착 옆 모든 자식 뷰 삭제
        val busArrivalLayoutGroup = binding.arrivalSoonLayout
        busArrivalLayoutGroup.removeAllViews()
        val busArrivalItemLayout = binding.arrivalSoonItem
        busArrivalItemLayout.removeAllViews()

        // GlobalScope를 사용하는 것이 적합하다.
        // 앱이 실행되는 내내 반복적인 작업을 수행할 때 사용하기 때문이다.

        // 코루틴 어떻게 돌리는 게 좋을지 생각해보자
        // 1. 시간 확인 (무한루프)
        // 2. 특정 시간대 (적어도 한 대 이상의 버스 노선이 운행중인 시간일 때) -> 30초마다 요청 보냄
        //  요청 보내서 받은 결과에 따라 예외처리 or 뷰 갱신
        //  뷰 갱신하면서 도착 시간이 한 정거장 이내 또는 2분 이내일 경우, 곧도착으로 취급하면서 TTS 안내 음성 출력
        //      -> 음성 출력 시 withContext(Dispachers.IO) {} 블록 안에서 출력해야 한다.
        //  뷰 갱신은 곧도착인 경우와 도착 예정 정보 없는 경우, 그리고 그 외 정보가 있는 경우

        // CoroutineScope에서 돌아가는 코드는 메인 스레드에서 돌아가는 코드.
        // -> 뷰와 관련된 작업은 백그라운드 스레드에서 돌아가게 해서는 안 된다.
        // GlobalScope에서 돌아가는 코드는 백그라운드 스레드에서 돌아가는 코드다.
        // 따라서, 뷰를 건드리는 코드를 GlobalScope 안에 넣으면 안 된다.
        CoroutineScope(Dispatchers.Main).launch {
            // 곧도착 버스 목록 큐 (24, 720-3, 셔틀)
            val arrivalSoonBusQueue: ArrayDeque<String> = ArrayDeque()
            // 현재 뷰에 추가된 버스 목록 큐
            val arrivalSoonBusNowAddedQueue: ArrayDeque<String> = ArrayDeque()

            while (true) {
                try {
                    // 1. API에 요청하여 값 가져오기
                    val retrofit = Retrofit.Builder()
                        .baseUrl("https://2ot8ocxpaf.execute-api.ap-northeast-2.amazonaws.com/")
                        .addConverterFactory(GsonConverterFactory.create()).build()

                    val apiServiceGG = retrofit.create(APIServiceGG::class.java)
                    val apiServiceShuttle = retrofit.create(APIServiceShuttle::class.java)

                    val arrivalInfo24 = apiServiceGG.getBusArrivalInfo("24")
                    val arrivalInfo720_3 = apiServiceGG.getBusArrivalInfo("720-3")
                    val arrivalInfoShuttle = apiServiceShuttle.getBusArrivalInfo("shuttle")

                    // 1-1. 요청하여 받은 값 확인
                    Log.d(
                        "responseResult",
                        "${arrivalInfo24.currentStation} ${arrivalInfo24.timeRemaining}"
                    )

                    // 2. 가져온 값으로 뷰 갱신하기
                    // 2-1. 가져온 값으로 24번 항목 갱신
                    binding.current24.text = arrivalInfo24.currentStation
                    // 남은 시간이 2분 이하일 때, 곧도착으로 남은 시간 변경
                    if (arrivalInfo24.timeRemaining > 120) {
                        binding.remaining24.text = "${arrivalInfo24.timeRemaining / 60} 분"
                        binding.remaining24.setTextColor(Color.BLACK)

                        // 곧도착에 뷰가 있다면 제거
                        if (arrivalSoonBusNowAddedQueue.contains("24")) {
                            busArrivalLayoutGroup.removeViewAt(arrivalSoonBusNowAddedQueue.indexOf("24"))
                            arrivalSoonBusNowAddedQueue.remove("24")
                        }

                    } else if (arrivalInfo24.timeRemaining >= 0) {
                        binding.remaining24.text = "곧도착"
                        binding.remaining24.setTextColor(
                            ContextCompat.getColor(
                                this@ASideActivity, R.color.color_arrival_soon
                            )
                        )

                        // 뷰에 추가되지 않았을 때에만 뷰 생성할 목록에 추가
                        if (!arrivalSoonBusNowAddedQueue.contains("24")) {
                            arrivalSoonBusQueue.add("24")
                        }

                    } else {
                        binding.current24.text = "도착 정보 없음"
                        binding.remaining24.text = "도착 정보 없음"
                        binding.current24.setTextColor(Color.BLACK)
                        binding.remaining24.setTextColor(Color.BLACK)
                    }

                    // 2-2. 가져온 값으로 720-3번 항목 갱신
                    binding.current7203.text = arrivalInfo720_3.currentStation
                    // 남은 시간이 2분 이하일 때, 곧도착으로 남은 시간 변경
                    if (arrivalInfo720_3.timeRemaining > 120) {
                        binding.remaining7203.text = "${arrivalInfo720_3.timeRemaining / 60} 분"
                        binding.remaining7203.setTextColor(Color.BLACK)

                        // 곧도착에 뷰가 있다면 제거
                        if (arrivalSoonBusNowAddedQueue.contains("720-3")) {
                            busArrivalLayoutGroup.removeViewAt(arrivalSoonBusNowAddedQueue.indexOf("720-3"))
                            arrivalSoonBusNowAddedQueue.remove("720-3")
                        }

                    } else if (arrivalInfo720_3.timeRemaining >= 0) {
                        binding.remaining7203.text = "곧도착"
                        binding.remaining7203.setTextColor(
                            ContextCompat.getColor(
                                this@ASideActivity, R.color.color_arrival_soon
                            )
                        )

                        // 뷰에 추가되지 않았을 때에만 뷰 생성할 목록에 추가
                        if (!arrivalSoonBusNowAddedQueue.contains("720-3")) {
                            arrivalSoonBusQueue.add("720-3")
                        }

                    } else {
                        binding.current7203.text = "도착 정보 없음"
                        binding.remaining7203.text = "도착 정보 없음"
                        binding.current7203.setTextColor(Color.BLACK)
                        binding.remaining7203.setTextColor(Color.BLACK)
                    }

                    // 2-3. 가져온 값으로 셔틀 항목 갱신
                    binding.currentShuttle.text =
                        "${arrivalInfoShuttle.estimateDepartureTime / 60} 분 전 출발 예상"
                    // 남은 시간이 2분 이하일 때, 곧도착으로 남은 시간 변경
                    if (arrivalInfoShuttle.estimateArrivalTime > 120) {
                        binding.remainingShuttle.text =
                            "${arrivalInfoShuttle.estimateArrivalTime / 60} 분 후 도착 예상"
                        binding.remainingShuttle.setTextColor(Color.BLACK)

                        // 곧도착에 뷰가 있다면 제거
                        if (arrivalSoonBusNowAddedQueue.contains("셔틀")) {
                            busArrivalLayoutGroup.removeViewAt(arrivalSoonBusNowAddedQueue.indexOf("셔틀"))
                            arrivalSoonBusNowAddedQueue.remove("셔틀")
                        }

                    } else if (arrivalInfoShuttle.estimateArrivalTime >= 0) {
                        binding.remainingShuttle.text = "곧도착 예상"
                        binding.remainingShuttle.setTextColor(
                            ContextCompat.getColor(
                                this@ASideActivity, R.color.color_arrival_soon
                            )
                        )

                        // 뷰에 추가되지 않았을 때에만 뷰 생성할 목록에 추가
                        if (!arrivalSoonBusNowAddedQueue.contains("셔틀")) {
                            arrivalSoonBusQueue.add("셔틀")
                        }

                    } else {
                        binding.currentShuttle.text = "도착 정보 없음"
                        binding.remainingShuttle.text = "도착 정보 없음"
                        binding.currentShuttle.setTextColor(Color.BLACK)
                        binding.remainingShuttle.setTextColor(Color.BLACK)
                    }

                    // 3. 곧도착 옆 도착 예정 노선 목록 갱신
                    // * 곧도착 옆에 있는 노선이 사라지는 기준은 남은 시간이 10초 이하일 때

                    // 큐를 다 비울 때까지 ViewGroup에 전부 추가
                    while (arrivalSoonBusQueue.isNotEmpty()) {
                        var addedBusName: String = arrivalSoonBusQueue.removeFirst()

                        var busArrivalItemLayout = LinearLayout(this@ASideActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                resources.getDimensionPixelSize(R.dimen.dimen_200dp),
                                resources.getDimensionPixelSize(R.dimen.dimen_140dp)
                            ).apply {
                                marginStart = resources.getDimensionPixelSize(R.dimen.dimen_20dp)
                                marginEnd = resources.getDimensionPixelSize(R.dimen.dimen_20dp)
                            }
                            background = ContextCompat.getDrawable(context, R.drawable.border)
                            gravity = Gravity.CENTER
                        }

                        var busArrivalItemView = TextView(this@ASideActivity).apply {
                            text = addedBusName
                            textSize = 50F
                            setTextColor(Color.BLACK)
                            setTypeface(typeface, Typeface.BOLD)
                        }

                        busArrivalItemLayout.addView(busArrivalItemView)
                        busArrivalLayoutGroup.addView(busArrivalItemLayout)

                        arrivalSoonBusNowAddedQueue.add(addedBusName)
                    }


                    // 4. 음성 안내 메시지 출력
                    // TTS 음성 구할 것 (한국어, 영어)
                    // 한국어 -> 영어 순으로 출력
                    if (arrivalSoonBusNowAddedQueue.isNotEmpty()) {
                        withContext(Dispatchers.IO) {
                            var playlist: Array<Int> = arrayOf()
                            var currentPlayedIndex = 0

                            // 음성 출력
                            for (arrival in arrivalSoonBusNowAddedQueue) {
                                when (arrival) {
                                    "24" -> {
                                        playlist = playlist.plus(
                                            arrayOf(
                                                R.raw.korean_24,
                                                R.raw.english_24
                                            )
                                        )
                                    }

                                    "720-3" -> {
                                        playlist = playlist.plus(
                                            arrayOf(
                                                R.raw.korean_720_3,
                                                R.raw.english_720_3
                                            )
                                        )
                                    }

                                    "셔틀" -> {
                                        playlist = playlist.plus(
                                            arrayOf(
                                                R.raw.korean_shuttle,
                                                R.raw.english_shuttle
                                            )
                                        )
                                    }
                                }
                            }

                            mediaPlayer =
                                MediaPlayer.create(this@ASideActivity, playlist[currentPlayedIndex])
                            mediaPlayer.setOnCompletionListener {
                                if (currentPlayedIndex < playlist.size - 1) {
                                    currentPlayedIndex++
                                    mediaPlayer.reset()
                                    mediaPlayer.setDataSource(
                                        this@ASideActivity,
                                        Uri.parse("android.resource://${packageName}/${playlist[currentPlayedIndex]}")
                                    )
                                    mediaPlayer.prepare()
                                    mediaPlayer.start()
                                } else {
                                    mediaPlayer.stop()
                                    mediaPlayer.release()
                                }
                            }
                            mediaPlayer.start()
                        }
                    }

                    // 5. 최근 새로고침 변경
                    binding.recentRefreshTimestamp.text =
                        "최근 새로고침: ${DateTimeHandler.getCurrentTimeStamp()}"

                } catch (e: HttpException) {
                    if (e.code() != 200) {
                        // API 연결 오류 시 Toast 출력
                        val toast = Toast(this@ASideActivity)
                        toast.setText("${e.code()}")
                        toast.show()
                    }
                }

                delay(30000) // temp. actual interval time is 30000 mills
            }
        }
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.stop()
        mediaPlayer?.release()
    }
}