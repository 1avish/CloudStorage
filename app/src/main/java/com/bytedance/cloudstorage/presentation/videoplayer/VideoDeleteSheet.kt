package com.bytedance.cloudstorage.presentation.videoplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
// 底部弹窗：确认删除
// ────────────────────────────────────────────────

@Composable
internal fun VideoDeleteSheet(
    fileName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.w.dp)
            .padding(bottom = 32.w.dp)
    ) {
        // 拖拽手柄
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.w.dp), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(40.w.dp, 4.w.dp).clip(RoundedCornerShape(2.w.dp)).background(BorderGray))
        }
        // 警告图标
        Box(
            modifier = Modifier
                .padding(top = 12.w.dp, bottom = 8.w.dp)
                .size(48.w.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFEBEE))
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Delete, null, tint = Color(0xFFFF3B30), modifier = Modifier.size(26.w.dp))
        }
        // 标题
        Text(
            "确认删除",
            fontSize = 20.ws.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.w.dp, bottom = 6.w.dp)
        )
        // 描述
        Text(
            "将删除 $fileName，删除后无法恢复",
            fontSize = 14.ws.sp,
            color = TextSecondary,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 24.w.dp)
        )
        // 按钮行
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.w.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.w.dp)
                    .clip(RoundedCornerShape(8.w.dp))
                    .background(SheetBg)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text("取消", fontSize = 16.ws.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.w.dp)
                    .clip(RoundedCornerShape(8.w.dp))
                    .background(Color(0xFFFF3B30))
                    .clickable { onConfirm() },
                contentAlignment = Alignment.Center
            ) {
                Text("删除", fontSize = 16.ws.sp, fontWeight = FontWeight.Medium, color = Color.White)
            }
        }
    }
}
