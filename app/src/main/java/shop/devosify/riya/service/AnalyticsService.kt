package shop.devosify.riya.service

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsService @Inject constructor(
    @ApplicationContext context: Context
) {
    private val analytics = FirebaseAnalytics.getInstance(context)

    fun logVoiceInteraction(
        transcriptionSuccess: Boolean,
        responseSuccess: Boolean,
        duration: Long
    ) {
        analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT) {
            param("interaction_type", "voice")
            param("transcription_success", transcriptionSuccess)
            param("response_success", responseSuccess)
            param("duration_ms", duration)
        }
    }

    fun logError(
        errorType: String,
        errorMessage: String,
        stackTrace: String? = null
    ) {
        analytics.logEvent("error") {
            param("error_type", errorType)
            param("error_message", errorMessage)
            stackTrace?.let { param("stack_trace", it) }
        }
    }

    fun logAssistantCreation(success: Boolean) {
        analytics.logEvent("assistant_creation") {
            param("success", success)
        }
    }

    fun logConversationStarted() {
        analytics.logEvent("conversation_started", null)
    }

    fun logConversationEnded(messageCount: Int, duration: Long) {
        analytics.logEvent("conversation_ended") {
            param("message_count", messageCount.toLong())
            param("duration_ms", duration)
        }
    }
} 