package com.example.model

import com.example.automation.FixaAction
import java.util.UUID

enum class MessageSender {
    USER, FIXA
}

enum class AssistantLanguage(val displayName: String, val promptInstruction: String) {
    AUTO("ऑटो (Auto)", "Detect language and reply in the same language (Hindi, English, or Hinglish)."),
    HINDI("हिंदी (Hindi)", "Always reply in clear, natural Hindi (देवनागरी लिपि)."),
    ENGLISH("English", "Always reply in clear, professional English."),
    HINGLISH("Hinglish", "Reply in natural conversational Hinglish (Hindi written in Roman/English script or conversational Hindi-English mix).")
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val sender: MessageSender,
    val timestamp: Long = System.currentTimeMillis(),
    val language: AssistantLanguage = AssistantLanguage.AUTO,
    val category: String? = null,
    val isStreaming: Boolean = false,
    val action: FixaAction? = null,
    val actionExecuted: Boolean = false
)

data class KnowledgePrompt(
    val id: String,
    val title: String,
    val hindiTitle: String,
    val prompt: String,
    val category: String,
    val iconName: String
)

data class AutomationShortcut(
    val id: String,
    val title: String,
    val hindiTitle: String,
    val command: String,
    val iconName: String,
    val category: String
)
