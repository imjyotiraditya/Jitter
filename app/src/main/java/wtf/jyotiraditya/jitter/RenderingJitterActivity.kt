package wtf.jyotiraditya.jitter

import android.app.Activity
import android.os.*
import android.view.FrameMetrics
import android.view.View
import android.view.Window
import android.view.Window.OnFrameMetricsAvailableListener
import android.widget.TextView
import kotlin.math.abs

class RenderingJitterActivity : Activity() {

    private var jitterReport: TextView? = null
    private var uiFrameTimeReport: TextView? = null
    private var renderThreadTimeReport: TextView? = null
    private var totalFrameTimeReport: TextView? = null
    private var mostlyTotalFrameTimeReport: TextView? = null
    private var graph: PointGraphView? = null

    companion object {
        private var frameMetricsHandler: Handler? = null

        init {
            val thread = HandlerThread("frameMetricsListener")
            thread.start()
            frameMetricsHandler = Handler(thread.looper)
        }
    }

    private val updateHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                R.id.jitter_mma -> jitterReport?.text = msg.obj as CharSequence
                R.id.totalish_mma -> mostlyTotalFrameTimeReport?.text = msg.obj as CharSequence
                R.id.ui_frametime_mma -> uiFrameTimeReport?.text = msg.obj as CharSequence
                R.id.rt_frametime_mma -> renderThreadTimeReport?.text = msg.obj as CharSequence
                R.id.total_mma -> totalFrameTimeReport?.text = msg.obj as CharSequence
                R.id.graph -> graph?.addJitterSample(msg.arg1, msg.arg2)
            }
        }
    }

    private val frameMetricsListener: OnFrameMetricsAvailableListener =
        object : OnFrameMetricsAvailableListener {
            private val WEIGHT = 40.0
            private var previousFrameTotal: Long = 0
            private var jitterMma = 0.0
            private var uiFrameTimeMma = 0.0
            private var rtFrameTimeMma = 0.0
            private var totalFrameTimeMma = 0.0
            private var mostlyTotalFrameTimeMma = 0.0
            private var needsFirstValues = true
            override fun onFrameMetricsAvailable(
                window: Window, frameMetrics: FrameMetrics,
                dropCountSinceLastInvocation: Int,
            ) {
                if (frameMetrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 1L) {
                    return
                }
                val uiDuration = (frameMetrics.getMetric(FrameMetrics.INPUT_HANDLING_DURATION)
                        + frameMetrics.getMetric(FrameMetrics.ANIMATION_DURATION)
                        + frameMetrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION)
                        + frameMetrics.getMetric(FrameMetrics.DRAW_DURATION))
                val rtDuration = (frameMetrics.getMetric(FrameMetrics.SYNC_DURATION)
                        + frameMetrics.getMetric(FrameMetrics.COMMAND_ISSUE_DURATION))
                val totalDuration = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION)
                val jitter = abs(totalDuration - previousFrameTotal)
                if (needsFirstValues) {
                    jitterMma = 0.0
                    uiFrameTimeMma = uiDuration.toDouble()
                    rtFrameTimeMma = rtDuration.toDouble()
                    totalFrameTimeMma = totalDuration.toDouble()
                    mostlyTotalFrameTimeMma = (uiDuration + rtDuration).toDouble()
                    needsFirstValues = false
                } else {
                    jitterMma = add(jitterMma, jitter.toDouble())
                    uiFrameTimeMma = add(uiFrameTimeMma, uiDuration.toDouble())
                    rtFrameTimeMma = add(rtFrameTimeMma, rtDuration.toDouble())
                    totalFrameTimeMma = add(totalFrameTimeMma, totalDuration.toDouble())
                    mostlyTotalFrameTimeMma =
                        add(mostlyTotalFrameTimeMma, (uiDuration + rtDuration).toDouble())
                }
                previousFrameTotal = totalDuration
                updateHandler.obtainMessage(
                    R.id.jitter_mma,
                    String.format("Jitter: %.3fms", toMs(jitterMma))
                ).sendToTarget()
                updateHandler.obtainMessage(
                    R.id.totalish_mma,
                    String.format("CPU-total duration: %.3fms", toMs(mostlyTotalFrameTimeMma))
                ).sendToTarget()
                updateHandler.obtainMessage(
                    R.id.ui_frametime_mma,
                    String.format("UI duration: %.3fms", toMs(uiFrameTimeMma))
                ).sendToTarget()
                updateHandler.obtainMessage(
                    R.id.rt_frametime_mma,
                    String.format("RT duration: %.3fms", toMs(rtFrameTimeMma))
                ).sendToTarget()
                updateHandler.obtainMessage(
                    R.id.total_mma,
                    String.format("Total duration: %.3fms", toMs(totalFrameTimeMma))
                ).sendToTarget()
                updateHandler.obtainMessage(
                    R.id.graph, (jitter / 1000).toInt(),
                    (jitterMma / 1000).toInt()
                ).sendToTarget()
            }

            fun add(previous: Double, today: Double): Double {
                return ((WEIGHT - 1) * previous + today) / WEIGHT
            }

            fun toMs(value: Double): Double {
                return value / 1000000
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.rendering_jitter)
        val content = findViewById<View>(android.R.id.content)
        content.background = AnimatedBackgroundDrawable()
        content.keepScreenOn = true
        jitterReport = findViewById(R.id.jitter_mma)
        mostlyTotalFrameTimeReport = findViewById(R.id.totalish_mma)
        uiFrameTimeReport = findViewById(R.id.ui_frametime_mma)
        renderThreadTimeReport = findViewById(R.id.rt_frametime_mma)
        totalFrameTimeReport = findViewById(R.id.total_mma)
        graph = findViewById(R.id.graph)
        jitterReport?.text = "abcdefghijklmnopqrstuvwxyz"
        mostlyTotalFrameTimeReport?.text = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        uiFrameTimeReport?.text = "012345689"
        renderThreadTimeReport?.text = ",.!()[]{};"
        window.addOnFrameMetricsAvailableListener(frameMetricsListener, frameMetricsHandler)
    }
}