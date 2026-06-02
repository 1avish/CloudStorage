package com.bytedance.cloudstorage.presentation.filelist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytedance.cloudstorage.domain.model.CloudFile
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws

private val MoveBreadcrumbGray = Color(0xFF8C93A4)
private val MoveBreadcrumbDivider = Color(0xFFD0D3D9)

// ────────────────────────────────────────────────
// 移动目标文件夹选择器（Bottom Sheet 内容）
// ────────────────────────────────────────────────

/**
 * 移动目标文件夹选择器
 *
 * 提供文件夹层级浏览、面包屑导航和新建文件夹能力，
 * 用户选定目标文件夹后点击「保存到此处」完成移动。
 *
 * @param pathStack    从根目录到当前层级的文件夹名称列表
 * @param folders      当前层级下的子文件夹列表
 * @param onNavigateInto 进入子文件夹回调（文件夹 ID、名称）
 * @param onNavigateBack 返回上一级回调
 * @param onPathClick    点击面包屑跳转到指定层级回调（index）
 * @param onConfirmMove  确认移动到当前目录回调
 * @param onDismiss      关闭选择器回调
 * @param onNewFolder    新建文件夹回调
 */
@Composable
internal fun MoveFolderPickerSheet(
    pathStack: List<String>,
    folders: List<CloudFile>,
    onNavigateInto: (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    onPathClick: (Int) -> Unit,
    onConfirmMove: () -> Unit,
    onDismiss: () -> Unit,
    onNewFolder: () -> Unit,
) {
    val listDragBlocker = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset = available
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.82f)
            .background(Color.White)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.w.dp)
                .pointerInput(onDismiss) {
                    val dismissThreshold = 80.dp.toPx()
                    var totalDrag = 0f
                    var dismissed = false
                    detectVerticalDragGestures(
                        onDragStart = {
                            totalDrag = 0f
                            dismissed = false
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (dragAmount > 0f) {
                                totalDrag += dragAmount
                                change.consume()
                                if (!dismissed && totalDrag >= dismissThreshold) {
                                    dismissed = true
                                    onDismiss()
                                }
                            }
                        },
                        onDragEnd = { totalDrag = 0f },
                        onDragCancel = { totalDrag = 0f }
                    )
                }
                .padding(horizontal = 20.w.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = TextPrimary,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(30.w.dp)
                    .clickable {
                        if (pathStack.isEmpty()) onDismiss() else onNavigateBack()
                    }
                    .padding(3.w.dp)
            )

            Text(
                text = "选择文件夹",
                fontSize = 20.ws.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                tint = TextPrimary,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(32.w.dp)
                    .clickable { onDismiss() }
                    .padding(2.w.dp)
            )
        }

        MoveBreadcrumb(
            pathStack = pathStack,
            onPathClick = onPathClick
        )

        Spacer(modifier = Modifier.height(12.w.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .nestedScroll(listDragBlocker),
            verticalArrangement = Arrangement.spacedBy(6.w.dp)
        ) {
            items(folders, key = { it.id }) { folder ->
                MoveFolderRow(
                    folder = folder,
                    onClick = { onNavigateInto(folder.id, folder.name) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.w.dp, bottom = 12.w.dp),
            horizontalArrangement = Arrangement.spacedBy(12.w.dp)
        ) {
            MovePickerButton(
                text = "新建文件夹",
                textColor = TextPrimary,
                background = Color(0xFFF5F5F5),
                modifier = Modifier.weight(1f),
                onClick = onNewFolder
            )
            MovePickerButton(
                text = "保存到此处",
                textColor = Color.White,
                background = Color(0xFF3370FF),
                modifier = Modifier.weight(1f),
                onClick = onConfirmMove
            )
        }
    }
}

// ── 面包屑导航栏 ──

@Composable
private fun MoveBreadcrumb(
    pathStack: List<String>,
    onPathClick: (Int) -> Unit,
) {
    val fullPath = listOf("我的网盘") + pathStack
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.w.dp, vertical = 6.w.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        fullPath.forEachIndexed { index, name ->
            if (index > 0) {
                Text(
                    text = " / ",
                    fontSize = 16.ws.sp,
                    fontWeight = FontWeight.Normal,
                    color = MoveBreadcrumbDivider
                )
            }

            val isCurrent = index == fullPath.lastIndex
            Text(
                text = name,
                fontSize = 16.ws.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) TextPrimary else MoveBreadcrumbGray,
                modifier = if (isCurrent) Modifier else Modifier.clickable { onPathClick(index) },
                maxLines = 1
            )
        }
    }
}

// ── 单个文件夹行项 ──

@Composable
private fun MoveFolderRow(
    folder: CloudFile,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.w.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.w.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.w.dp)
                .clip(RoundedCornerShape(14.w.dp))
                .background(IconFolderBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = IconFolderTint,
                modifier = Modifier.size(24.w.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.w.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                fontSize = 16.ws.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.w.dp))

            Text(
                text = formatTimestamp(folder.updatedAt),
                fontSize = 12.ws.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}

// ── 底部操作按钮（新建文件夹 / 保存到此处） ──

@Composable
private fun MovePickerButton(
    text: String,
    textColor: Color,
    background: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(48.w.dp)
            .clip(RoundedCornerShape(12.w.dp))
            .background(background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.ws.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
