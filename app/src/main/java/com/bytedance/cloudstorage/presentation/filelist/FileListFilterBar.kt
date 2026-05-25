package com.bytedance.cloudstorage.presentation.filelist

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws

// ────────────────────────────────────────────────
// 胶囊分段控件（滑动指示器 + 无波纹）
// ────────────────────────────────────────────────

@Composable
internal fun CapsuleSegmentedControl(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = FileFilter.entries
    var totalWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .height(40.w.dp)
            .clip(RoundedCornerShape(50))
            .background(CapsuleBg)
            .padding(4.w.dp)
            .onSizeChanged { totalWidth = it.width }
    ) {
        if (totalWidth > 0) {
            val itemWidthDp = with(density) { totalWidth.toDp() } / filters.size
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidthDp * selectedIndex,
                animationSpec = tween(durationMillis = 300),
                label = "capsuleSlide"
            )

            // 滑动指示器白球
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .size(itemWidthDp, 32.w.dp)
                    .padding(horizontal = 2.w.dp)
                    .shadow(2.dp, RoundedCornerShape(50))
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
            )
        }

        // 文字层
        Row(modifier = Modifier.fillMaxSize().padding(vertical = 4.w.dp)) {
            filters.forEachIndexed { index, filter ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.label,
                        fontSize = 16.ws.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) TextPrimary else TextSecondary
                    )
                }
            }
        }
    }
}
