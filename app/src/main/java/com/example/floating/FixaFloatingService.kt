package com.example.floating

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlin.math.abs

class FixaFloatingService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingBubbleView: View? = null
    private var params: WindowManager.LayoutParams? = null

    companion object {
        const val ACTION_START = "com.example.floating.START"
        const val ACTION_STOP = "com.example.floating.STOP"
        private const val CHANNEL_ID = "fixa_floating_assistant_channel"
        private const val NOTIFICATION_ID = 1001

        var isRunning: Boolean = false
            private set

        fun startService(context: Context) {
            if (Settings.canDrawOverlays(context)) {
                val intent = Intent(context, FixaFloatingService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FixaFloatingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                removeFloatingBubble()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                isRunning = false
            }
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForeground(NOTIFICATION_ID, buildForegroundNotification())
                }
                if (Settings.canDrawOverlays(this)) {
                    createFloatingBubble()
                    isRunning = true
                } else {
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Fixa Floating Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when Fixa AI Assistant is floating on your screen"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val appIntent = Intent(this, MainActivity::class.java)
        val pendingAppIntent = PendingIntent.getActivity(
            this, 0, appIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FixaFloatingService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fixa Floating AI Active")
            .setContentText("Tap to open Fixa or tap the screen bubble")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingAppIntent)
            .addAction(0, "Turn Off", pendingStopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingBubble() {
        if (floatingBubbleView != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val displayMetrics = resources.displayMetrics
        val density = displayMetrics.density
        val bubbleSizePx = (64 * density).toInt()

        params = WindowManager.LayoutParams(
            bubbleSizePx,
            bubbleSizePx,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = displayMetrics.widthPixels - bubbleSizePx - (16 * density).toInt()
            y = (displayMetrics.heightPixels * 0.35f).toInt()
        }

        // Create programmatic floating bubble container with modern cyber glow
        val container = FrameLayout(this).apply {
            // Glowing round background
            val backgroundDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(
                    Color.parseColor("#4F46E5"),
                    Color.parseColor("#06B6D4")
                )
                gradientType = GradientDrawable.LINEAR_GRADIENT
                setStroke((2 * density).toInt(), Color.parseColor("#38BDF8"))
            }
            background = backgroundDrawable
            elevation = 16 * density
        }

        // Inner icon / text
        val iconView = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher_round)
            val padding = (4 * density).toInt()
            setPadding(padding, padding, padding, padding)
            val iconLp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            layoutParams = iconLp
        }
        container.addView(iconView)

        // Micro badge: "FIXA"
        val badge = TextView(this).apply {
            text = "FIXA"
            setTextColor(Color.WHITE)
            textSize = 8f
            gravity = Gravity.CENTER
            val badgeBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 6 * density
                setColor(Color.parseColor("#9333EA"))
            }
            background = badgeBg
            val badgeLp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply {
                bottomMargin = (2 * density).toInt()
            }
            setPadding((4 * density).toInt(), (1 * density).toInt(), (4 * density).toInt(), (1 * density).toInt())
            layoutParams = badgeLp
        }
        container.addView(badge)

        // Drag & Tap touch listener
        container.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var touchStartTime = 0L

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val currentParams = params ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = currentParams.x
                        initialY = currentParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        touchStartTime = System.currentTimeMillis()
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        currentParams.x = initialX + dx
                        currentParams.y = initialY + dy
                        try {
                            windowManager?.updateViewLayout(container, currentParams)
                        } catch (e: Exception) {
                            // ignore update race conditions
                        }
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        val totalDx = abs(event.rawX - initialTouchX)
                        val totalDy = abs(event.rawY - initialTouchY)
                        val duration = System.currentTimeMillis() - touchStartTime

                        // Tap detection: small movement and short duration
                        if (totalDx < (12 * density) && totalDy < (12 * density) && duration < 350) {
                            openFixaFloatingChat()
                        } else {
                            // Snap to nearest screen edge
                            val screenWidth = resources.displayMetrics.widthPixels
                            val targetX = if (currentParams.x + (bubbleSizePx / 2) < screenWidth / 2) {
                                (12 * density).toInt()
                            } else {
                                screenWidth - bubbleSizePx - (12 * density).toInt()
                            }
                            currentParams.x = targetX
                            try {
                                windowManager?.updateViewLayout(container, currentParams)
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                        return true
                    }
                }
                return false
            }
        })

        floatingBubbleView = container
        try {
            windowManager?.addView(container, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openFixaFloatingChat() {
        val chatIntent = Intent(this, FixaFloatingChatActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(chatIntent)
    }

    private fun removeFloatingBubble() {
        floatingBubbleView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                // ignore
            }
        }
        floatingBubbleView = null
    }

    override fun onDestroy() {
        removeFloatingBubble()
        isRunning = false
        super.onDestroy()
    }
}
