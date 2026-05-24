package com.bytedance.cloudstorage.presentation.filelist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 文件 Tab 占位页
 *
 * MVP 阶段展示占位文字，后续实现：
 * - 文件列表展示（文件夹、视频、TXT）
 * - 文件夹进入与返回
 * - 文件操作菜单（上传、删除、重命名、移动）
 */
@Composable
fun FileListScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "文件",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
