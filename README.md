# Fixa - Floating AI Assistant & Automation Engine 🤖✨

**Fixa** is an intelligent, floating on-screen AI assistant for Android built with Jetpack Compose, Gemini 3.5, and native device automation. It answers questions on electronics components, science, math, coding, and world knowledge, while also executing real-world phone actions like WhatsApp messaging, phone calls, and app controls.

---

## 🌟 मुख्य विशेषताएं (Key Features)

### 1. Screen Floating Assistant (स्क्रीन पर तैरने वाला बबल)
- **Floating Overlay**: एक बार चालू करने पर Fixa का बबल आपकी स्क्रीन पर किसी भी ऐप के ऊपर तैरने लगता है।
- **Drag & Snap**: अपनी उंगली से स्क्रीन पर कहीं भी ड्रैग करें।
- **Quick Floating Window**: बबल पर टैप करते ही स्क्रीन के ऊपर ही एक फ्लोटिंग चैट और कमांड बॉक्स खुल जाता है।

### 2. WhatsApp & Phone Automation (व्हाट्सएप व फोन ऑटोमेशन)
- **WhatsApp Commands**: "Person A को WhatsApp पर 'मैंने खाना खा लिया' मैसेज कर दो" बोलने या लिखने पर Fixa तुरंत WhatsApp खोलकर संदेश तैयार कर देता है।
- **Smart Calling**: किसी को भी सीधे कॉल डायल करें ("Call 9876543210")।
- **Device Controls**: टॉर्च/फ्लैशलाइट चालू-बंद करना, टाइमर लगाना, YouTube/Camera/Settings खोलना।

### 3. Components & Deep Problem Solving (कॉम्पोनेन्ट्स व जटिल ज्ञान)
- **Electronics & Hardware**: BJT vs MOSFET ट्रांजिस्टर, Arduino vs ESP32 vs Raspberry Pi, कैपेसिटर्स, डायोड्स, सेंसर्स और सर्किट डिजाइन।
- **Complex STEM**: क्वांटम कंप्यूटिंग, टाइम डाइलेशन, ब्लैक होल, उच्च गणित और लॉजिक पहेलियों का चरण-दर-चरण (Step-by-step) समाधान।

### 4. Bilingual Support (हिंदी + English + Hinglish)
- हिंदी, English और Hinglish में सहज बातचीत।
- **Voice Speech & Text-To-Speech**: माइक से बोलकर सवाल पूछें और Fixa की आवाज़ में उत्तर सुनें।

---

## 🚀 GitHub Actions पर कैसे रन करें (How to Run on GitHub)

इस प्रोजेक्ट में `.github/workflows/android.yml` पहले से कॉन्फ़िगर किया गया है:
1. जब भी आप इस प्रोजेक्ट को GitHub पर पुश करेंगे, GitHub Actions अपने आप कोड को कंपाइल करेगा।
2. यूनिट व रोबोलेक्ट्रिक टेस्ट अपने आप रन होंगे।
3. **Downloadable APK**: GitHub Actions के **Artifacts** टैब में जाकर आप सीधे `Fixa-AI-Assistant-APK` डाउनलोड कर सकते हैं और अपने फ़ोन में इंस्टॉल कर सकते हैं!

---

## 💻 Local Build (Android Studio)
1. रिपॉजिटरी क्लोन करें:
   ```bash
   git clone https://github.com/<your-username>/fixa-ai-assistant.git
   ```
2. Android Studio में **Open Existing Project** करें।
3. बिल्ड करें:
   ```bash
   gradle assembleDebug
   ```
4. APK लोकेशन: `app/build/outputs/apk/debug/app-debug.apk`
