package com.bytedance.cloudstorage.presentation.videoplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws

// ────────────────────────────────────────────────
// 更多操作 Bottom Sheet
// ────────────────────────────────────────────────

/**
 * 更多操作底部弹窗，提供下载、分享、重命名、删除四项操作入口。
 *
 * @param onDownload 下载按钮回调
 * @param onShare    分享按钮回调
 * @param onRename   重命名按钮回调
 * @param onDelete   删除按钮回调
 */
@Composable
internal fun VideoMoreSheet(
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.w.dp)
            .padding(bottom = 32.w.dp)
    ) {
        // 拖拽手柄
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.w.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 40.w.dp, height = 4.w.dp)
                    .clip(RoundedCornerShape(2.w.dp))
                    .background(BorderGray)
            )
        }

        // 标题
        Text(
            text = "更多操作",
            fontSize = 20.ws.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 8.w.dp, bottom = 16.w.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.w.dp)
        ) {
            MoreSheetAction(
                label = "下载",
                icon = Icons.Filled.ArrowDownward,
                iconColor = ProgressBlue,
                onClick = onDownload,
                modifier = Modifier.weight(1f)
            )
            MoreSheetAction(
                label = "分享",
                icon = Icons.Filled.Share,
                iconColor = Color(0xFF34C759),
                onClick = onShare,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.w.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.w.dp)
        ) {
            MoreSheetAction(
                label = "重命名",
                icon = Icons.Filled.Edit,
                iconColor = ProgressBlue,
                onClick = onRename,
                modifier = Modifier.weight(1f)
            )
            MoreSheetAction(
                label = "删除",
                icon = Icons.Filled.Delete,
                iconColor = Color(0xFFFF3B30),
                onClick = onDelete,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ────────────────────────────────────────────────
// 更多操作：单个操作按钮
// ────────────────────────────────────────────────

/**
 * 更多操作弹窗中的单个操作按钮，包含彩色圆形图标和文字标签。
 *
 * @param label     按钮文字
 * @param icon      图标
 * @param iconColor 图标颜色
 * @param onClick   点击回调
 */
@Composable
private fun MoreSheetAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.w.dp))
            .background(SheetBg)
            .clickable { onClick() }
            .padding(vertical = 18.w.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(50.w.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = iconColor, modifier = Modifier.size(26.w.dp))
        }
        Spacer(modifier = Modifier.height(10.w.dp))
        Text(
            text = label,
            fontSize = 14.ws.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}
