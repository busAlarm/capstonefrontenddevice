package com.example.busarrivalalram

import Model.BusData
import Model.DateTime
import Model.DeviceStatus
import Utils.DateTimeHandler
import ViewModel.APIServiceBus
import ViewModel.APIServiceDeviceStatus
import android.annotation.SuppressLint
import android.app.ApplicationErrorReport.BatteryInfo
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.system.ErrnoException
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.content.res.ResourcesCompat
import com.example.busarrivalalram.databinding.ActivityAsideBinding
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class ASideActivity : AppCompatActivity() {
    val binding by lazy { ActivityAsideBinding.inflate(layoutInflater) }

    // 전원버튼 비활성화 위한 객체
    private lateinit var wifiBroadcastReceiver: WifiBroadcastReceiver

    // 전원버튼 비활성화 위한 객체
    private lateinit var powerButtonReceiver: BroadcastReceiver

    // 볼륨버튼 비활성화 위한 객체
    private lateinit var volumeButtonReceiver: BroadcastReceiver

    // 버스 도착 시간 갱신 주기 (30초)
    val busTimeInterval: Long = 30 * 1000

    // 요청 재시도 시간
    val requestRetryTime: Long = 2500

    // 상단 곧도착 노선 뷰에 들어갈 폰트
    private val font = R.font.ibm_plex_sans_kr_medium

    // 오디오 재생을 위한 객체
    private var mediaPlayer: MediaPlayer? = null
    private var isMediaPlayerReleased = false

    // 곧도착 버스 목록 큐 (24, 720-3, 셔틀)
    val arrivalSoonBusQueue: ArrayDeque<String> = ArrayDeque()

    // 현재 뷰에 추가된 버스 목록 큐
    val arrivalSoonBusNowAddedQueue: ArrayDeque<String> = ArrayDeque()

    // 버스 노선별 도착 데이터
    var arrivalInfo24: BusData? = null
    var arrivalInfo720_3: BusData? = null
    var arrivalInfoShuttle: BusData? = null

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(binding.root)

        // progress bar 숨기기
        binding.progressCircular.visibility = View.GONE

        // title bar 가리기
        supportActionBar?.hide()

        // full screen
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        // wifi 리시버 등록
        val wifiFilter = IntentFilter()
        wifiBroadcastReceiver = WifiBroadcastReceiver()
        wifiFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        registerReceiver(wifiBroadcastReceiver, wifiFilter)

        // 전원버튼 동작 비활성화
        // Power 버튼 이벤트를 가로채는 BroadcastReceiver 등록
        val powerButtonFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        powerButtonReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // 전원 버튼 이벤트를 무시하고 아무 동작도 수행하지 않음
                abortBroadcast()
            }
        }
        registerReceiver(powerButtonReceiver, powerButtonFilter)

        // Volume 버튼 이벤트를 가로채는 BroadcastReceiver 등록
        val volumeButtonFilter = IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
            priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        }
        volumeButtonReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val keyCode = intent?.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                if (keyCode == AudioManager.STREAM_RING || keyCode == AudioManager.STREAM_MUSIC) {
                    // 볼륨 버튼 이벤트를 무시하고 아무 동작도 수행하지 않음
                    abortBroadcast()
                }
            }
        }
        registerReceiver(volumeButtonReceiver, volumeButtonFilter)


        // 곧도착 옆 모든 자식 뷰 삭제
        val busArrivalLayoutGroup = binding.arrivalSoonLayout
        busArrivalLayoutGroup?.removeAllViews()
        val busArrivalItemLayout = binding.arrivalSoonItem
        busArrivalItemLayout?.removeAllViews()

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
            while (true) {
                try {
                    // Progressbar 보이기
                    binding.progressCircular.visibility = View.VISIBLE

                    // 1. API에 요청하여 값 가져오기
                    val retrofit = Retrofit.Builder()
                        .baseUrl("https://2ot8ocxpaf.execute-api.ap-northeast-2.amazonaws.com/")
                        .addConverterFactory(GsonConverterFactory.create()).build()

                    val apiServiceBus = retrofit.create(APIServiceBus::class.java)

                    arrivalInfo24 = apiServiceBus.getBusArrivalInfo("24").checkArrival()
                    arrivalInfo720_3 = apiServiceBus.getBusArrivalInfo("720-3").checkArrival()
                    arrivalInfoShuttle = apiServiceBus.getBusArrivalInfo("shuttle").checkArrival()

                    // 2. 가져온 값으로 뷰 갱신하기
                    // 2-1-1. 가져온 값으로 24번 도착 시간 항목 갱신
                    if (!arrivalInfo24!!.arrivalSoon && arrivalInfo24!!.predictTime1.toInt() >= 0) {
                        binding.currentArrivalTime24.text = "${arrivalInfo24!!.predictTime1} 분"
                        binding.currentArrivalTime24.setTextColor(Color.BLACK)
                        binding.currentArrivalStation24.setTextColor(Color.BLACK)

                        // 곧도착에 뷰가 있다면 제거
                        if (arrivalSoonBusNowAddedQueue.contains("24")) {
                            busArrivalLayoutGroup?.removeViewAt(
                                arrivalSoonBusNowAddedQueue.indexOf(
                                    "24"
                                )
                            )
                            arrivalSoonBusNowAddedQueue.remove("24")
                        }

                    } else if (arrivalInfo24!!.predictTime1.toInt() >= 0) {
                        // 남은 시간이 2분 이하일 때, 곧도착으로 남은 시간 변경
                        binding.currentArrivalTime24.text = "곧도착"
                        binding.currentArrivalTime24.setTextColor(
                            ContextCompat.getColor(
                                this@ASideActivity, R.color.color_arrival_soon
                            )
                        )
                        binding.currentArrivalStation24.setTextColor(Color.BLACK)

                        // 뷰에 추가되지 않았을 때에만 뷰 생성할 목록에 추가
                        if (!arrivalSoonBusNowAddedQueue.contains("24")) {
                            arrivalSoonBusQueue.add("24")
                        }

                    } else {
                        binding.currentArrivalTime24.text = "도착 정보 없음"
                        binding.currentArrivalTime24.setTextColor(Color.GRAY)
                        binding.currentArrivalStation24.setTextColor(Color.GRAY)
                    }

                    if (arrivalInfo24!!.predictTime2.toInt() >= 0) {
                        binding.nextArrivalTime24.text = "${arrivalInfo24!!.predictTime2} 분"
                        binding.nextArrivalTime24.setTextColor(Color.BLACK)
                        binding.nextArrivalStation24.setTextColor(Color.BLACK)
                    } else {
                        binding.nextArrivalTime24.text = "도착 정보 없음"
                        binding.nextArrivalTime24.setTextColor(Color.GRAY)
                        binding.nextArrivalStation24.setTextColor(Color.GRAY)
                    }

                    binding.currentArrivalStation24.text = arrivalInfo24!!.stationNm1
                    binding.nextArrivalStation24.text = arrivalInfo24!!.stationNm2


                    // 2-2. 가져온 값으로 720-3번 항목 갱신
                    // 남은 시간이 2분 이하일 때, 곧도착으로 남은 시간 변경
                    if (!arrivalInfo720_3!!.arrivalSoon && arrivalInfo720_3!!.predictTime1.toInt() >= 0) {
                        binding.currentArrivalTime7203.text = "${arrivalInfo720_3!!.predictTime1} 분"
                        binding.currentArrivalTime7203.setTextColor(Color.BLACK)
                        binding.currentArrivalStation7203.setTextColor(Color.BLACK)

                        // 곧도착에 뷰가 있다면 제거
                        if (arrivalSoonBusNowAddedQueue.contains("720-3")) {
                            busArrivalLayoutGroup?.removeViewAt(
                                arrivalSoonBusNowAddedQueue.indexOf(
                                    "720-3"
                                )
                            )
                            arrivalSoonBusNowAddedQueue.remove("720-3")
                        }

                    } else if (arrivalInfo720_3!!.predictTime1.toInt() >= 0) {
                        binding.currentArrivalTime7203.text = "곧도착"
                        binding.currentArrivalTime7203.setTextColor(
                            ContextCompat.getColor(
                                this@ASideActivity, R.color.color_arrival_soon
                            )
                        )
                        binding.currentArrivalStation7203.setTextColor(Color.BLACK)

                        // 뷰에 추가되지 않았을 때에만 뷰 생성할 목록에 추가
                        if (!arrivalSoonBusNowAddedQueue.contains("720-3")) {
                            arrivalSoonBusQueue.add("720-3")
                        }

                    } else {
                        binding.currentArrivalTime7203.text = "도착 정보 없음"
                        binding.currentArrivalTime7203.setTextColor(Color.GRAY)
                        binding.currentArrivalStation7203.setTextColor(Color.GRAY)
                    }

                    if (arrivalInfo720_3!!.predictTime2.toInt() >= 0) {
                        binding.nextArrivalTime7203.text = "${arrivalInfo720_3!!.predictTime2} 분"
                        binding.nextArrivalTime7203.setTextColor(Color.BLACK)
                        binding.nextArrivalStation7203.setTextColor(Color.BLACK)
                    } else {
                        binding.nextArrivalTime7203.text = "도착 정보 없음"
                        binding.nextArrivalTime7203.setTextColor(Color.GRAY)
                        binding.nextArrivalStation7203.setTextColor(Color.GRAY)
                    }

                    binding.currentArrivalStation7203.text = arrivalInfo720_3!!.stationNm1
                    binding.nextArrivalStation7203.text = arrivalInfo720_3!!.stationNm2


                    // 2-3. 가져온 값으로 셔틀 항목 갱신
                    // 남은 시간이 2분 이하일 때, 곧도착으로 남은 시간 변경
                    if (arrivalInfoShuttle!!.isRunning) {
                        if (!arrivalInfoShuttle!!.arrivalSoon && arrivalInfoShuttle!!.predictTime1.toInt() >= 0) {
                            binding.currentArrivalTimeShuttle.text =
                                "${arrivalInfoShuttle!!.predictTime1} 분 후 도착 예상"
                            binding.currentArrivalTimeShuttle.setTextColor(Color.BLACK)

                            // 곧도착에 뷰가 있다면 제거
                            if (arrivalSoonBusNowAddedQueue.contains("셔틀")) {
                                busArrivalLayoutGroup?.removeViewAt(
                                    arrivalSoonBusNowAddedQueue.indexOf(
                                        "셔틀"
                                    )
                                )
                                arrivalSoonBusNowAddedQueue.remove("셔틀")
                            }

                        } else if (arrivalInfoShuttle!!.predictTime1.toInt() >= 0) {
                            binding.currentArrivalTimeShuttle.text = "곧도착 예상"
                            binding.currentArrivalTimeShuttle.setTextColor(
                                ContextCompat.getColor(
                                    this@ASideActivity, R.color.color_arrival_soon
                                )
                            )

                            // 뷰에 추가되지 않았을 때에만 뷰 생성할 목록에 추가
                            if (!arrivalSoonBusNowAddedQueue.contains("셔틀")) {
                                arrivalSoonBusQueue.add("셔틀")
                            }
                        }

                        if (arrivalInfoShuttle!!.predictTime2.toInt() >= 0) {
                            binding.nextArrivalTimeShuttle.text =
                                "${arrivalInfoShuttle!!.predictTime2} 분 후 도착 예상"
                            binding.nextArrivalTimeShuttle.setTextColor(Color.BLACK)
                        } else {
                            binding.nextArrivalTimeShuttle.text = "도착 정보 없음"
                            binding.nextArrivalTimeShuttle.setTextColor(Color.GRAY)
                        }
                    } else {
                        binding.currentArrivalTimeShuttle.text = "도착 정보 없음"
                        binding.currentArrivalTimeShuttle.setTextColor(Color.GRAY)
                    }


                    // 3. 곧도착 옆 도착 예정 노선 목록 갱신
                    // * 곧도착 옆에 있는 노선이 사라지는 기준은 남은 시간이 10초 이하일 때

                    // 큐를 다 비울 때까지 ViewGroup에 전부 추가
                    while (arrivalSoonBusQueue.isNotEmpty()) {
                        var addedBusName: String = arrivalSoonBusQueue.removeFirst()

                        var busArrivalItemLayout = LinearLayout(this@ASideActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                resources.getDimensionPixelSize(R.dimen.dimen_240dp),
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
                            textSize = 70F
                            gravity = Gravity.CENTER
                            setTextColor(Color.BLACK)
                            setTypeface(typeface, Typeface.BOLD)

                            val appFont = ResourcesCompat.getFont(this@ASideActivity, font)
                            typeface = appFont
                        }

                        busArrivalItemLayout?.addView(busArrivalItemView)
                        busArrivalLayoutGroup?.addView(busArrivalItemLayout)

                        arrivalSoonBusNowAddedQueue.add(addedBusName)
                    }

//                    arrivalSoonBusNowAddedQueue.add("24")   // debug


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
                                                R.raw.korean_24, R.raw.english_24
                                            )
                                        )
                                    }

                                    "720-3" -> {
                                        playlist = playlist.plus(
                                            arrayOf(
                                                R.raw.korean_720_3, R.raw.english_720_3
                                            )
                                        )
                                    }

                                    "셔틀" -> {
                                        playlist = playlist.plus(
                                            arrayOf(
                                                R.raw.korean_shuttle, R.raw.english_shuttle
                                            )
                                        )
                                    }
                                }
                            }

                            mediaPlayer =
                                MediaPlayer.create(this@ASideActivity, playlist[currentPlayedIndex])

                            mediaPlayer?.setOnCompletionListener {
                                if (currentPlayedIndex < playlist.size - 1) {
                                    currentPlayedIndex++
                                    mediaPlayer?.reset()
                                    mediaPlayer?.setDataSource(
                                        this@ASideActivity,
                                        Uri.parse("android.resource://${packageName}/${playlist[currentPlayedIndex]}")
                                    )
                                    mediaPlayer?.prepare()
                                    mediaPlayer?.start()
                                } else {
                                    mediaPlayer?.stop()
                                    mediaPlayer?.release()
                                }
                            }
                            mediaPlayer?.start()
                        }
                    }

                    // 5. 최근 새로고침 변경
                    binding.aSideRecentRefreshTimestamp.text =
                        "최근 새로고침: ${DateTimeHandler.getCurrentTimeStamp()}"

                } catch (e: HttpException) {
                    if (e.code() != 200) {
                        // API 연결 오류 시 Toast 출력
                        val toast = Toast(this@ASideActivity)
                        toast.setText("${e.code()}, ${e.message()}")
                        toast.show()

                        delay(requestRetryTime)
                        continue
                    }
                } catch (e: Exception) {
                    if (e is ErrnoException) {
                        // API 연결 오류 시 Toast 출력
                        val toast = Toast(this@ASideActivity)
                        toast.setText("네트워크 연결이 끊어졌습니다. 잠시 기다리세요...")
                        toast.show()

                        delay(requestRetryTime)
                        continue
                    }
                }

                // Progressbar 숨기기
                binding.progressCircular.visibility = View.GONE

                // 다음 요청 시간까지 대기
                delay(busTimeInterval)
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            // TO DO: 서버 주소 변경
            val retrofit = Retrofit.Builder().baseUrl("http://43.201.109.211:8080/api/")
                .addConverterFactory(GsonConverterFactory.create()).build()
            val apiServiceBus = retrofit.create(APIServiceBus::class.java)

            while (true) {
                try {
                    if (arrivalInfo24 != null) {
                        Log.d("busData24", arrivalInfo24!!.toString())
                        val call = apiServiceBus.postBusArrivalInfo(arrivalInfo24!!)
                        val response = call.execute()
                        if (response.isSuccessful) {
                            Log.d("response24", "success")
                        } else {
                            Log.d("response24", "failure: ${response.message()}")
                        }

                        Log.d("response24", response.toString())
                    }

                    if (arrivalInfo720_3 != null) {
                        Log.d("busData7203", arrivalInfo720_3!!.toString())
                        val call = apiServiceBus.postBusArrivalInfo(arrivalInfo720_3!!)
                        val response = call.execute()
                        Log.d("response720_3", response.toString())
                    }

                    if (arrivalInfoShuttle != null) {
                        Log.d("busDataShuttle", arrivalInfoShuttle!!.toString())
                        val call = apiServiceBus.postBusArrivalInfo(arrivalInfoShuttle!!)
                        val response = call.execute()
                        Log.d("responseShuttle", response.toString())
                    }

                } catch (e: HttpException) {
                    if (e.code() != 200) {
                        // API 연결 오류 시 Toast 출력
                        val toast = Toast(this@ASideActivity)
                        toast.setText("${e.code()}, ${e.message()}")
                        toast.show()

                        Log.d("response_exception", e.toString())

                        delay(requestRetryTime)
                        continue
                    }
                } catch (e: Exception) {
                    if (e is ErrnoException) {
                        // API 연결 오류 시 Toast 출력
                        val toast = Toast(this@ASideActivity)
                        toast.setText("네트워크 연결이 끊어졌습니다. 잠시 기다리세요...")
                        toast.show()

                        Log.d("response_exception", e.toString())

                        delay(requestRetryTime)
                        continue
                    }
                }

                delay(busTimeInterval)
            }
        }

        // 기기 상태 정보 전달
        CoroutineScope(Dispatchers.Main).launch {
            val contentResolver: ContentResolver = getContentResolver() // for brightness
            val wifiManager: WifiManager =
                applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val batteryManager: BatteryManager =
                applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

            val retrofit = Retrofit.Builder().baseUrl("http://43.201.109.211:8080/api/")
                .addConverterFactory(GsonConverterFactory.create()).build()
            val apiServiceDeviceStatus = retrofit.create(APIServiceDeviceStatus::class.java)

            while (true) {
                try {
                    val wifiInfo: WifiInfo = wifiManager.connectionInfo
                    val ssid: String = wifiInfo.ssid
                    val brightness: Int = (Settings.System.getInt(
                        contentResolver, Settings.System.SCREEN_BRIGHTNESS
                    ) / 255.0 * 100).toInt()
                    val signalPower: Int = wifiInfo.rssi
                    val batteryPercent: Int =
                        batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    val batteryAmpere: Int =
                        batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) / 1000
                    val timeStamp: String = DateTimeHandler.getCurrentTimeStamp()

                    // 서버에 상태값 전달
                    val deviceStatus = DeviceStatus(
                        "A",
                        timeStamp,
                        ssid,
                        "$brightness",
                        "$signalPower",
                        "$batteryPercent",
                        "$batteryAmpere",
                        "[$timeStamp]: Connected to $ssid / " + "Brightness ${brightness}% / " + "Current signal power: ${signalPower}dBm / " + "Current battery percent: ${batteryPercent}% / " + "Current battery ampere: ${batteryAmpere}mAh"
                    )

                    Log.d("deviceA", deviceStatus.logMessage)
                    val call = apiServiceDeviceStatus.postDeviceStatus(deviceStatus)
                    val response = call.execute()
                    Log.d("responseDevice", response.toString())

                } catch (e: HttpException) {
                    if (e.code() != 200) {
                        // API 연결 오류 시 Toast 출력
                        val toast = Toast(this@ASideActivity)
                        toast.setText("${e.code()}, ${e.message()}")
                        toast.show()

                        delay(requestRetryTime)
                        continue
                    }
                } catch (e: Exception) {
                    if (e is ErrnoException) {
                        // API 연결 오류 시 Toast 출력
                        val toast = Toast(this@ASideActivity)
                        toast.setText("네트워크 연결이 끊어졌습니다. 잠시 기다리세요...")
                        toast.show()

                        delay(requestRetryTime)
                        continue
                    }
                }

                delay(busTimeInterval)
            }
        }
    }

    // 액티비티 실행되는 동안 터치 이벤트 소비
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return true
    }

    override fun onPause() {
        super.onPause()

//        pauseMediaPlayerAsync()

        arrivalSoonBusQueue.clear()
        arrivalSoonBusNowAddedQueue.clear()
    }

    private fun pauseMediaPlayerAsync() = CoroutineScope(Dispatchers.Main).launch {
        val mediaPlayerToPause = mediaPlayer // 현재 mediaPlayer 객체를 가져옴
        mediaPlayer = null // mediaPlayer 객체를 null로 설정하여 재생을 중단함

        if (!isMediaPlayerReleased && mediaPlayerToPause?.isPlaying == true) {
            mediaPlayerToPause.pause()
        }
        mediaPlayerToPause?.release()
        isMediaPlayerReleased = true
    }

    override fun onDestroy() {
        super.onDestroy()

        // wifi 리시버
        unregisterReceiver(wifiBroadcastReceiver)

        // 전원버튼 비활성화하는 BroadcastReceiver 등록 해제
        unregisterReceiver(powerButtonReceiver)

        // 볼륨버튼 비활성화하는 BroadcastReceiver 등록 해제
        unregisterReceiver(volumeButtonReceiver)
    }
}