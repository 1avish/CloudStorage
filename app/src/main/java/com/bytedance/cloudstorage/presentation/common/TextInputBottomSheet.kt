package com.bytedance.cloudstorage.presentation.common

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
import com.bytedance.cloudstorage.presentation.filelist.DividerColor
import com.bytedance.cloudstorage.presentation.filelist.PrimaryBlue
import com.bytedance.cloudstorage.presentation.filelist.TextPrimary
import com.bytedance.cloudstorage.presentation.filelist.TextSecondary
import com.bytedance.cloudstorage.presentation.filelist.sanitizeFileName
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws

// ────────────────────────────────────────────────
// 通用文本输入底部弹窗
// ────────────────────────────────────────────────

/**
 * 通用文本输入 BottomSheet 组件。
 *
 * 用于新建文件夹命名、文件重命名等场景，替换原先独立的
 * [FileListNewFolderSheet] 和 [FileListRenameSheet]。
 *
 * 自动聚焦输入框，键盘 Done 键或点击「确认」按钮触发回调。
 * 点击弹窗背景空白区域可收起键盘。
 *
 * @param title       弹窗标题，如「新建文件夹」「重命名」
 * @param placeholder 输入框占位文字
 * @param initialValue 初始值，重命名场景传入原名
 * @param maxLength   最大输入字符数，默认 50
 * @param onDismiss   取消/关闭回调
 * @param onConfirm   确认回调，返回经 sanitizeFileName 清理后的文本
 */
@Composable
internal fun TextInputBottomSheet(
    title: String,
    placeholder: String,
    initialValue: String = "",
    maxLength: Int = 50,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var inputValue by remember(initialValue) { mutableStateOf(initialValue) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // 弹窗打开后自动聚焦输入框
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.w.dp)
            .padding(bottom = 32.w.dp)
            // 点击空白区域收起键盘
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { focusManager.clearFocus() }
    ) {
        // 顶部拖拽指示条
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

        // 标题
        Text(
            text = title,
            fontSize = 20.ws.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 8.w.dp, bottom = 20.w.dp)
        )

        // 输入框区域：占位文字 + BasicTextField 叠放
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.w.dp)
                .clip(RoundedCornerShape(8.w.dp))
                .background(Color(0xFFF5F7FA))
                .padding(horizontal = 14.w.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // 输入为空时显示占位文字
            if (inputValue.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 15.ws.sp,
                    color = TextSecondary
                )
            }
            BasicTextField(
                value = inputValue,
                onValueChange = { raw ->
                    // 实时过滤非法字符并限制长度
                    val filtered = raw.replace(Regex("[\\\\/:*?\"<>|\\x00-\\x1f]"), "")
                    inputValue = if (filtered.length > maxLength) filtered.substring(0, maxLength) else filtered
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    fontSize = 15.ws.sp,
                    color = TextPrimary
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (inputValue.isNotBlank()) onConfirm(sanitizeFileName(inputValue))
                })
            )
        }

        Spacer(modifier = Modifier.height(24.w.dp))

        // 取消 / 确认 双按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.w.dp)
        ) {
            // 取消按钮
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.w.dp)
                    .clip(RoundedCornerShape(8.w.dp))
                    .background(Color(0xFFF5F7FA))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "取消",
                    fontSize = 16.ws.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }

            // 确认按钮：输入为空时文字半透明提示不可用
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.w.dp)
                    .clip(RoundedCornerShape(8.w.dp))
                    .background(PrimaryBlue)
                    .clickable(enabled = inputValue.isNotBlank()) {
                        onConfirm(sanitizeFileName(inputValue))
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "确认",
                    fontSize = 16.ws.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (inputValue.isNotBlank()) Color.White
                    else Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}
