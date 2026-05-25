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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
// 底部弹窗：离线下载 + 上传文件
// ────────────────────────────────────────────────

@Composable
internal fun BottomSheetContent(
    onDismiss: () -> Unit,
    onNewFolder: () -> Unit = {},
    onUploadVideo: () -> Unit = {},
    onUploadDoc: () -> Unit = {},
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
                    .background(DividerColor)
            )
        }

        // ── 离线下载 ──
        Text(
            text = "离线下载",
            fontSize = 20.ws.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 4.w.dp, bottom = 12.w.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.w.dp))
                .background(Color(0xFFF5F7FA))
                .clickable { onDismiss() }
                .padding(horizontal = 16.w.dp, vertical = 16.w.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.w.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = Color(0xFF3370FF),
                    modifier = Modifier.size(22.w.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.w.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "添加磁力链",
                    fontSize = 16.ws.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.w.dp)
            )
        }

        // ── 上传文件 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.w.dp, bottom = 14.w.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "上传文件",
                fontSize = 20.ws.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onDismiss() }
            ) {
                Text(
                    text = "保存到 网盘/文件",
                    fontSize = 13.ws.sp,
                    color = TextSecondary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.w.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.w.dp)
        ) {
            listOf(
                Triple("视频", Icons.Default.OndemandVideo, Color(0xFF3370FF)),
                Triple("文档", Icons.Default.Description, Color(0xFF3370FF)),
                Triple("新建文件夹", Icons.Default.CreateNewFolder, Color(0xFF3370FF))
            ).forEach { (label, icon, iconColor) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.w.dp))
                        .background(Color(0xFFF5F7FA))
                        .clickable {
                            when (label) {
                                "新建文件夹" -> onNewFolder()
                                "视频" -> onUploadVideo()
                                "文档" -> onUploadDoc()
                                else -> onDismiss()
                            }
                        }
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
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = iconColor,
                            modifier = Modifier.size(26.w.dp)
                        )
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
        }
    }
}
