package com.fittrack.app.service

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class FitTrackVoiceCoach(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false
    var isMuted = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val viLocale = Locale("vi", "VN")
            val result = tts?.setLanguage(viLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to English if Vietnamese voice data is not installed on device
                tts?.setLanguage(Locale.US)
            }
            tts?.setSpeechRate(1.0f)
            tts?.setPitch(1.0f)
            isInitialized = true
        }
    }

    fun speak(text: String) {
        if (isMuted || !isInitialized || tts == null) return
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "fittrack_speech")
        } catch (_: Exception) {}
    }

    fun speakStartExercise(exerciseName: String, durationSec: Int) {
        speak("Bắt đầu bài tập $exerciseName. $durationSec giây!")
    }

    fun speakHalfway() {
        speak("Đã qua nửa thời gian, cố lên nào!")
    }

    fun speakCountdown(sec: Int) {
        if (sec in 1..3) {
            speak("$sec")
        }
    }

    fun speakRest(restSec: Int, nextName: String) {
        speak("Nghỉ ngơi $restSec giây. Chuẩn bị bài $nextName.")
    }

    fun speakComplete(calories: Int) {
        speak("Xuất sắc! Buổi tập đã hoàn thành. Bạn đã đốt cháy $calories calo.")
    }

    fun speakKmMilestone(km: Int, paceSec: Long, calories: Int) {
        val pm = paceSec / 60
        val ps = paceSec % 60
        speak("Bạn đã hoàn thành $km kilômét! Pace $pm phút $ps giây. Đã đốt $calories calo. Cố lên nào!")
    }

    // PRE-RUN COUNTDOWN & ROADMAP AUDIO ALERTS
    fun speakRunCountdown(sec: Int) {
        when (sec) {
            3 -> speak("Ba")
            2 -> speak("Hai")
            1 -> speak("Một")
            0 -> speak("Bắt đầu chạy!")
        }
    }

    fun speakOffTrackWarning(distMeters: Int) {
        speak("Chú ý! Bạn đang chạy chệch khỏi lộ trình $distMeters mét!")
    }

    fun speakBackOnTrack() {
        speak("Bạn đã quay lại lộ trình đã chọn. Tiếp tục nào!")
    }

    fun speakRoadmapCompleted() {
        speak("Chúc mừng! Bạn đã hoàn thành toàn bộ lộ trình mục tiêu!")
    }

    // ZOMBIE CHASE AUDIO ALERTS
    fun speakZombieStart(difficultyTitle: String) {
        speak("Kích hoạt chế độ sinh tồn! Đàn $difficultyTitle đang ở sau lưng bạn 80 mét. Chạy ngay, đừng dừng lại!")
    }

    fun speakZombieApproaching(meters: Int) {
        speak("Cảnh báo! Zombie cách $meters mét! Tăng tốc ngay!")
    }

    fun speakZombieDanger() {
        speak("Nguy hiểm cực độ! Zombie ngay sát sau lưng, bứt tốc ngay!")
    }

    fun speakZombieEscaped() {
        speak("Rất tốt! Bạn vừa bứt tốc nới rộng khoảng cách an toàn!")
    }

    fun speakZombieCaught() {
        speak("Bạn đã bị zombie bắt kịp! Tăng tốc thoát hiểm ngay lập tức!")
    }

    fun destroy() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
    }
}
