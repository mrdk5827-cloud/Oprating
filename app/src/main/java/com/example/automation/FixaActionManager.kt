package com.example.automation

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import android.widget.Toast
import java.net.URLEncoder

sealed class FixaAction(
    val title: String,
    val hindiDescription: String,
    val iconType: String
) {
    data class WhatsAppMessage(
        val recipient: String,
        val message: String
    ) : FixaAction(
        title = "WhatsApp Message",
        hindiDescription = "WhatsApp पर $recipient को संदेश: \"$message\"",
        iconType = "whatsapp"
    )

    data class CallPhone(
        val numberOrName: String
    ) : FixaAction(
        title = "Phone Call",
        hindiDescription = "$numberOrName को कॉल डायल करें",
        iconType = "call"
    )

    data class SendSms(
        val number: String,
        val message: String
    ) : FixaAction(
        title = "SMS Message",
        hindiDescription = "$number पर SMS: \"$message\"",
        iconType = "sms"
    )

    data class OpenApp(
        val appName: String,
        val packageName: String?
    ) : FixaAction(
        title = "Open App",
        hindiDescription = "$appName ऐप खोलें",
        iconType = "app"
    )

    data class WebSearch(
        val query: String
    ) : FixaAction(
        title = "Web Search",
        hindiDescription = "Google पर सर्च करें: \"$query\"",
        iconType = "search"
    )

    data class ToggleFlashlight(
        val enable: Boolean
    ) : FixaAction(
        title = "Torch / Flashlight",
        hindiDescription = if (enable) "टॉर्च चालू करें" else "टॉर्च बंद करें",
        iconType = "torch"
    )

    data class SetTimer(
        val seconds: Int,
        val message: String
    ) : FixaAction(
        title = "Set Timer",
        hindiDescription = "${seconds / 60} मिनट का टाइमर लगाएं",
        iconType = "timer"
    )
}

object FixaActionManager {

    private var isTorchOn = false

    /**
     * Parses natural language query in Hindi, English or Hinglish to detect actionable commands.
     */
    fun parseAction(rawQuery: String): FixaAction? {
        val q = rawQuery.trim()
        val lower = q.lowercase()

        // 1. WhatsApp Message Pattern:
        // "Rahul ko whatsapp par message karo maine khana kha liya"
        // "whatsapp per person a ko message karo ..."
        // "send whatsapp message to X saying Y"
        if (lower.contains("whatsapp") || lower.contains("व्हाट्सएप") || lower.contains("व्हाट्सअप")) {
            val extracted = extractWhatsAppDetails(q)
            return FixaAction.WhatsAppMessage(
                recipient = extracted.first,
                message = extracted.second
            )
        }

        // 2. Call Pattern:
        // "Call 9876543210", "Papa ko call lagao", "call karo"
        if (lower.startsWith("call ") || lower.contains("ko call") || lower.contains("कॉल लगाओ") || lower.contains("कॉल करो")) {
            val target = extractCallTarget(q)
            if (target.isNotBlank()) {
                return FixaAction.CallPhone(target)
            }
        }

        // 3. SMS Pattern:
        if (lower.contains("sms") || lower.contains("मैसेज भेजो") || lower.contains("message bhejo")) {
            val (recipient, text) = extractSmsDetails(q)
            return FixaAction.SendSms(recipient, text)
        }

        // 4. Torch / Flashlight:
        if (lower.contains("torch") || lower.contains("flashlight") || lower.contains("टॉर्च") || lower.contains("फ्लैशलाइट")) {
            val turnOff = lower.contains("off") || lower.contains("बंद")
            return FixaAction.ToggleFlashlight(!turnOff)
        }

        // 5. Open App:
        if (lower.startsWith("open ") || lower.startsWith("kholo ") || lower.contains(" kholo") || lower.contains(" ऐप खोलो")) {
            val appInfo = extractAppToOpen(lower)
            if (appInfo != null) {
                return FixaAction.OpenApp(appInfo.first, appInfo.second)
            }
        }

        // 6. Set Timer / Alarm:
        if (lower.contains("timer") || lower.contains("अलार्म") || lower.contains("टाइम लगाओ")) {
            val minutes = extractTimerMinutes(lower)
            return FixaAction.SetTimer(minutes * 60, "Fixa Timer")
        }

        // 7. Web search:
        if (lower.startsWith("search ") || lower.startsWith("google ") || lower.contains("सर्च करो")) {
            val searchQuery = q
                .replace(Regex("^(search|google|सर्च करो|ढूंढो)", RegexOption.IGNORE_CASE), "")
                .trim()
            if (searchQuery.isNotBlank()) {
                return FixaAction.WebSearch(searchQuery)
            }
        }

        return null
    }

    private fun extractWhatsAppDetails(query: String): Pair<String, String> {
        var recipient = "Contact"
        var message = query

        // Regex patterns for:
        // "X ko whatsapp par message karo Y"
        // "whatsapp per X ko ye Y message kar do"
        val regex1 = Regex("""(?:person\s+([A-Za-z0-9_]+)|([A-Za-z0-9_\u0900-\u097F]+)\s*ko)\s*(?:whatsapp|व्हाट्सएप)[^:]*?(?:karo|bhejo|kar do|likh do)?\s*[:,-]?\s*(?:ki|ke|ye)?\s*["']?(.+)["']?""", RegexOption.IGNORE_CASE)
        val match1 = regex1.find(query)
        if (match1 != null) {
            recipient = match1.groupValues[1].ifBlank { match1.groupValues[2] }.ifBlank { "Contact" }
            message = match1.groupValues[3].trim()
        } else {
            // Fallback: extract message after "message karo" or "bhejo"
            val splits = query.split(Regex("""(?i)(?:message|मैसेज|massage)\s*(?:karo|bhejo|kar do|kardo)\s*"""))
            if (splits.size > 1) {
                message = splits[1].trim()
            }
        }

        // Clean quotes or prefix words
        message = message.trim('"', '\'', ' ')
        if (message.isBlank()) {
            message = "नमस्ते! मैं खाना खा चुका हूँ।"
        }

        return Pair(recipient, message)
    }

    private fun extractCallTarget(query: String): String {
        val numberRegex = Regex("""\b\d{10,12}\b""")
        val numberMatch = numberRegex.find(query)
        if (numberMatch != null) return numberMatch.value

        return query
            .replace(Regex("""(?i)(call|lagao|karo|ko|dial|कॉल|लगाओ|मिलाओ)"""), "")
            .trim()
            .ifBlank { "Contact" }
    }

    private fun extractSmsDetails(query: String): Pair<String, String> {
        val numberMatch = Regex("""\b\d{10,12}\b""").find(query)
        val number = numberMatch?.value ?: "Recipient"
        val msg = query.substringAfter(":", query).trim()
        return Pair(number, msg.ifBlank { "Hello from Fixa" })
    }

    private fun extractAppToOpen(lower: String): Pair<String, String?>? {
        return when {
            lower.contains("youtube") -> Pair("YouTube", "com.google.android.youtube")
            lower.contains("whatsapp") -> Pair("WhatsApp", "com.whatsapp")
            lower.contains("camera") || lower.contains("कैमरा") -> Pair("Camera", null)
            lower.contains("chrome") || lower.contains("browser") -> Pair("Google Chrome", "com.android.chrome")
            lower.contains("maps") || lower.contains("मैप्स") -> Pair("Google Maps", "com.google.android.apps.maps")
            lower.contains("settings") || lower.contains("सेटिंग्स") -> Pair("Settings", null)
            else -> null
        }
    }

    private fun extractTimerMinutes(lower: String): Int {
        val match = Regex("""(\d+)\s*(?:min|minute|मिनट)""").find(lower)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 5
    }

    /**
     * Executes the parsed action using native Android intents.
     */
    fun executeAction(context: Context, action: FixaAction): Boolean {
        try {
            when (action) {
                is FixaAction.WhatsAppMessage -> {
                    // Try WhatsApp Intent directly
                    val encodedMessage = URLEncoder.encode(action.message, "UTF-8")
                    val isNumber = action.recipient.matches(Regex("""^\+?\d{10,13}$"""))

                    val uri = if (isNumber) {
                        Uri.parse("https://api.whatsapp.com/send?phone=${action.recipient}&text=$encodedMessage")
                    } else {
                        Uri.parse("whatsapp://send?text=$encodedMessage")
                    }

                    val whatsappIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        `package` = "com.whatsapp"
                    }

                    try {
                        context.startActivity(whatsappIntent)
                        Toast.makeText(context, "WhatsApp खोला जा रहा है...", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        // Fallback: Generic SEND intent targeting WhatsApp or Chooser
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, action.message)
                            setPackage("com.whatsapp")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(sendIntent)
                        } catch (ex: Exception) {
                            // If WhatsApp not installed on device
                            val chooserIntent = Intent.createChooser(sendIntent, "संदेश भेजें").apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(chooserIntent)
                        }
                    }
                    return true
                }

                is FixaAction.CallPhone -> {
                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${action.numberOrName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(dialIntent)
                    return true
                }

                is FixaAction.SendSms -> {
                    val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("smsto:${action.number}")
                        putExtra("sms_body", action.message)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(smsIntent)
                    return true
                }

                is FixaAction.OpenApp -> {
                    if (action.appName == "Settings") {
                        val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(settingsIntent)
                        return true
                    }
                    if (action.appName == "Camera") {
                        val cameraIntent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(cameraIntent)
                        return true
                    }
                    if (action.packageName != null) {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(action.packageName)
                        if (launchIntent != null) {
                            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(launchIntent)
                            return true
                        }
                    }
                    Toast.makeText(context, "${action.appName} ऐप डिवाइस पर नहीं मिली", Toast.LENGTH_SHORT).show()
                    return false
                }

                is FixaAction.ToggleFlashlight -> {
                    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                    if (cameraManager != null) {
                        val cameraId = cameraManager.cameraIdList.firstOrNull()
                        if (cameraId != null) {
                            cameraManager.setTorchMode(cameraId, action.enable)
                            isTorchOn = action.enable
                            Toast.makeText(context, if (action.enable) "टॉर्च चालू" else "टॉर्च बंद", Toast.LENGTH_SHORT).show()
                            return true
                        }
                    }
                    Toast.makeText(context, "Flashlight unavailable", Toast.LENGTH_SHORT).show()
                    return false
                }

                is FixaAction.WebSearch -> {
                    val searchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${URLEncoder.encode(action.query, "UTF-8")}")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(searchIntent)
                    return true
                }

                is FixaAction.SetTimer -> {
                    val timerIntent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                        putExtra(AlarmClock.EXTRA_LENGTH, action.seconds)
                        putExtra(AlarmClock.EXTRA_MESSAGE, action.message)
                        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(timerIntent)
                    return true
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Action execution error: ${e.message}", Toast.LENGTH_SHORT).show()
            return false
        }
    }
}
