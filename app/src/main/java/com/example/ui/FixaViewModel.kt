package com.example.ui

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GeminiService
import com.example.floating.FixaFloatingService
import com.example.model.AssistantLanguage
import com.example.model.ChatMessage
import com.example.model.KnowledgePrompt
import com.example.model.MessageSender
import com.example.util.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FixaViewModel(application: Application) : AndroidViewModel(application) {

    private val geminiService = GeminiService()
    val ttsManager = TtsManager(application)

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "नमस्ते! मैं हूँ Fixa — आपका ऑल-इन-वन फ्लोटिंग AI असिस्टेंट।\n\nमैं दुनिया के किसी भी विषय, कॉम्पोनेन्ट (इलेक्ट्रॉनिक्स, सेंसर्स, हार्डवेयर, सॉफ्टवेयर), और जटिल गणित व विज्ञान के सवालों को स्टेप-बाय-स्टेप हल कर सकता हूँ।\n\nस्क्रीन पर फ्लोट करने के लिए ऊपर 'Floating Assistant' चालू करें, या नीचे मुझसे सीधे बात करें!",
                sender = MessageSender.FIXA
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(AssistantLanguage.AUTO)
    val selectedLanguage: StateFlow<AssistantLanguage> = _selectedLanguage.asStateFlow()

    private val _isFloatingServiceEnabled = MutableStateFlow(false)
    val isFloatingServiceEnabled: StateFlow<Boolean> = _isFloatingServiceEnabled.asStateFlow()

    private val _showInAppBubble = MutableStateFlow(true)
    val showInAppBubble: StateFlow<Boolean> = _showInAppBubble.asStateFlow()

    private val _hasOverlayPermission = MutableStateFlow(false)
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    init {
        checkOverlayPermission()
    }

    fun checkOverlayPermission() {
        val hasPermission = Settings.canDrawOverlays(getApplication())
        _hasOverlayPermission.value = hasPermission
        _isFloatingServiceEnabled.value = FixaFloatingService.isRunning
    }

    fun toggleFloatingService(enable: Boolean) {
        val context = getApplication<Application>()
        if (enable) {
            if (Settings.canDrawOverlays(context)) {
                FixaFloatingService.startService(context)
                _isFloatingServiceEnabled.value = true
            } else {
                _hasOverlayPermission.value = false
            }
        } else {
            FixaFloatingService.stopService(context)
            _isFloatingServiceEnabled.value = false
        }
    }

    fun toggleInAppBubble(enable: Boolean) {
        _showInAppBubble.value = enable
    }

    fun setLanguage(language: AssistantLanguage) {
        _selectedLanguage.value = language
    }

    fun sendMessage(query: String) {
        val text = query.trim()
        if (text.isBlank() || _isLoading.value) return

        val userMessage = ChatMessage(
            text = text,
            sender = MessageSender.USER
        )
        _messages.value = _messages.value + userMessage
        _isLoading.value = true

        viewModelScope.launch {
            val response = geminiService.generateFixaResponse(
                prompt = text,
                history = _messages.value,
                languageMode = _selectedLanguage.value
            )

            val fixaMessage = ChatMessage(
                text = response,
                sender = MessageSender.FIXA,
                language = _selectedLanguage.value
            )
            _messages.value = _messages.value + fixaMessage
            _isLoading.value = false
        }
    }

    fun clearMessages() {
        _messages.value = listOf(
            ChatMessage(
                text = "चैट साफ़ कर दी गई है। अब आप मुझसे नया सवाल पूछ सकते हैं!",
                sender = MessageSender.FIXA
            )
        )
    }

    val knowledgePrompts = listOf(
        KnowledgePrompt(
            id = "comp_1",
            title = "Transistor BJT vs MOSFET",
            hindiTitle = "ट्रांजिस्टर (BJT vs MOSFET) कैसे काम करता है?",
            prompt = "इलेक्ट्रॉनिक्स में BJT और MOSFET ट्रांजिस्टर कैसे काम करते हैं और सर्किट में इन्हें कैसे चुना जाता है? विस्तार से समझाओ।",
            category = "COMPONENTS",
            iconName = "Memory"
        ),
        KnowledgePrompt(
            id = "comp_2",
            title = "Microcontrollers Comparison",
            hindiTitle = "Arduino, ESP32 और Raspberry Pi में क्या फर्क है?",
            prompt = "Arduino, ESP32, और Raspberry Pi में क्या मुख्य अंतर है और IoT या रोबोटिक्स में कौन सा सबसे बेहतर है?",
            category = "COMPONENTS",
            iconName = "DeveloperBoard"
        ),
        KnowledgePrompt(
            id = "math_1",
            title = "Quantum Computing",
            hindiTitle = "क्वांटम कंप्यूटिंग और सुपरपोजिशन को सरल भाषा में समझाओ",
            prompt = "क्वांटम कंप्यूटिंग क्या है? सुपरपोजिशन और एंटैंगलमेंट को आम इंसान के समझने योग्य आसान उदाहरण के साथ समझाओ।",
            category = "COMPLEX",
            iconName = "Psychology"
        ),
        KnowledgePrompt(
            id = "math_2",
            title = "Einstein's Time Dilation",
            hindiTitle = "आइंस्टीन की थ्योरी ऑफ रिलेटिविटी और टाइम डाइलेशन",
            prompt = "Time dilation (समय का धीमा होना) कैसे काम करता है? स्पेस में और ब्लैक होल के पास समय क्यों बदल जाता है? स्टेप बाय स्टेप समझाओ।",
            category = "COMPLEX",
            iconName = "HourglassEmpty"
        ),
        KnowledgePrompt(
            id = "world_1",
            title = "Black Holes Secrets",
            hindiTitle = "ब्लैक होल और इवेंट होराइजन का संपूर्ण रहस्य",
            prompt = "ब्लैक होल क्या है? सिंगुलैरिटी और इवेंट होराइजन पर भौतिकी के नियम कैसे काम करते हैं?",
            category = "WORLD",
            iconName = "Public"
        ),
        KnowledgePrompt(
            id = "world_2",
            title = "Human Brain & Neurons",
            hindiTitle = "इंसानी दिमाग और न्यूरॉन्स कैसे सोचते हैं?",
            prompt = "मानव मस्तिष्क में न्यूरॉन्स कैसे सिग्नल भेजते हैं और हम यादें कैसे बनाते हैं? जैविक प्रक्रिया बताओ।",
            category = "WORLD",
            iconName = "Lightbulb"
        )
    )

    override fun onCleared() {
        ttsManager.shutdown()
        super.onCleared()
    }
}
