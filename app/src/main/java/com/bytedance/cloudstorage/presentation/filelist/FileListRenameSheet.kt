package com.bytedance.cloudstorage.presentation.filelist

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
import com.bytedance.cloudstorage.domain.model.CloudFile
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws

// ────────────────────────────────────────────────
// 底部弹窗：重命名文件
// ────────────────────────────────────────────────

@Composable
internal fun RenameBottomSheet(
    file: CloudFile,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var fileName by remember { mutableStateOf(file.name) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.w.dp)
            .padding(bottom = 32.w.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { focusManager.clearFocus() }
    ) {
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

        Text(
            text = "重命名",
            fontSize = 20.ws.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 8.w.dp, bottom = 20.w.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.w.dp)
                .clip(RoundedCornerShape(8.w.dp))
                .background(Color(0xFFF5F7FA))
                .padding(horizontal = 14.w.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (fileName.isEmpty()) {
                Text(
                    text = "请输入新文件名",
                    fontSize = 15.ws.sp,
                    color = TextSecondary
                )
            }
            BasicTextField(
                value = fileName,
                onValueChange = { fileName = it },
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
                    if (fileName.isNotBlank()) onConfirm(fileName.trim())
                })
            )
        }

        Spacer(modifier = Modifier.height(24.w.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.w.dp)
        ) {
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

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.w.dp)
                    .clip(RoundedCornerShape(8.w.dp))
                    .background(Color(0xFF3370FF))
                    .clickable(enabled = fileName.isNotBlank()) {
                        onConfirm(fileName.trim())
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "确认",
                    fontSize = 16.ws.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (fileName.isNotBlank()) Color.White
                    else Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}
