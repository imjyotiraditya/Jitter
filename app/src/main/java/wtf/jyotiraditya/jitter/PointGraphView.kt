package wtf.jyotiraditya.jitter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import kotlin.math.floor

class PointGraphView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val paint = Paint()
    private val jitterYs = FloatArray(JITTER_LINES_MS.size)
    private val labelWidth: Float
    private val labelHeight: Float
    private val density: Float
    private var graphScale = 0f
    private var graphMaxMs = 0f
    private var jitterPoints: FloatArray = FloatArray(0)
    private var jitterAvgPoints: FloatArray = FloatArray(0)

    companion object {
        private val JITTER_LINES_MS = floatArrayOf(
            .5f, 1.0f, 1.5f, 2.0f, 3.0f, 4.0f, 5.0f
        )
        private val JITTER_LINES_LABELS = JITTER_LINES_MS.makeLabels()
        private val JITTER_LINES_COLORS = intArrayOf(
            -0xff198a, -0xe8a, -0x227cb, -0x43fd3, -0x657db,
            -0xa80e9, -0x22d400
        )

        private fun FloatArray.makeLabels(): Array<String?> {
            val ret = arrayOfNulls<String>(size)
            indices.forEach { i ->
                ret[i] = this[i].toString()
            }
            return ret
        }
    }

    init {
        setWillNotDraw(false)
        density = context.resources.displayMetrics.density
        paint.textSize = dp(10f)
        val textBounds = Rect()
        paint.getTextBounds("8.8", 0, 3, textBounds)
        labelWidth = textBounds.width() + dp(2f)
        labelHeight = textBounds.height().toFloat()
    }

    fun addJitterSample(jitterUs: Int, jitterUsAvg: Int) {
        var i = 1
        while (i < jitterPoints.size - 2) {
            jitterPoints[i] = jitterPoints[i + 2]
            jitterAvgPoints[i] = jitterAvgPoints[i + 2]
            i += 2
        }
        jitterPoints[jitterPoints.size - 1] = height - graphScale * (jitterUs / 1000.0f)
        jitterAvgPoints[jitterAvgPoints.size - 1] =
            height - graphScale * (jitterUsAvg / 1000.0f)
        invalidate()
    }

    private fun dp(dp: Float): Float {
        return density * dp
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(-0x70000000)
        val h = height
        val w = width
        paint.color = Color.WHITE
        paint.strokeWidth = dp(1f)
        canvas.drawLine(labelWidth, 0f, labelWidth, h.toFloat(), paint)
        JITTER_LINES_LABELS.indices.forEach { i ->
            canvas.drawText(
                JITTER_LINES_LABELS[i]!!, 0f,
                floor((jitterYs[i] + labelHeight * .5f).toDouble()).toFloat(), paint
            )
        }
        JITTER_LINES_LABELS.indices.forEach { i ->
            paint.color = JITTER_LINES_COLORS[i]
            canvas.drawLine(labelWidth, jitterYs[i], w.toFloat(), jitterYs[i], paint)
        }
        paint.strokeWidth = dp(2f)
        paint.color = Color.WHITE
        canvas.drawPoints(jitterPoints, paint)
        paint.color = -0xde690d
        canvas.drawPoints(jitterAvgPoints, paint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val graphWidth = ((w - labelWidth - dp(1f)) / density).toInt()
        val oldJitterPoints = jitterPoints
        val oldJitterAvgPoints = jitterAvgPoints
        jitterPoints = FloatArray(graphWidth * 2)
        jitterAvgPoints = FloatArray(graphWidth * 2)
        let {
            var i = 0
            while (i < it.jitterPoints.size) {
                it.jitterPoints[i] = it.labelWidth + (i / 2 + 1) * it.density
                it.jitterAvgPoints[i] = it.jitterPoints[i]
                i += 2
            }
        }
        val newIndexShift = (jitterPoints.size - oldJitterPoints.size).coerceAtLeast(0)
        val oldIndexShift = oldJitterPoints.size - jitterPoints.size
        var i = 1 + newIndexShift
        while (i < jitterPoints.size) {
            jitterPoints[i] = oldJitterPoints[i + oldIndexShift]
            jitterAvgPoints[i] = oldJitterAvgPoints[i + oldIndexShift]
            i += 2
        }
        graphMaxMs = JITTER_LINES_MS[JITTER_LINES_MS.size - 1] + .5f
        graphScale = h / graphMaxMs
        JITTER_LINES_MS.indices.forEach {
            jitterYs[it] = floor((h - graphScale * JITTER_LINES_MS[it]).toDouble())
                .toFloat()
        }
    }
}