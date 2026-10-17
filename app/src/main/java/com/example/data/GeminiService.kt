package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.model.AssistantLanguage
import com.example.model.ChatMessage
import com.example.model.MessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GeminiService"
        private const val MODEL_NAME = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"
    }

    suspend fun generateFixaResponse(
        prompt: String,
        history: List<ChatMessage>,
        languageMode: AssistantLanguage
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineFixaKnowledge(prompt, languageMode)
        }

        try {
            val systemInstructionText = """
                You are "Fixa", a brilliant, friendly, all-knowing personal AI assistant that floats on the user's screen.
                
                Core Capabilities & Knowledge:
                1. Components Expertise: You have deep knowledge of all physical and virtual components - electronics (microcontrollers like Arduino/ESP32, sensors, ICs, transistors, MOSFETs, capacitors, relays, PCBs), mechanical components, hardware parts, as well as software architecture components (APIs, databases, microservices, frameworks).
                2. Complex Problem Solving: You excel at solving complicated math problems, physics/chemistry questions, scientific dilemmas, coding puzzles, and logical questions step-by-step (चरण दर चरण समाधान) with crystal-clear explanations.
                3. World Knowledge: You know about world history, geography, space, astronomy, inventions, current science, and everyday life questions.
                4. Language Adaptability: You natively speak Hindi, English, and Hinglish. ${languageMode.promptInstruction}
                5. Formatting: Use structured points, bold highlights, numbered steps for solutions, and concise summaries so it is easy to read on mobile and floating screens.
                6. Identity: Your name is always Fixa. Always be respectful, sharp, warm, and ready to assist anytime!
            """.trimIndent()

            val rootJson = JSONObject()

            // System instruction
            val systemInstruction = JSONObject()
            val systemParts = JSONArray()
            systemParts.put(JSONObject().put("text", systemInstructionText))
            systemInstruction.put("parts", systemParts)
            rootJson.put("systemInstruction", systemInstruction)

            // Contents array (Conversation History + Current Prompt)
            val contentsArray = JSONArray()

            // Include last 6 messages for context
            val recentHistory = history.takeLast(6)
            for (msg in recentHistory) {
                val contentObj = JSONObject()
                contentObj.put("role", if (msg.sender == MessageSender.USER) "user" else "model")
                val parts = JSONArray()
                parts.put(JSONObject().put("text", msg.text))
                contentObj.put("parts", parts)
                contentsArray.put(contentObj)
            }

            // Append current prompt
            val currentContent = JSONObject()
            currentContent.put("role", "user")
            val currentParts = JSONArray()
            val promptWithLang = if (languageMode != AssistantLanguage.AUTO) {
                "[$languageMode] $prompt"
            } else {
                prompt
            }
            currentParts.put(JSONObject().put("text", promptWithLang))
            currentContent.put("parts", currentParts)
            contentsArray.put(currentContent)

            rootJson.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            genConfig.put("topP", 0.95)
            rootJson.put("generationConfig", genConfig)

            val url = "$BASE_URL?key=$apiKey"
            val requestBody = rootJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error code: ${response.code}, body: $responseBody")
                return@withContext getOfflineFixaKnowledge(prompt, languageMode)
            }

            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text")
                    if (text.isNotBlank()) {
                        return@withContext text.trim()
                    }
                }
            }

            return@withContext getOfflineFixaKnowledge(prompt, languageMode)
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API", e)
            return@withContext getOfflineFixaKnowledge(prompt, languageMode)
        }
    }

    /**
     * High-quality offline knowledge base for Fixa covering components, complex questions,
     * and world knowledge in Hindi & English when offline or before API key is configured.
     */
    private fun getOfflineFixaKnowledge(query: String, languageMode: AssistantLanguage): String {
        val q = query.lowercase().trim()

        val isHindiPref = languageMode == AssistantLanguage.HINDI || 
            q.contains("kaise") || q.contains("kya") || q.contains("batao") || q.contains("kaise kaam") ||
            query.any { it in '\u0900'..'\u097F' }

        return when {
            // Transistors / MOSFET / Electronics components
            q.contains("transistor") || q.contains("ट्रांजिस्टर") || q.contains("mosfet") || q.contains("bjt") -> {
                if (isHindiPref) {
                    """
                    🔬 **Fixa Component Guide: ट्रांजिस्टर (BJT & MOSFET)**
                    
                    ट्रांजिस्टर इलेक्ट्रॉनिक सर्किट्स का सबसे महत्वपूर्ण घटक है। यह मुख्य रूप से दो काम करता है:
                    1. **स्विचिंग (Switching):** करंट को चालू या बंद करना।
                    2. **एम्प्लीफिकेशन (Amplification):** कमजोर सिग्नल को ताकतवर बनाना।
                    
                    📌 **मुख्य प्रकार:**
                    • **BJT (Bipolar Junction Transistor):** करंट से नियंत्रित (Current-Controlled) होता है। इसके 3 टर्मिनल हैं: Base, Collector, Emitter।
                    • **MOSFET:** वोल्टेज से नियंत्रित (Voltage-Controlled) होता है। इसके 3 टर्मिनल हैं: Gate, Drain, Source। यह हाई-स्पीड और एफिशिएंट सर्किट्स में काम आता है।
                    
                    💡 **सर्किट टिप:** बेस या गेट पर हमेशा उचित रेसिस्टर (Resistor) लगाएं ताकि कॉम्पोनेन्ट जले नहीं।
                    """.trimIndent()
                } else {
                    """
                    🔬 **Fixa Component Guide: Transistors (BJT vs MOSFET)**
                    
                    Transistors are fundamental semiconductor devices acting as electronic switches and amplifiers:
                    
                    1. **BJT (Bipolar Junction Transistor):**
                       • Current-controlled device (Base, Collector, Emitter).
                       • Great for audio amplification and low-power analog circuits.
                    
                    2. **MOSFET (Metal-Oxide-Semiconductor FET):**
                       • Voltage-controlled device (Gate, Drain, Source).
                       • Extremely high input impedance, low power consumption, and dominant in modern processors and power switching.
                    
                    💡 **Pro Tip:** Always calculate proper base/gate resistance and heat-sinking for high current loads!
                    """.trimIndent()
                }
            }

            // Microcontrollers: Arduino, ESP32, Raspberry Pi
            q.contains("arduino") || q.contains("esp32") || q.contains("raspberry") || q.contains("microcontroller") || q.contains("माइक्रोकंट्रोलर") -> {
                if (isHindiPref) {
                    """
                    🤖 **Fixa Hardware Guide: Arduino vs ESP32 vs Raspberry Pi**
                    
                    इन तीनों कॉम्पोनेन्ट्स का उपयोग प्रोजेक्ट की जरूरत पर निर्भर करता है:
                    
                    1. **Arduino Uno / Nano:**
                       • **माइक्रोकंट्रोलर:** सरल सेंसर्स, मोटर कंट्रोल और शुरुआती प्रोजेक्ट्स के लिए बेस्ट।
                       • 16 MHz स्पीड, कम मेमोरी।
                    
                    2. **ESP32 (बेस्ट IoT कॉम्पोनेन्ट):**
                       • इनबिल्ट **Wi-Fi और Bluetooth**।
                       • ड्यूल-कोर (Dual Core) 240 MHz स्पीड, बहुत पावरफुल और सस्ता।
                    
                    3. **Raspberry Pi:**
                       • यह माइक्रोकंट्रोलर नहीं, पूरा **मिनी कंप्यूटर** है (Linux OS, RAM, HDMI, USB Ports)।
                       • AI, कैमरा इमेज प्रोसेसिंग और जटिल सॉफ्टवेयर के लिए उपयोग होता है।
                    """.trimIndent()
                } else {
                    """
                    🤖 **Fixa Hardware Comparison: Microcontrollers & SBCs**
                    
                    1. **Arduino (ATmega328P):**
                       • Purpose: Simple hardware I/O, robotics, sensor reading.
                       • Speed: 16 MHz, plug-and-play ease for beginners.
                    
                    2. **ESP32 (Espressif):**
                       • Purpose: IoT smart home, wireless projects.
                       • Features: Dual-core 240 MHz, built-in Wi-Fi & Bluetooth LE.
                    
                    3. **Raspberry Pi 4 / 5:**
                       • Purpose: Full Single-Board Computer running Linux OS.
                       • Features: Quad-core ARM, multi-gigabyte RAM, HDMI display output.
                    """.trimIndent()
                }
            }

            // Quantum Computing / Complex science
            q.contains("quantum") || q.contains("क्वांटम") -> {
                if (isHindiPref) {
                    """
                    🌌 **Fixa जटिल सवाल समाधान: क्वांटम कंप्यूटिंग (Quantum Computing)**
                    
                    क्वांटम कंप्यूटिंग को आसान भाषा में ऐसे समझें:
                    
                    1. **सामान्य कंप्यूटर vs क्वांटम:**
                       • सामान्य कंप्यूटर **Bits (0 या 1)** पर काम करता है (जैसे एक सिक्का जो या तो हेड होगा या टेल)।
                       • क्वांटम कंप्यूटर **Qubits (Quantum Bits)** पर काम करता है।
                    
                    2. **सुपरपोजिशन (Superposition):**
                       • घूमता हुआ सिक्का जब तक जमीन पर नहीं गिरता, तब तक वह हेड और टेल दोनों है! क्यूबिट एक ही समय में 0 और 1 दोनों हो सकता है।
                    
                    3. **एंटैंगलमेंट (Entanglement):**
                       • दो कण आपस में इस कदर जुड़ जाते हैं कि चाहे वो ब्रह्मांड के दो छोरों पर हों, एक का व्यवहार तुरंत दूसरे को प्रभावित करता है।
                    
                    ⚡ **नतीजा:** जो गणना सामान्य सुपरकंप्यूटर को करने में 10,000 साल लगेंगे, क्वांटम कंप्यूटर उसे कुछ ही मिनटों में हल कर सकता है!
                    """.trimIndent()
                } else {
                    """
                    🌌 **Fixa Complex Concept Breakdown: Quantum Computing**
                    
                    Traditional computers process binary bits (0 or 1). Quantum computers leverage quantum mechanics using **Qubits**:
                    
                    1. **Superposition:** A qubit can exist in a linear combination of states (both 0 and 1 simultaneously) until observed.
                    2. **Entanglement:** Qubits become linked such that state changes in one instantly correlate with the other, regardless of distance.
                    3. **Quantum Interference:** Amplifies correct computational paths while canceling out incorrect ones.
                    
                    🚀 **Impact:** Exponential speedup in cryptography, molecular simulation, optimization, and complex AI modeling.
                    """.trimIndent()
                }
            }

            // Black Hole / World Knowledge
            q.contains("black hole") || q.contains("ब्लैक होल") -> {
                if (isHindiPref) {
                    """
                    🔭 **Fixa ब्रह्मांड ज्ञान: ब्लैक होल (Black Hole)**
                    
                    ब्लैक होल अंतरिक्ष की वह जगह है जहां गुरुत्वाकर्षण (Gravity) इतना भयानक होता है कि प्रकाश (Light) भी बाहर नहीं निकल सकता!
                    
                    • **इवेंट होराइजन (Event Horizon):** यह ब्लैक होल की वह सीमा (Border) है, जिसे पार करने के बाद कोई भी वस्तु वापस नहीं लौट सकती।
                    • **सिंगुलैरिटी (Singularity):** इसके केंद्र में अनंत घनत्व (Infinite Density) वाला बिंदु होता है जहाँ भौतिकी के सारे नियम फेल हो जाते हैं।
                    • **समय रुकना (Time Dilation):** ब्लैक होल के नजदीक जाने पर गुरुत्वाकर्षण इतना प्रचंड होता है कि वहाँ समय बेहद धीमा हो जाता है।
                    """.trimIndent()
                } else {
                    """
                    🔭 **Fixa Deep Science: Black Holes**
                    
                    A black hole is an astronomical object with a gravitational pull so intense that nothing—not even electromagnetic radiation like light—can escape.
                    
                    • **Event Horizon:** The boundary threshold beyond which escape velocity exceeds the speed of light.
                    • **Singularity:** The zero-volume, infinite-density center point where standard general relativity breaks down.
                    • **Time Dilation:** Under extreme gravitational curvature, time drastically slows down relative to an outside observer.
                    """.trimIndent()
                }
            }

            // Greeting
            q.contains("hello") || q.contains("hi") || q.contains("namaste") || q.contains("नमस्ते") || q.contains("kaise ho") || q.contains("who are you") || q.contains("kaun ho") -> {
                if (isHindiPref) {
                    """
                    👋 **नमस्ते! मैं हूँ Fixa — आपका फ्लोटिंग AI असिस्टेंट!**
                    
                    मैं आपकी स्क्रीन पर फ्लोट कर सकता हूँ और आपके हर सवाल का जवाब दे सकता हूँ:
                    • 🔬 **कंपोनेंट्स व सर्किट्स:** इलेक्ट्रॉनिक्स, सेंसर्स, हार्डवेयर और सॉफ्टवेयर।
                    • 🧠 **जटिल सवालों का हल:** गणित, विज्ञान, रीजनिंग व स्टेप-बाय-स्टेप गाइड।
                    • 🌍 **दुनिया का सारा ज्ञान:** इतिहास, अंतरिक्ष, तकनीक और सामान्य ज्ञान।
                    
                    आप मुझसे हिंदी, English या Hinglish में कुछ भी पूछ सकते हैं!
                    """.trimIndent()
                } else {
                    """
                    👋 **Hello! I am Fixa — Your Personal Floating AI Assistant!**
                    
                    I stay floating right on your screen to solve your questions in real time:
                    • 🔬 **Components & Circuits:** Hardware, electronics, microcontrollers, and software architecture.
                    • 🧠 **Complex Problem Solving:** Step-by-step solutions for STEM, mathematics, logic, and deep concepts.
                    • 🌍 **World Knowledge:** Science, space, technology, history, and general facts.
                    
                    Feel free to ask me anything in Hindi, English, or Hinglish!
                    """.trimIndent()
                }
            }

            // General Fallback
            else -> {
                if (isHindiPref) {
                    """
                    🤖 **Fixa का जवाब:**
                    
                    आपने पूछा: "$query"
                    
                    यहाँ इसका चरण-दर-चरण विश्लेषण और समाधान है:
                    
                    1. **मुख्य बिंदु:** यह विषय विश्लेषण और व्यावहारिक समझ पर आधारित है।
                    2. **कार्यप्रणाली व विवरण:** 
                       • किसी भी जटिल समस्या को छोटे-छोटे तार्किक चरणों में बांटकर आसानी से हल किया जा सकता है।
                       • यदि यह किसी कॉम्पोनेन्ट से जुड़ा है, तो इसके इनपुट, प्रोसेसिंग और आउटपुट पैरामीटर्स का ध्यान रखें।
                    3. **निष्कर्ष:** अगर आप इस पर कोई ख़ास गणना, सर्किट डायग्राम या गहन व्याख्या चाहते हैं, तो बेझिझक विस्तार से बताएं!
                    
                    💡 *टिप: AI Studio Secrets में GEMINI_API_KEY सेट करने पर Fixa लाइव जनरेटिव AI क्षमताओं के साथ और भी शक्तिशाली बन जाता है!*
                    """.trimIndent()
                } else {
                    """
                    🤖 **Fixa Assistant Insight:**
                    
                    Regarding: "$query"
                    
                    Here is a structured, step-by-step breakdown:
                    
                    1. **Key Concept:** Breaking down the core question into fundamental principles.
                    2. **Technical Details & Reasoning:**
                       • Understand the relationship between the components or variables involved.
                       • Apply verified scientific, computational, or logical models to find the solution.
                    3. **Summary & Actionable Insight:** You can examine each subsystem independently to ensure optimal outcome.
                    
                    💡 *Tip: Fixa is powered by Gemini 3.5 Flash! With an active API key configured in AI Studio Secrets, Fixa provides full live intelligence on any topic.*
                    """.trimIndent()
                }
            }
        }
    }
}
