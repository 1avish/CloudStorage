package com.bytedance.cloudstorage.presentation.videoplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws

// ────────────────────────────────────────────────
// 底部弹窗：重命名
// ────────────────────────────────────────────────

/**
 * 重命名底部弹窗，打开时自动聚焦输入框并填入当前文件名。
 *
 * @param currentName 当前文件名（作为初始值）
 * @param onDismiss   取消/关闭回调
 * @param onConfirm   确认重命名回调，参数为新文件名
 */
@Composable
internal fun VideoRenameSheet(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.w.dp)
            .padding(bottom = 32.w.dp)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                focusManager.clearFocus()
            }
    ) {
        // 拖拽手柄
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.w.dp), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(40.w.dp, 4.w.dp).clip(RoundedCornerShape(2.w.dp)).background(BorderGray))
        }
        // 标题
        Text("重命名", fontSize = 20.ws.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(top = 8.w.dp, bottom = 20.w.dp))
        // 输入框
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.w.dp)
                .clip(RoundedCornerShape(8.w.dp))
                .background(SheetBg)
                .padding(horizontal = 14.w.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (newName.isEmpty()) {
                Text("请输入新文件名", fontSize = 15.ws.sp, color = TextSecondary)
            }
            BasicTextField(
                value = newName,
                onValueChange = { newName = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                textStyle = TextStyle(fontSize = 15.ws.sp, color = TextPrimary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (newName.isNotBlank()) onConfirm(newName.trim())
                })
            )
        }
        Spacer(modifier = Modifier.height(24.w.dp))
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
                    .background(ProgressBlue)
                    .clickable(enabled = newName.isNotBlank()) { onConfirm(newName.trim()) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "确认",
                    fontSize = 16.ws.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (newName.isNotBlank()) Color.White else Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}
