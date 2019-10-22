package com.krushi.minisheart

import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.os.BatteryManager

import android.os.Bundle
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.view.SurfaceHolder

//import java.awt.font.ShapeGraphicAttribute
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.R.attr.name
import android.graphics.RectF
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.R.attr.name
import android.R.attr.y
import android.R.attr.x
import android.graphics.PointF
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.R.attr.name
import android.R.attr.path
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.R.attr.name
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.R.attr.path
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.R.attr.name
import android.util.Log
import org.w3c.dom.Text
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.*

private const val HOUR_STROKE_WIDTH = 10f
private const val MINUTE_STROKE_WIDTH = 6f
private const val SECOND_STROKE_WIDTH = 2f
private const val STROKE_WIDTH = 4f
private const val HEART_STROKE_WIDTH = 50f

private const val CENTER_GAP_AND_CIRCLE_RADIUS = 17f

private const val SHADOW_RADIUS = 10f

private const val DATE = "2012-08-05" //Month starts from 0 in LocalDate class
private const val TIME = "17:00"

class MyWatchFace : CanvasWatchFaceService() {

    override fun onCreateEngine(): Engine {
        return Engine()
    }

    inner class Engine : CanvasWatchFaceService.Engine() {

        private lateinit var calendar: Calendar

        private var registeredTimeZoneReceiver = false
        private var registeredBatteryInfoReceiver = false
        private var muteMode: Boolean = false
        private var centerX: Float = 0F
        private var centerY: Float = 0F
        private var batteryPercent: Int = 0

        private var sMinuteHandLength: Float = 0F
        private var sHourHandLength: Float = 0F

        /* Colors for all hands (hour, minute, ticks) based on photo loaded. */
        private var backgroundColor: Int = Color.BLACK
        private var watchHandColor: Int = Color.WHITE
        private var watchHandShadowColor: Int = Color.RED
        private var tickAndCircleColor: Int = Color.RED

        private lateinit var hourPaint: Paint
        private lateinit var minutePaint: Paint
        private lateinit var secondPaint: Paint
        private lateinit var tickAndCirclePaint: Paint
        private lateinit var heartPaint: Paint
        private lateinit var heartOutlinePaint: Paint
        private lateinit var fillPaint: Paint
        private lateinit var centerDotPaint: Paint
        private lateinit var batteryPercentPaint: Paint
        private lateinit var durationPaint: Paint

        private lateinit var backgroundPaint: Paint
        private lateinit var backgroundBitmap: Bitmap

        private var ambient: Boolean = false
        private var lowBitAmbient: Boolean = false
        private var burnInProtection: Boolean = false

        private val timeZoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                calendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }

        private val batteryInfoReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                batteryPercent = intent.getIntExtra(
                        BatteryManager.EXTRA_LEVEL, 0
                )
                invalidate()
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            setWatchFaceStyle(
                WatchFaceStyle.Builder(this@MyWatchFace)
                    .setShowUnreadCountIndicator(true)
                    .build()
            )

            calendar = Calendar.getInstance()

            initializeBackground()
            initializeWatchFace()
        }

        private fun initializeBackground() {
            backgroundPaint = Paint().apply {
                color = Color.BLACK
            }
            backgroundBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
            backgroundBitmap.eraseColor(backgroundColor)
        }

        private fun initializeWatchFace() {
            /* Set defaults for colors */

            hourPaint = Paint().apply {
                color = watchHandColor
                strokeWidth = HOUR_STROKE_WIDTH
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
            }

            minutePaint = Paint().apply {
                color = watchHandColor
                strokeWidth = MINUTE_STROKE_WIDTH
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
            }

            secondPaint = Paint().apply {
                color = tickAndCircleColor
                strokeWidth = SECOND_STROKE_WIDTH
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
            }

            tickAndCirclePaint = Paint().apply {
                color = tickAndCircleColor
                strokeWidth = STROKE_WIDTH
                isAntiAlias = true
                style = Paint.Style.STROKE
            }

            heartPaint = Paint().apply {
                color = backgroundColor
                shader = null
                strokeWidth = HEART_STROKE_WIDTH
                isAntiAlias = true
                style = Paint.Style.STROKE
            }

            heartOutlinePaint = Paint().apply {
                color = watchHandColor
                isAntiAlias = true
                strokeWidth = 2f
                style = Paint.Style.STROKE
            }

            fillPaint = Paint().apply {
                color = tickAndCircleColor
                isAntiAlias = true
                style = Paint.Style.FILL_AND_STROKE
            }

            centerDotPaint = Paint().apply {
                color = tickAndCircleColor
                isAntiAlias = true
                style = Paint.Style.FILL_AND_STROKE
            }

            batteryPercentPaint = Paint().apply {
                color = Color.WHITE
                isAntiAlias = true
                style = Paint.Style.FILL_AND_STROKE
                textAlign = Paint.Align.CENTER
                textSize = 30f
            }

            durationPaint = Paint().apply {
                color = tickAndCircleColor
                isAntiAlias = true
                style = Paint.Style.FILL_AND_STROKE
                textAlign = Paint.Align.CENTER
                textSize = 30f
            }
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)
            lowBitAmbient = properties.getBoolean(
                WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false
            )
            burnInProtection = properties.getBoolean(
                WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false
            )
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            ambient = inAmbientMode

            updateWatchHandStyle()
        }

        private fun updateWatchHandStyle() {
            /*
            Paint the watch face based on the mode
            Ambient or power mode
            */
            if (ambient) {
                hourPaint.color = Color.WHITE
                minutePaint.color = Color.WHITE
                secondPaint.color = Color.TRANSPARENT
                tickAndCirclePaint.color = Color.WHITE
                fillPaint.color = Color.TRANSPARENT
                heartOutlinePaint.color = Color.GRAY
                centerDotPaint.color = Color.WHITE
                durationPaint.color = Color.TRANSPARENT

                hourPaint.isAntiAlias = false
                minutePaint.isAntiAlias = false
                secondPaint.isAntiAlias = false
                tickAndCirclePaint.isAntiAlias = false
                fillPaint.isAntiAlias = false
                heartOutlinePaint.isAntiAlias = false
                centerDotPaint.isAntiAlias = false
                durationPaint.isAntiAlias = false

                hourPaint.clearShadowLayer()
                minutePaint.clearShadowLayer()
                secondPaint.clearShadowLayer()
                tickAndCirclePaint.clearShadowLayer()
                fillPaint.clearShadowLayer()

            } else {
                hourPaint.color = watchHandColor
                minutePaint.color = watchHandColor
                secondPaint.color = tickAndCircleColor
                tickAndCirclePaint.color = tickAndCircleColor
                fillPaint.color = tickAndCircleColor
                heartOutlinePaint.color = watchHandColor
                centerDotPaint.color = tickAndCircleColor
                durationPaint.color = tickAndCircleColor

                hourPaint.isAntiAlias = true
                minutePaint.isAntiAlias = true
                secondPaint.isAntiAlias = true
                tickAndCirclePaint.isAntiAlias = true
                fillPaint.isAntiAlias = true
                heartOutlinePaint.isAntiAlias = true
                centerDotPaint.isAntiAlias = true
                durationPaint.isAntiAlias = true

                hourPaint.setShadowLayer(SHADOW_RADIUS, 0f, 0f, watchHandShadowColor)
                minutePaint.setShadowLayer(SHADOW_RADIUS, 0f, 0f, watchHandShadowColor)
                secondPaint.setShadowLayer(SHADOW_RADIUS, 0f, 0f, watchHandShadowColor)
            }
        }

        override fun onInterruptionFilterChanged(interruptionFilter: Int) {
            super.onInterruptionFilterChanged(interruptionFilter)
            val inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE

            /* Dim display in mute mode. */
            if (muteMode != inMuteMode) {
                muteMode = inMuteMode
                hourPaint.alpha = if (inMuteMode) 100 else 255
                minutePaint.alpha = if (inMuteMode) 100 else 255
                invalidate()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            centerX = width / 2f
            centerY = height / 2f

            sMinuteHandLength = (centerX * 0.75).toFloat()
            sHourHandLength = (centerX * 0.5).toFloat()

            val scale = width.toFloat() / backgroundBitmap.width.toFloat()

            backgroundBitmap = Bitmap.createScaledBitmap(
                backgroundBitmap,
                (backgroundBitmap.width * scale).toInt(),
                (backgroundBitmap.height * scale).toInt(), true
            )

        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            val now = System.currentTimeMillis()
            calendar.timeInMillis = now

            drawBackground(canvas)
            drawWatchFace(canvas)
        }

        private fun drawBackground(canvas: Canvas) {
            if (ambient && (lowBitAmbient || burnInProtection)) {
                canvas.drawColor(Color.BLACK)
            } else {
                canvas.drawBitmap(backgroundBitmap, 0f, 0f, backgroundPaint)
            }
        }

        private fun drawCenterDot(canvas: Canvas) {
            canvas.drawCircle(208f, 208f, 10f, centerDotPaint)
        }

        private fun drawDialMarking(canvas: Canvas) {
            val innerTickRadius = centerX - 10
            val outerTickRadius = centerX

            for (tickIndex in 0..11) {
                val tickRot = (tickIndex.toDouble() * Math.PI * 2.0 / 12).toFloat()
                val innerX = Math.sin(tickRot.toDouble()).toFloat() * innerTickRadius
                val innerY = (-Math.cos(tickRot.toDouble())).toFloat() * innerTickRadius
                val outerX = Math.sin(tickRot.toDouble()).toFloat() * outerTickRadius
                val outerY = (-Math.cos(tickRot.toDouble())).toFloat() * outerTickRadius
                canvas.drawLine(
                        centerX + innerX, centerY + innerY,
                        centerX + outerX, centerY + outerY, tickAndCirclePaint
                )
            }
        }

        private fun drawHeart(
                canvas: Canvas, paint: Paint, height: Float, width: Float, dx: Float, dy: Float,
                isOutline: Boolean
        ) {
            val path = Path()

            // Starting point
            path.moveTo(
                    (width / 2) + (if (isOutline) 0f else 15f),
                    (height / 5) + (if (isOutline) 5f else 0f)
            )

            // Upper left path
            path.cubicTo(5 * width / 14, 0f,
                    0f, height / 15,
                    width / 28, 2 * height / 5)

            // Lower left path
            path.cubicTo(width / 14, 2 * height / 3,
                    3 * width / 7, 5 * height / 6,
                    width / 2, height)

            // Lower right path
            path.cubicTo(4 * width / 7, 5 * height / 6,
                    13 * width / 14, 2 * height / 3,
                    27 * width / 28, 2 * height / 5)

            // Upper right path
            path.cubicTo(width, height / 15,
                    9 * width / 14, 0f,
                    (width / 2) - (if (isOutline) 0f else 12.5f),
                    (height / 5) + (if (isOutline) 5f else -2.5f)
            )

            path.offset(dx, dy)
            canvas.drawPath(path, paint)

            if (isOutline) {
                canvas.drawText(batteryPercent.toString(), 208f, 290f, batteryPercentPaint)
            }
        }

        private fun fillBatteryColor(canvas: Canvas) {
            val top = 246f + ((1f - (batteryPercent/100f)) * 72f)
            canvas.drawRect(158f, top, 258f, 318f, fillPaint)
        }

        private fun drawHeartAndFill(canvas: Canvas) {
            //Fill color in heart shape by battery percentage
            fillBatteryColor(canvas)

            //Draw heart shape
            drawHeart(
                    canvas,
                    heartPaint,
                    150f,
                    150f,
                    133f,
                    208f,
                    false
            )

            // Draw heart outline
            drawHeart(
                    canvas,
                    heartOutlinePaint,
                    90f,
                    100f,
                    158f,
                    233f,
                    true
            )
        }

        private fun drawHands(canvas: Canvas) {
            val secondsRotation = calendar.get(Calendar.SECOND) * 6f
            val minutesRotation = calendar.get(Calendar.MINUTE) * 6f

            val hourHandOffset = calendar.get(Calendar.MINUTE) / 2f
            val hoursRotation = calendar.get(Calendar.HOUR) * 30 + hourHandOffset

            canvas.rotate(hoursRotation, centerX, centerY)
            canvas.drawLine(
                    centerX,
                    centerY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    centerX,
                    centerY - sHourHandLength,
                    hourPaint
            )

            canvas.rotate(minutesRotation - hoursRotation, centerX, centerY)
            canvas.drawLine(
                    centerX,
                    centerY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    centerX,
                    centerY - sMinuteHandLength,
                    minutePaint
            )

            canvas.rotate(secondsRotation, centerX, centerY)
            canvas.drawLine(
                    centerX,
                    centerY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    centerX,
                    centerY - sMinuteHandLength,
                    secondPaint
            )
        }

        private fun getDateDiffString() : String {
            val constDate = LocalDate.parse(DATE)
            val offset = if (calendar.get(Calendar.HOUR_OF_DAY) >= 17) 0 else -1
            val currDate = LocalDate.of(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH), // Month starts from 0
                    calendar.get(Calendar.DATE) + offset
            )
            val currTime = "%02d:%02d".format(
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE)
            )
            val period = Period.between(constDate, currDate)
            val hours = LocalDateTime.parse("${DATE}T${TIME}").until(
                    LocalDateTime.parse(
                            "${currDate}T${currTime}"
                    ),
                    ChronoUnit.HOURS
            )
            return "%02d-%02d-%02d-%02d".format(period.years, period.months, period.days, hours%24)
        }

        private fun drawDurationText(canvas: Canvas) {
            canvas.drawText(
                getDateDiffString(),
                208f,
                158f,
                durationPaint
            )
        }

        private fun drawWatchFace(canvas: Canvas) {
            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
            canvas.save()

            // Draw heart and fill the color
            drawHeartAndFill(canvas)

            // Draw center dot
            drawCenterDot(canvas)

            // Draw dial markings
            drawDialMarking(canvas)

            // Draw duration text
            drawDurationText(canvas)

            // Draw hands
            drawHands(canvas)

            canvas.restore()

            //To redraw for every second
            if (!(ambient && (lowBitAmbient || burnInProtection))) {
                invalidate()
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()
                /* Update time zone in case it changed while we weren't visible. */
                calendar.timeZone = TimeZone.getDefault()
                invalidate()
            } else {
                unregisterReceiver()
            }
        }

        private fun registerReceiver() {
            if (registeredTimeZoneReceiver) {
                return
            }
            registeredTimeZoneReceiver = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@MyWatchFace.registerReceiver(timeZoneReceiver, filter)

            if (registeredBatteryInfoReceiver) {
                return
            }
            registeredBatteryInfoReceiver = true
            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            this@MyWatchFace.registerReceiver(batteryInfoReceiver, batteryFilter)
        }

        private fun unregisterReceiver() {
            if (!registeredTimeZoneReceiver) {
                return
            }
            registeredTimeZoneReceiver = false
            this@MyWatchFace.unregisterReceiver(timeZoneReceiver)

            if (!registeredBatteryInfoReceiver) {
                return
            }
            registeredBatteryInfoReceiver = false
            this@MyWatchFace.unregisterReceiver(batteryInfoReceiver)
        }
    }
}