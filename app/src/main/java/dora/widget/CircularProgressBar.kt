package dora.widget

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Cap
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Property
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator

class CircularProgressBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private val bounds = RectF()
    private lateinit var sweepAnimator: ObjectAnimator
    private lateinit var angleAnimator: ObjectAnimator
    private var appearingMode = true
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var curGlobalAngleOffset = 0f
    private var curGlobalAngle = 0f
    private var curSweepAngle = 0f
    private val progressWidth: Float
    private val progressColor: Int
    private var isRunning = false
    private val colors: IntArray
    private var curColorIndex: Int
    private var nextColorIndex: Int

    private fun start() {
        if (!isRunning) {
            isRunning = true
            angleAnimator?.start()
            sweepAnimator?.start()
            postInvalidate()
        }
    }

    private fun stop() {
        if (isRunning) {
            isRunning = false
            angleAnimator?.cancel()
            sweepAnimator?.cancel()
            postInvalidate()
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            start()
        } else {
            stop()
        }
    }

    override fun onAttachedToWindow() {
        start()
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bounds.left = progressWidth / 2f + .5f
        bounds.right = w - progressWidth / 2f - .5f
        bounds.top = progressWidth / 2f + .5f
        bounds.bottom = h - progressWidth / 2f - .5f
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        var startAngle = curGlobalAngle - curGlobalAngleOffset
        var sweepAngle = curSweepAngle
        if (appearingMode) {
            paint.color = gradient(
                colors[curColorIndex], colors[nextColorIndex],
                curSweepAngle / (360 - MIN_SWEEP_ANGLE * 2)
            )
            sweepAngle += MIN_SWEEP_ANGLE.toFloat()
        } else {
            startAngle += sweepAngle
            sweepAngle = 360 - sweepAngle - MIN_SWEEP_ANGLE
        }
        canvas.drawArc(bounds, startAngle, sweepAngle, false, paint)
    }

    private val angleProperty: Property<CircularProgressBar, Float> =
        object : Property<CircularProgressBar, Float>(
            Float::class.java, "angle"
        ) {
            override fun get(`object`: CircularProgressBar): Float {
                return `object`.currentGlobalAngle
            }

            override fun set(`object`: CircularProgressBar, value: Float) {
                `object`.currentGlobalAngle = value
            }
        }

    private val sweepProperty: Property<CircularProgressBar, Float> =
        object : Property<CircularProgressBar, Float>(
            Float::class.java, "arc"
        ) {
            override fun get(`object`: CircularProgressBar): Float {
                return `object`.currentSweepAngle
            }

            override fun set(`object`: CircularProgressBar, value: Float) {
                `object`.currentSweepAngle = value
            }
        }

    private fun initAnimations() {
        angleAnimator = ObjectAnimator.ofFloat(this, angleProperty, 360f)
        angleAnimator.interpolator = ANGLE_INTERPOLATOR
        angleAnimator.duration = ANGLE_ANIMATOR_DURATION.toLong()
        angleAnimator.repeatMode = ValueAnimator.RESTART
        angleAnimator.repeatCount = ValueAnimator.INFINITE
        sweepAnimator = ObjectAnimator.ofFloat(this, sweepProperty, 360f - MIN_SWEEP_ANGLE * 2)
        sweepAnimator.interpolator = SWEEP_INTERPOLATOR
        sweepAnimator.duration = SWEEP_ANIMATOR_DURATION.toLong()
        sweepAnimator.repeatMode = ValueAnimator.RESTART
        sweepAnimator.repeatCount = ValueAnimator.INFINITE
    }

    var currentGlobalAngle: Float
        get() = curGlobalAngle
        set(currentGlobalAngle) {
            curGlobalAngle = currentGlobalAngle
            postInvalidate()
        }
    var currentSweepAngle: Float
        get() = curSweepAngle
        set(currentSweepAngle) {
            curSweepAngle = currentSweepAngle
            postInvalidate()
        }

    companion object {
        private val ANGLE_INTERPOLATOR: Interpolator = LinearInterpolator()
        private val SWEEP_INTERPOLATOR: Interpolator = AccelerateDecelerateInterpolator()
        private const val ANGLE_ANIMATOR_DURATION = 3000
        private const val SWEEP_ANIMATOR_DURATION = 1200
        private const val MIN_SWEEP_ANGLE = 30
        private fun gradient(color1: Int, color2: Int, p: Float): Int {
            val r1 = color1 and 0xff0000 shr 16
            val g1 = color1 and 0xff00 shr 8
            val b1 = color1 and 0xff
            val r2 = color2 and 0xff0000 shr 16
            val g2 = color2 and 0xff00 shr 8
            val b2 = color2 and 0xff
            val newr = (r2 * p + r1 * (1 - p)).toInt()
            val newg = (g2 * p + g1 * (1 - p)).toInt()
            val newb = (b2 * p + b1 * (1 - p)).toInt()
            return Color.argb(255, newr, newg, newb)
        }
    }

    init {
        val density = context.resources.displayMetrics.density
        val a = context.obtainStyledAttributes(attrs, R.styleable.CircularProgressBar, defStyleAttr, 0)
        progressWidth = a.getDimension(
            R.styleable.CircularProgressBar_dora_progressWidth,
            10 * density
        )
        progressColor = a.getColor(R.styleable.CircularProgressBar_dora_progressColor, Color.BLACK);
        a.recycle()
        colors = IntArray(4)
        colors[0] = progressColor
        colors[1] = progressColor
        colors[2] = progressColor
        colors[3] = progressColor
        curColorIndex = 0
        nextColorIndex = 1
        initPaints()
        initAnimations()
    }

    private fun initPaints() {
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Cap.ROUND
        paint.strokeWidth = progressWidth
        paint.color = colors[curColorIndex]
    }
}