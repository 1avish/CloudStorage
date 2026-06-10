package com.bytedance.cloudstorage.presentation.filelist

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OndemandVideo
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
// 底部弹窗：保存分享 + 新建文件夹 + 上传文件
// ────────────────────────────────────────────────

/**
 * 创建/上传底部弹窗内容，提供视频上传、文档上传、新建文件夹、保存分享四项入口。
 *
 * 采用四宫格布局，每个入口为彩色圆角方块图标 + 文字标签。
 *
 * @param onDismiss     关闭弹窗回调
 * @param onSaveShare   保存分享入口回调
 * @param onNewFolder   新建文件夹入口回调
 * @param onUploadVideo 上传视频入口回调
 * @param onUploadDoc   上传文档入口回调
 */
@Composable
internal fun BottomSheetContent(
    onDismiss: () -> Unit,
    onSaveShare: () -> Unit = {},
    onNewFolder: () -> Unit = {},
    onUploadVideo: () -> Unit = {},
    onUploadDoc: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.w.dp)
            .padding(top = 8.w.dp, bottom = 16.w.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.w.dp,bottom = 16.w.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "上传文件",
                    fontSize = 22.ws.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.w.dp))
                Text(
                    text = "文件将保存至「网盘」",
                    fontSize = 12.ws.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.w.dp)
                    .clip(RoundedCornerShape(22.w.dp))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.w.dp)
                )
            }
        }

        val actions = listOf(
            Triple("视频", Icons.Default.OndemandVideo, Color(0xFF9B6CFF)),
            Triple("文档", Icons.Default.Description, Color(0xFF58D27F)),
            Triple("新建文件夹", Icons.Default.CreateNewFolder, Color(0xFF7389FF)),
            Triple("保存分享", Icons.Default.Link, PrimaryBlue)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            actions.forEach { (label, icon, iconColor) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            when (label) {
                                "保存分享" -> onSaveShare()
                                "新建文件夹" -> onNewFolder()
                                "视频" -> onUploadVideo()
                                "文档" -> onUploadDoc()
                                else -> onDismiss()
                            }
                        }
                        .padding(vertical = 4.w.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.w.dp)
                            .clip(RoundedCornerShape(10.w.dp))
                            .background(iconColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (iconColor == PrimaryBlue) TextPrimary else Color.White,
                            modifier = Modifier.size(31.w.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.w.dp))
                    Text(
                        text = label,
                        fontSize = 15.ws.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
