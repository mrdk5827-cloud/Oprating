# 🔮 Fixa AI (फिक्सा) - Floating Screen AI Assistant

[![Android](https://img.shields.io/badge/Platform-Android%2024%2B-brightgreen.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.2-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![Gemini AI](https://img.shields.io/badge/AI-Google%20Gemini%203.5%20Flash-cyan.svg)](https://ai.google.dev)
[![License](https://img.shields.io/badge/License-MIT-orange.svg)](LICENSE)

> **Fixa (फिक्सा)** is an intelligent, floating on-screen AI companion designed for Android. It floats on top of your screen over any application, communicates fluently in **Hindi, English, and Hinglish**, solves complex technical & mathematical questions, and executes instant device automation such as sending WhatsApp messages with a single command!

---

## ✨ Features (मुख्य विशेषताएं)

1. **🫧 Floating Screen Bubble (फ्लोटिंग स्क्रीन असिस्टेंट):**
   - Floats effortlessly on top of any active app (WhatsApp, YouTube, Chrome, Games).
   - Draggable anywhere on the screen with magnetic edge snapping.
   - Tap to expand the interactive Fixa command hub right over your screen.

2. **📲 Smart WhatsApp & Device Automator (व्हाट्सएप ऑटोमेशन):**
   - Natural language commands like:
     - *"person A ko whatsapp per jakar ye maine khana kha liye ye message kar do"*
     - *"WhatsApp Mohan ko message bhejo: kal subah milte hain"*
     - *"Call Mummy"*
     - *"Open Camera / Calculator / YouTube"*
   - Automatically formats recipient, prepares payload, and launches WhatsApp directly.

3. **🗣️ Bilingual Speech & Voice (हिंदी और English आवाज़):**
   - Voice input (Speech-to-Text) with auto language detection.
   - Natural spoken responses (Text-to-Speech) in both Indian Hindi (`hi-IN`) and English (`en-US`).
   - One-tap audio read-back on any answer.

4. **🔬 Omniscient Knowledge & Component Solver (गहन ज्ञान व कंपोनेंट सॉल्वर):**
   - Solves complicated mathematics equations step-by-step.
   - Comprehensive electronics & robotics component library (Arduino, ESP32, Resistors, Transistors, ICs, Sensors, Microcontrollers).
   - Science, programming, and general daily questions.

5. **🌐 Interactive Companion Web Page & GitHub Pages Ready:**
   - Standalone responsive web dashboard (`index.html`) with canvas particle animations & interactive orb.
   - Built-in In-App Web View companion.

---

## 🚀 How to Run on Android

1. Open project in **Android Studio**.
2. To use live Google Gemini AI, configure `GEMINI_API_KEY` in your `.env` or the AI Studio Secrets panel.
3. Build and install the APK on an Android device (API 24+).
4. Launch **Fixa AI**, grant **Display over other apps** (System Overlay) permission when prompted to enable screen floating!

---

## 📱 Tech Stack
- **Architecture:** Clean MVVM with Kotlin Coroutines & StateFlow
- **UI:** Jetpack Compose + Material 3 Dark Cybernetic Theme
- **Networking:** Retrofit + Moshi + OkHttp
- **AI Core:** Google Gemini 3.5 Flash (`gemini-3.5-flash`)
- **System Services:** Android WindowManager Overlay & Foreground Service
