package Utils

import android.content.Context
import android.media.MediaPlayer
import com.example.busarrivalalram.R

class AudioPlayer(private val context: Context) {
    private val audioList = arrayOf(
        R.raw.korean_24,
        R.raw.english_24,
        R.raw.korean_720_3,
        R.raw.english_720_3,
        R.raw.korean_shuttle,
        R.raw.english_shuttle
    )

    // indices
    private val audio24 = 0
    private val audio720_3 = 2
    private val audioShuttle = 4

    private var mediaPlayer: MediaPlayer? = null
    private var currentAudioIndex = 0

    private fun playCurrentAudio(startAudio: String) {
        if (currentAudioIndex < audioList.size) {
            mediaPlayer = MediaPlayer.create(context, audioList[currentAudioIndex])
            mediaPlayer?.setOnCompletionListener {
                currentAudioIndex++
            }
            mediaPlayer?.start()
        } else {
            // 모든 오디오 파일 재생이 완료된 경우
            currentAudioIndex = 0
        }
    }
}
