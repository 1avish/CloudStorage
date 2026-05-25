package com.bytedance.cloudstorage.presentation.filelist

import android.os.SystemClock
import android.util.Log
import android.view.Choreographer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 列表性能监测工具（临时，测试完删除）
 *
 * 监测两项核心指标：
 *   1. 帧耗时 / 卡顿率 — 反映滑动流畅度
 *   2. 每个 item 的 recompose 次数 — 反映 Compose 重组效率
 *
 * Logcat 过滤 tag: [FileListPerf]
 */
object FileListPerfMonitor {

    private const val TAG = "FileListPerf"

    // ── 帧耗时 ──
    private val frameCount = AtomicInteger(0)
    private val jankCount = AtomicInteger(0)          // >16ms 的帧
    private val severeJankCount = AtomicInteger(0)    // >32ms 的帧
    private var lastFrameTime = AtomicLong(0L)
    private var totalFrameTime = AtomicLong(0L)

    // ── item recompose 计数 ──
    private val recomposeMap = ConcurrentHashMap<String, AtomicInteger>()

    private var isRunning = false

    // ────────────────────────────────────────────────
    // 帧监听（滑动期间连续上报）
    // ────────────────────────────────────────────────

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isRunning) return

            val now = SystemClock.elapsedRealtime()
            val last = lastFrameTime.getAndSet(now)

            if (last > 0L) {
                val frameMs = now - last
                frameCount.incrementAndGet()
                totalFrameTime.addAndGet(frameMs)

                when {
                    frameMs > 32 -> {
                        severeJankCount.incrementAndGet()
                        jankCount.incrementAndGet()
                        Log.w(TAG, "严重卡顿帧: ${frameMs}ms")
                    }
                    frameMs > 16 -> {
                        jankCount.incrementAndGet()
                        Log.w(TAG, "卡顿帧: ${frameMs}ms")
                    }
                }
            }

            // 继续监听下一帧
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    /** 开始监测，在 LazyColumn 所在 Composable 的 LaunchedEffect 中调用 */
    fun start() {
        if (isRunning) return
        isRunning = true
        reset()
        Log.i(TAG, "========== 性能监测开始 ==========")
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    /** 停止并输出汇总 */
    fun stop() {
        isRunning = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        printSummary()
    }

    // ────────────────────────────────────────────────
    // item recompose 追踪
    // ────────────────────────────────────────────────

    /** 在每个 item 顶部调用，记录该 item 被 recompose 的次数 */
    fun onRecompose(itemId: String) {
        if (!isRunning) return
        recomposeMap.getOrPut(itemId) { AtomicInteger(0) }.incrementAndGet()
    }

    // ────────────────────────────────────────────────
    // 日志输出
    // ────────────────────────────────────────────────

    private fun printSummary() {
        val total = frameCount.get()
        val jank = jankCount.get()
        val severe = severeJankCount.get()
        val totalTime = totalFrameTime.get()
        val avgFrame = if (total > 0) totalTime / total else 0L

        Log.i(TAG, "┌────────── 帧性能 ──────────")
        Log.i(TAG, "│ 总帧数        : $total")
        Log.i(TAG, "│ 卡顿帧(>16ms) : $jank")
        Log.i(TAG, "│ 严重卡顿(>32ms): $severe")
        Log.i(TAG, "│ 卡顿率        : ${if (total > 0) "%.1f%%".format(jank * 100.0 / total) else "N/A"}")
        Log.i(TAG, "│ 平均帧耗时    : ${avgFrame}ms")
        Log.i(TAG, "│ 总耗时        : ${totalTime}ms")
        Log.i(TAG, "└────────────────────────────")

        if (recomposeMap.isNotEmpty()) {
            val sorted = recomposeMap.entries.sortedByDescending { it.value.get() }
            val totalRecompose = sorted.sumOf { it.value.get() }
            Log.i(TAG, "┌──── item Recompose（前 10）────")
            sorted.take(10).forEach { (id, count) ->
                Log.i(TAG, "│ [$id] × ${count.get()}")
            }
            Log.i(TAG, "│ 共 ${sorted.size} 个 item，总 recompose: $totalRecompose")
            Log.i(TAG, "└────────────────────────────────")
        }
    }

    private fun reset() {
        frameCount.set(0)
        jankCount.set(0)
        severeJankCount.set(0)
        lastFrameTime.set(0L)
        totalFrameTime.set(0L)
        recomposeMap.clear()
    }
}

// ────────────────────────────────────────────────
// Composable 辅助：item recompose 追踪包装器
// ────────────────────────────────────────────────

/**
 * 包裹在 item 内部，通过 SideEffect 计数 recompose 次数。
 * 优化前：每个 item 每次进入屏幕可能 recompose 多次；
 * 优化后（定高 + stable）：recompose 次数应明显下降。
 */
@Composable
fun TrackRecompose(itemId: String) {
    SideEffect {
        FileListPerfMonitor.onRecompose(itemId)
    }
}
