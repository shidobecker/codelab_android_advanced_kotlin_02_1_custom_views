package com.example.android.customfancontroller

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.withStyledAttributes
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import java.lang.Integer.min

private const val RADIUS_OFFSET_LABEL = 30
private const val RADIUS_OFFSET_INDICATOR = -35


class DialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private enum class FanSpeed(val label: Int) {
        OFF(R.string.fan_off),
        LOW(R.string.fan_low),
        MEDIUM(R.string.fan_medium),
        HIGH(R.string.fan_high);

        fun next() = when (this) {
            OFF -> LOW
            LOW -> MEDIUM
            MEDIUM -> HIGH
            HIGH -> OFF
        }
    }

    init {
        isClickable = true

        context.withStyledAttributes(attrs, R.styleable.DialView) {
            fanSpeedLowColor = getColor(R.styleable.DialView_fanColor1, 0)
            fanSpeedMediumColor = getColor(R.styleable.DialView_fanColor2, 0)
            fanSpeedMaxColor = getColor(R.styleable.DialView_fanColor3, 0)
        }

        updateContentDescription()


        ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                val customClick = AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    AccessibilityNodeInfo.ACTION_CLICK,
                    context.getString(if (fanSpeed !=  FanSpeed.HIGH) R.string.change else R.string.reset)

                )

                info.addAction(customClick)

            }
        })
    }

    private var radius = 0.0f                   // Radius of the circle.
    private var fanSpeed = FanSpeed.OFF         // The active selection.

    // position variable which will be used to draw label and indicator circle position
    private val pointPosition: PointF = PointF(0.0f, 0.0f)


    private var fanSpeedLowColor = 0
    private var fanSpeedMediumColor = 0
    private var fanSpeedMaxColor = 0

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = 55.0f
        typeface = Typeface.create("", Typeface.BOLD)
    }

    /**
     * The onSizeChanged() method is called any time the view's size changes,
     * including the first time it is drawn when the layout is inflated. Override onSizeChanged()
     * to calculate positions, dimensions, and any other values related to your custom view's size,
     * instead of recalculating them every time you draw. In this case you use onSizeChanged()
     * to calculate the current radius of the dial's circle element.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        radius = (min(width, height) / 2.0 * 0.8).toFloat()
    }

    /**
     * This extension function on the PointF class calculates the X, Y coordinates on the screen
     * for the text label and current indicator (0, 1, 2, or 3), given the current FanSpeed position
     * and radius of the dial. You'll use this in onDraw().
     */
    private fun PointF.computeXYForSpeed(pos: FanSpeed, radius: Float) {
        // Angles are in radians.
        val startAngle = Math.PI * (9 / 8.0)
        val angle = startAngle + pos.ordinal * (Math.PI / 4)
        x = (radius * kotlin.math.cos(angle)).toFloat() + width / 2
        y = (radius * kotlin.math.sin(angle)).toFloat() + height / 2
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        // Set dial background color to green if selection not off.
        paint.color = when (fanSpeed) {
            FanSpeed.OFF -> Color.GRAY
            FanSpeed.LOW -> fanSpeedLowColor
            FanSpeed.MEDIUM -> fanSpeedMediumColor
            FanSpeed.HIGH -> fanSpeedMaxColor
        }

        // Draw the dial.
        /**
         * with the drawCircle() method. This method uses the current view width and height to
         * find the center of the circle, the radius of the circle, and the current paint color.
         * The width and height properties are members of the View superclass and indicate the
         * current dimensions of the view.
         */
        canvas?.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), radius, paint)

        // Draw the indicator circle.
        val markerRadius = radius + RADIUS_OFFSET_INDICATOR
        pointPosition.computeXYForSpeed(fanSpeed, markerRadius)
        paint.color = Color.BLACK
        canvas?.drawCircle(pointPosition.x, pointPosition.y, radius / 12, paint)

        // Draw the text labels.
        val labelRadius = radius + RADIUS_OFFSET_LABEL
        for (i in FanSpeed.values()) {
            pointPosition.computeXYForSpeed(i, labelRadius)
            val label = resources.getString(i.label)
            canvas?.drawText(label, pointPosition.x, pointPosition.y, paint)
        }
    }


    /**
     * Normally, with a standard Android view, you implement OnClickListener() to perform an action
     * when the user clicks that view. For a custom view, you implement the View class's
     * performClick``() method instead, and call super.performClick(). The default performClick()
     * method also calls onClickListener(), so you can add your actions to performClick() and leave
     * onClickListener() available for further customization by you or other developers that might
     * use your custom view.
     *
     *
     * The call to super.performClick() must happen first, which enables accessibility events as
     * well as calls onClickListener().

    The next two lines increment the speed of the fan with the next() method, and set the view's
    content description to the string resource representing the current speed (off, 1, 2 or 3).

    FInally, the invalidate() method invalidates the entire view, forcing a call to onDraw() to
    redraw the view. If something in your custom view changes for any reason, including user
    interaction, and the change needs to be displayed, call invalidate().
     */
    override fun performClick(): Boolean {
        if (super.performClick()) return true

        fanSpeed = fanSpeed.next()
        contentDescription = resources.getString(fanSpeed.label)

        updateContentDescription()
        invalidate()
        return true
    }

    /**
     * Content descriptions describe the meaning and purpose of the views in your app.
     * These labels allow screen readers such as Android's TalkBack feature to explain the function
     * of each element accurately. For static views such as ImageView, you can add the content
     * description to the view in the layout file with the contentDescription attribute.
     * Text views (TextView and EditText) automatically use the text in the view as the content
     * description.

    For the custom fan control view, you need to dynamically update the content description each
    time the view is clicked, to indicate the current fan setting.
     */
    fun updateContentDescription() {
        contentDescription = resources.getString(fanSpeed.label)
    }

}