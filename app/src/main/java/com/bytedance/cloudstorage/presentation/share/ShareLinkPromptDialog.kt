package com.bytedance.cloudstorage.presentation.share

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bytedance.cloudstorage.presentation.filelist.PrimaryBlue
import com.bytedance.cloudstorage.presentation.filelist.TextPrimary
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws

// ────────────────────────────────────────────────
// 分享链接提示弹窗
// ────────────────────────────────────────────────

/**
 * 分享链接已复制到剪贴板后的提示弹窗。
 *
 * 展示链接图标和「立即查看」按钮，用户点击后跳转到分享文件列表页。
 * 触发场景：在文件列表页创建分享链接成功后自动弹出。
 *
 * @param onDismiss  关闭弹窗（点击关闭图标或外部区域）
 * @param onViewNow  点击「立即查看」按钮，跳转到分享文件列表页
 */
@Composable
fun ShareLinkPromptDialog(
    onDismiss: () -> Unit,
    onViewNow: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 52.w.dp)
                .clip(RoundedCornerShape(18.w.dp))
                .background(Color.White)
                .padding(horizontal = 26.w.dp, vertical = 24.w.dp)
        ) {
            // 右上角关闭按钮
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                tint = Color(0xFF8C8C8C),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.w.dp)
                    .clickable { onDismiss() }
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(34.w.dp))
                // 链接图标
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(70.w.dp)
                )
                Spacer(modifier = Modifier.height(30.w.dp))
                Text(
                    text = "查看复制的分享链接",
                    fontSize = 22.ws.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(28.w.dp))
                // 「立即查看」按钮
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.w.dp)
                        .clip(RoundedCornerShape(29.w.dp))
                        .background(PrimaryBlue)
                        .clickable { onViewNow() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "立即查看",
                        fontSize = 22.ws.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(16.w.dp))
            }
        }
    }
}
