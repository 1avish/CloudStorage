package com.bytedance.cloudstorage.presentation.txtreader

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import com.bytedance.cloudstorage.data.local.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

// ── 阅读器 UI 状态 ──

/**
 * 阅读器页面的完整 UI 状态。
 *
 * 集中管理分页内容、设置项和菜单状态，
 * 通过 [TxtReaderViewModel] 暴露为 [StateFlow] 供 Composable 订阅。
 */
internal data class ReaderUiState(
    val pages: List<String> = emptyList(),
    val isPaginating: Boolean = true,
    val errorMessage: String? = null,
    val isReaderMenuVisible: Boolean = false,
    val activeSettingPanel: ReaderSettingPanel? = null,
    val brightness: Float = 0.65f,
    val useSystemBrightness: Boolean = true,
    val backgroundIndex: Int = 0,
    val fontSizeIndex: Int = 2,
    val lineSpacingIndex: Int = 2,
) {
    val readerSurface get() = ReaderBackgroundOptions[backgroundIndex]
    val isDarkReader get() = readerSurface == Color.Black
    val readerText get() = if (isDarkReader) Color(0xFFEDEFF3) else ReaderText
    val readerSubText get() = if (isDarkReader) Color(0xFF9CA3AF) else ReaderSubText
    val currentFontSize get() = ReaderFontSizes[fontSizeIndex]
    val currentLineHeight get() = readerLineHeight(currentFontSize, lineSpacingIndex)
}

/**
 * TXT 阅读器 ViewModel。
 *
 * 管理阅读器的设置状态（亮度、背景、字号、行间距）和分页进度，
 * 分页执行逻辑由 Composable 层的 LaunchedEffect 驱动（需要 TextMeasurer），
 * 本类只负责状态的读写和文件打开标记。
 */
class TxtReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ReaderUiState())
    internal val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    // ── 标记文件已打开 ──

    /** 更新数据库中的 lastOpenedAt 时间戳 */
    suspend fun markFileOpened(fileId: String) {
        val db = AppDatabase.getInstance(getApplication())
        withContext(Dispatchers.IO) {
            db.fileDao().updateLastOpenedAt(fileId, System.currentTimeMillis())
        }
    }

    // ── 分页状态管理（由 Composable 的 LaunchedEffect 调用）──

    /** 重置分页状态，开始新一轮分页 */
    fun startPagination() {
        _uiState.value = _uiState.value.copy(
            isPaginating = true,
            errorMessage = null,
            pages = emptyList()
        )
    }

    /** 追加一页内容 */
    fun addPage(pageText: String) {
        val currentPages = _uiState.value.pages.toMutableList()
        currentPages.add(pageText)
        _uiState.value = _uiState.value.copy(pages = currentPages)
    }

    /** 记录分页错误 */
    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    /** 分页结束，兜底空页并标记完成 */
    fun finishPagination() {
        val state = _uiState.value
        val pages = if (state.pages.isEmpty() && state.errorMessage == null) listOf(" ") else state.pages
        _uiState.value = state.copy(pages = pages, isPaginating = false)
    }

    // ── 分页设置变更 ──

    fun onFontSizeChanged(newIndex: Int) {
        _uiState.value = _uiState.value.copy(fontSizeIndex = newIndex)
    }

    fun onLineSpacingChanged(newIndex: Int) {
        _uiState.value = _uiState.value.copy(lineSpacingIndex = newIndex)
    }

    // ── 设置项（不需要重新分页）──

    fun setBrightness(value: Float) {
        _uiState.value = _uiState.value.copy(brightness = value, useSystemBrightness = false)
    }

    fun setUseSystemBrightness(value: Boolean) {
        _uiState.value = _uiState.value.copy(useSystemBrightness = value)
    }

    fun setBackgroundIndex(index: Int) {
        _uiState.value = _uiState.value.copy(backgroundIndex = index)
    }

    fun setReaderMenuVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(isReaderMenuVisible = visible)
    }

    fun setActiveSettingPanel(panel: ReaderSettingPanel?) {
        _uiState.value = _uiState.value.copy(activeSettingPanel = panel)
    }
}
