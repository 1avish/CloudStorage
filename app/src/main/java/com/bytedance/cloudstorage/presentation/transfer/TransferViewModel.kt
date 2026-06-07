package com.bytedance.cloudstorage.presentation.transfer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bytedance.cloudstorage.data.local.database.AppDatabase
import com.bytedance.cloudstorage.data.local.entity.TransferRecordEntity
import com.bytedance.cloudstorage.domain.model.FileType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// ────────────────────────────────────────────────
// 传输页面 ViewModel 及数据模型
// ────────────────────────────────────────────────

/** 传输方向筛选项 */
enum class TransferDirectionFilter(val value: String) {
    Upload("upload"),
    Download("download"),
}

/** 传输状态筛选项 */
enum class TransferStatusFilter(val value: String?) {
    All(null),
    Completed("completed"),
    Transferring("transferring"),
    Failed("failed"),
}

/**
 * 传输记录 UI 模型
 *
 * 由 TransferRecordEntity 转换而来，供 Compose UI 直接使用。
 */
data class TransferRecordUi(
    val id: String,
    val fileId: String?,
    val name: String,
    val size: Long,
    val type: FileType,
    val direction: String,
    val source: String,
    val status: String,
    val savedPath: String,
    val createdAt: Long,
)

/** 传输列表 UI 状态，按方向（上传/下载）分别持有 */
data class TransferUiState(
    val records: List<TransferRecordUi> = emptyList(),
    val allCount: Int = 0,
    val completedCount: Int = 0,
    val transferringCount: Int = 0,
    val failedCount: Int = 0,
)

/**
 * 传输列表 ViewModel
 *
 * 从 Room 数据库观察上传和下载的传输记录，
 * 转换为 UI 模型后通过 StateFlow 推送给 TransferScreen。
 */
class TransferViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).transferRecordDao()

    val uploadState: StateFlow<TransferUiState> =
        dao.observeRecords(TransferDirectionFilter.Upload.value)
            .map(::toUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransferUiState())

    val downloadState: StateFlow<TransferUiState> =
        dao.observeRecords(TransferDirectionFilter.Download.value)
            .map(::toUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransferUiState())

    private fun toUiState(records: List<TransferRecordEntity>): TransferUiState {
        val uiRecords = records.map { entity ->
            TransferRecordUi(
                id = entity.recordId,
                fileId = entity.fileId,
                name = entity.name,
                size = entity.size,
                type = FileType.fromString(entity.type),
                direction = entity.direction,
                source = entity.source,
                status = entity.status,
                savedPath = entity.savedPath,
                createdAt = entity.createdAt,
            )
        }
        return TransferUiState(
            records = uiRecords,
            allCount = uiRecords.size,
            completedCount = uiRecords.count { it.status == TransferStatusFilter.Completed.value },
            transferringCount = uiRecords.count { it.status == TransferStatusFilter.Transferring.value },
            failedCount = uiRecords.count { it.status == TransferStatusFilter.Failed.value },
        )
    }
}
