package com.example.ui

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.automation.FixaAction
import com.example.automation.FixaActionManager
import com.example.data.GeminiService
import com.example.floating.FixaFloatingService
import com.example.model.AssistantLanguage
import com.example.model.AutomationShortcut
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
                text = "नमस्ते! मैं हूँ Fixa — आपका ऑल-इन-वन फ्लोटिंग AI असिस्टेंट और फोन ऑटोमेशन इंजन।\n\nमुझसे दुनिया के किसी भी विषय, कॉम्पोनेन्ट (इलेक्ट्रॉनिक्स, सेंसर्स, हार्डवेयर), जटिल सवालों का हल पूछें, या मुझसे कोई भी काम करवाएं — जैसे:\n• \"Person A को WhatsApp पर 'मैंने खाना खा लिया' मैसेज कर दो\"\n• \"Call 9876543210\"\n• \"YouTube ऐप खोलो\"\n• \"टॉर्च चालू करो\"",
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

    private val _selectedTab = MutableStateFlow(0) // 0: AI Assistant, 1: Automation Hub
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    init {
        checkOverlayPermission()
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
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

    fun sendMessage(query: String, context: Context? = null) {
        val text = query.trim()
        if (text.isBlank() || _isLoading.value) return

        val userMessage = ChatMessage(
            text = text,
            sender = MessageSender.USER
        )
        _messages.value = _messages.value + userMessage

        // Check if query is an actionable device command
        val detectedAction = FixaActionManager.parseAction(text)
        if (detectedAction != null) {
            val responseText = when (detectedAction) {
                is FixaAction.WhatsAppMessage -> {
                    "🚀 **WhatsApp संदेश तैयार है!**\n\n📱 **प्राप्तकर्ता:** ${detectedAction.recipient}\n✉️ **संदेश:** \"${detectedAction.message}\"\n\nनीचे दिए गए हरे बटन पर टैप करके तुरंत WhatsApp पर भेजें!"
                }
                is FixaAction.CallPhone -> {
                    "📞 **कॉल तैयार है!**\n\nडायल करने के लिए नीचे दिए गए बटन पर टैप करें: **${detectedAction.numberOrName}**"
                }
                is FixaAction.SendSms -> {
                    "💬 **SMS संदेश तैयार है!**\n\n📱 **नंबर:** ${detectedAction.number}\n✉️ **संदेश:** \"${detectedAction.message}\""
                }
                is FixaAction.OpenApp -> {
                    "⚡ **ऐप लॉन्च:** ${detectedAction.appName} खोलने के लिए नीचे बटन दबाएं।"
                }
                is FixaAction.ToggleFlashlight -> {
                    if (detectedAction.enable) "🔦 टॉर्च चालू करने का आदेश तैयार है।" else "🔦 टॉर्च बंद करने का आदेश तैयार है।"
                }
                is FixaAction.WebSearch -> {
                    "🌐 **Google Search:** \"${detectedAction.query}\" सर्च करने के लिए तैयार।"
                }
                is FixaAction.SetTimer -> {
                    "⏰ **टाइमर:** ${detectedAction.seconds / 60} मिनट का टाइमर सेट करने के लिए तैयार।"
                }
            }

            val fixaMessage = ChatMessage(
                text = responseText,
                sender = MessageSender.FIXA,
                language = _selectedLanguage.value,
                action = detectedAction
            )
            _messages.value = _messages.value + fixaMessage

            // If context provided and user requested immediate action
            if (context != null) {
                FixaActionManager.executeAction(context, detectedAction)
            }
            return
        }

        // Standard AI Question/Answer via Gemini
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

    fun executeMessageAction(context: Context, action: FixaAction, messageId: String) {
        FixaActionManager.executeAction(context, action)
        _messages.value = _messages.value.map {
            if (it.id == messageId) it.copy(actionExecuted = true) else it
        }
    }

    fun clearMessages() {
        _messages.value = listOf(
            ChatMessage(
                text = "चैट साफ़ कर दी गई है। अब आप मुझसे नया सवाल पूछ सकते हैं या कोई काम करवा सकते हैं!",
                sender = MessageSender.FIXA
            )
        )
    }

    val automationShortcuts = listOf(
        AutomationShortcut(
            id = "wa_food",
            title = "Send WhatsApp: Ate food",
            hindiTitle = "WhatsApp: 'मैंने खाना खा लिया' भेजें",
            command = "Person A ko WhatsApp par message karo 'Maine khana kha liya'",
            iconName = "WhatsApp",
            category = "COMMUNICATION"
        ),
        AutomationShortcut(
            id = "wa_meet",
            title = "Send WhatsApp: Meeting reminder",
            hindiTitle = "WhatsApp: 'मीटिंग 5 बजे है' भेजें",
            command = "Rahul ko WhatsApp par message karo 'Meeting 5 baje hai, ready rehna'",
            iconName = "WhatsApp",
            category = "COMMUNICATION"
        ),
        AutomationShortcut(
            id = "call_dial",
            title = "Dial Phone Call",
            hindiTitle = "कॉल डायल करें",
            command = "Call 9876543210",
            iconName = "Call",
            category = "COMMUNICATION"
        ),
        AutomationShortcut(
            id = "app_yt",
            title = "Open YouTube",
            hindiTitle = "YouTube ऐप खोलें",
            command = "Open YouTube",
            iconName = "YouTube",
            category = "DEVICE"
        ),
        AutomationShortcut(
            id = "app_torch",
            title = "Toggle Flashlight",
            hindiTitle = "फ्लैशलाइट (टॉर्च) चालू करें",
            command = "Torch on karo",
            iconName = "Flashlight",
            category = "DEVICE"
        ),
        AutomationShortcut(
            id = "app_timer",
            title = "Set 10 Min Timer",
            hindiTitle = "10 मिनट का टाइमर लगाएं",
            command = "Set timer for 10 minutes",
            iconName = "Timer",
            category = "DEVICE"
        ),
        AutomationShortcut(
            id = "search_tech",
            title = "Search Google",
            hindiTitle = "Google पर सर्च करें",
            command = "Search Google for latest robotics and AI microcontrollers",
            iconName = "Search",
            category = "SEARCH"
        ),
        AutomationShortcut(
            id = "app_settings",
            title = "Open Settings",
            hindiTitle = "फ़ोन की सेटिंग्स खोलें",
            command = "Open Settings",
            iconName = "Settings",
            category = "DEVICE"
        )
    )

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
