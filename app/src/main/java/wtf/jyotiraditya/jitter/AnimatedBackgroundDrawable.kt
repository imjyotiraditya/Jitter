package wtf.jyotiraditya.jitter

import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.animation.AnimationUtils

class AnimatedBackgroundDrawable : Drawable() {

    private val paint: Paint = Paint()
    private var reverse = false
    private var startTime: Long = 0
    private var color = 0
    private var reverseX = false
    private var reverseY = false
    private var x = 0f
    private var y = 0f
    private var radius = 0f
    private var moveStep = 10.0f

    companion object {
        private const val FROM_COLOR = -0xe70001
        private const val TO_COLOR = -0xbf3b01
        private const val DURATION = 1400
    }

    init {
        paint.color = -0x100
        paint.isAntiAlias = true
    }

    override fun draw(canvas: Canvas) {
        stepColor()
        canvas.drawColor(color)
        x += if (reverseX) -moveStep else moveStep
        y += if (reverseY) -moveStep else moveStep
        clampXY()
        canvas.drawCircle(x, y, radius, paint)
        invalidateSelf()
    }

    private fun clampXY() {
        if (x <= radius) {
            reverseX = false
            x = radius
        }
        if (y <= radius) {
            reverseY = false
            y = radius
        }
        val maxX = bounds.width() - radius
        if (x >= maxX) {
            reverseX = true
            x = maxX
        }
        val maxY = bounds.height() - radius
        if (y >= maxY) {
            reverseY = true
            y = maxY
        }
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        moveStep = bounds.width().coerceAtMost(bounds.height()) / 130.0f
        radius = bounds.width().coerceAtMost(bounds.height()) / 20.0f
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

    private fun stepColor() {
        if (startTime == 0L) {
            startTime = AnimationUtils.currentAnimationTimeMillis()
        }
        var fraction = ((AnimationUtils.currentAnimationTimeMillis() - startTime)
                / DURATION.toFloat())
        if (fraction > 1.0f) fraction = 1.0f
        val dest = if (reverse) FROM_COLOR else TO_COLOR
        val src = if (reverse) TO_COLOR else FROM_COLOR
        val r = (Color.red(src) + (Color.red(dest) - Color.red(src)) * fraction).toInt()
        val g = (Color.green(src) + (Color.green(dest) - Color.green(src)) * fraction).toInt()
        val b = (Color.blue(src) + (Color.blue(dest) - Color.blue(src)) * fraction).toInt()
        color = Color.rgb(r, g, b)
        if (fraction == 1.0f) {
            startTime = 0
            reverse = !reverse
        }
    }
}