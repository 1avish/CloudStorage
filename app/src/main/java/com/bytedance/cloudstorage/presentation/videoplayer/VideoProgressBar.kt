package com.bytedance.cloudstorage.presentation.videoplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun VideoProgressBar(
    viewModel: VideoPlayerViewModel,
    durationMs: Long,
    onSeek: (Float) -> Unit,
    touchTargetHeight: Dp = 28.dp,
    trackHeight: Dp = 3.dp,
    trackColor: Color = Color.White.copy(alpha = 0.28f),
    progressColor: Color = ProgressBlue,
    thumbColor: Color = Color.White,
    thumbSize: Dp = 14.dp,
) {
    val currentPos by viewModel.currentPosition.collectAsStateWithLifecycle()
    val progress = if (durationMs > 0) (currentPos.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    var trackWidthPx by remember { mutableStateOf(0f) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    var pendingSeekFraction by remember { mutableStateOf<Float?>(null) }
    val displayProgress = dragFraction ?: pendingSeekFraction ?: progress

    LaunchedEffect(currentPos, durationMs, pendingSeekFraction, dragFraction) {
        val targetFraction = pendingSeekFraction ?: return@LaunchedEffect
        if (dragFraction != null || durationMs <= 0L) return@LaunchedEffect
        val targetPos = (targetFraction * durationMs).toLong()
        if (abs(currentPos - targetPos) <= 500L) {
            pendingSeekFraction = null
        }
    }

    LaunchedEffect(pendingSeekFraction) {
        if (pendingSeekFraction != null) {
            kotlinx.coroutines.delay(700)
            pendingSeekFraction = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(touchTargetHeight)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (trackWidthPx > 0f) {
                            dragFraction = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                        }
                    },
                    onDragEnd = {
                        dragFraction?.let {
                            pendingSeekFraction = it
                            onSeek(it)
                        }
                        dragFraction = null
                    },
                    onDragCancel = { dragFraction = null },
                    onHorizontalDrag = { change, _ ->
                        if (trackWidthPx > 0f) {
                            dragFraction = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                        }
                        change.consume()
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(RoundedCornerShape(2.dp))
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(displayProgress)
                    .height(trackHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(progressColor)
            )
        }
        if (trackWidthPx > 0f) {
            Box(
                modifier = Modifier
                    .offset { IntOffset((trackWidthPx * displayProgress - thumbSize.toPx()).roundToInt(), 0) }
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(thumbColor)
            )
        }
    }
}
