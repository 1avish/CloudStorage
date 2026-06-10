package com.bytedance.cloudstorage.presentation.txtreader

import android.content.Context
import android.net.Uri
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

// ── 编码检测 ──
// 读取文件前 8KB，按 BOM 标记 → UTF-8 合法性检查 → 回退 GB18030 的顺序判断

internal const val CHARSET_SNIFF_SIZE = 8 * 1024

/**
 * 嗅探文本文件编码。
 *
 * 优先级：
 * 1. BOM 标记（UTF-8 / UTF-16LE / UTF-16BE）
 * 2. 样本是否为合法 UTF-8 多字节序列
 * 3. 回退 GB18030（兼容 GBK、GB2312）
 */
internal fun detectCharset(context: Context, fileUri: String): Charset {
    if (fileUri.isBlank()) return StandardCharsets.UTF_8

    val uri = Uri.parse(fileUri)
    val sample = context.contentResolver.openInputStream(uri)?.use { stream ->
        val buf = ByteArray(CHARSET_SNIFF_SIZE)
        val read = stream.read(buf)
        if (read <= 0) return StandardCharsets.UTF_8
        buf.copyOf(read)
    } ?: return StandardCharsets.UTF_8

    // BOM 检测
    if (sample.size >= 3 &&
        sample[0] == 0xEF.toByte() &&
        sample[1] == 0xBB.toByte() &&
        sample[2] == 0xBF.toByte()
    ) return StandardCharsets.UTF_8

    if (sample.size >= 2) {
        if (sample[0] == 0xFF.toByte() && sample[1] == 0xFE.toByte())
            return StandardCharsets.UTF_16LE
        if (sample[0] == 0xFE.toByte() && sample[1] == 0xFF.toByte())
            return StandardCharsets.UTF_16BE
    }

    if (isValidUtf8Sample(sample)) return StandardCharsets.UTF_8

    return Charset.forName("GB18030")
}

/**
 * 简易 UTF-8 合法性检查。
 *
 * 遍历样本字节，统计合法的多字节序列数量。
 * 若存在至少一个合法的 2/3/4 字节序列，且无非法起始字节，判定为 UTF-8。
 */
internal fun isValidUtf8Sample(sample: ByteArray): Boolean {
    var multiByteSequences = 0
    var i = 0
    while (i < sample.size) {
        val b = sample[i].toInt() and 0xFF
        when {
            b <= 0x7F -> i++ // ASCII
            b in 0xC2..0xDF && i + 1 < sample.size &&
                (sample[i + 1].toInt() and 0xC0) == 0x80 -> {
                multiByteSequences++; i += 2
            }
            b in 0xE0..0xEF && i + 2 < sample.size &&
                (sample[i + 1].toInt() and 0xC0) == 0x80 &&
                (sample[i + 2].toInt() and 0xC0) == 0x80 -> {
                multiByteSequences++; i += 3
            }
            b in 0xF0..0xF4 && i + 3 < sample.size &&
                (sample[i + 1].toInt() and 0xC0) == 0x80 &&
                (sample[i + 2].toInt() and 0xC0) == 0x80 &&
                (sample[i + 3].toInt() and 0xC0) == 0x80 -> {
                multiByteSequences++; i += 4
            }
            else -> return false // 非法 UTF-8 字节
        }
    }
    return multiByteSequences > 0 || sample.all { (it.toInt() and 0xFF) <= 0x7F }
}
