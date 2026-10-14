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

    fun destroy() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
    }
}
