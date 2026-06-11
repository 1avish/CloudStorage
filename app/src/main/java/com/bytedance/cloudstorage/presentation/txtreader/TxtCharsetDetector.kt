package com.bytedance.cloudstorage.presentation.txtreader

import android.content.Context
import android.net.Uri
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CoderResult
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

internal const val CHARSET_SNIFF_SIZE = 8 * 1024

internal fun detectCharset(context: Context, fileUri: String): Charset {
    if (fileUri.isBlank()) return StandardCharsets.UTF_8

    val uri = Uri.parse(fileUri)
    val sample = context.contentResolver.openInputStream(uri)?.use { stream ->
        val buf = ByteArray(CHARSET_SNIFF_SIZE)
        val read = stream.read(buf)
        if (read <= 0) return StandardCharsets.UTF_8
        buf.copyOf(read)
    } ?: return StandardCharsets.UTF_8

    if (sample.size >= 3 &&
        sample[0] == 0xEF.toByte() &&
        sample[1] == 0xBB.toByte() &&
        sample[2] == 0xBF.toByte()
    ) return StandardCharsets.UTF_8

    if (sample.size >= 2) {
        if (sample[0] == 0xFF.toByte() && sample[1] == 0xFE.toByte()) {
            return StandardCharsets.UTF_16LE
        }
        if (sample[0] == 0xFE.toByte() && sample[1] == 0xFF.toByte()) {
            return StandardCharsets.UTF_16BE
        }
    }

    if (isValidUtf8Sample(sample)) return StandardCharsets.UTF_8

    return Charset.forName("GB18030")
}

internal fun isValidUtf8Sample(sample: ByteArray): Boolean {
    val decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
    val input = ByteBuffer.wrap(sample)
    val result = decoder.decode(input, CharBuffer.allocate(sample.size), false)

    if (result.isUnderflow) return true
    if (result != CoderResult.UNDERFLOW) {
        return isTruncatedUtf8Suffix(sample, input.position())
    }
    return false
}

private fun isTruncatedUtf8Suffix(sample: ByteArray, index: Int): Boolean {
    if (index !in sample.indices) return false

    val firstByte = sample[index].toInt() and 0xFF
    val expectedLength = when (firstByte) {
        in 0xC2..0xDF -> 2
        in 0xE0..0xEF -> 3
        in 0xF0..0xF4 -> 4
        else -> return false
    }
    val remaining = sample.size - index
    if (remaining >= expectedLength) return false

    for (offset in 1 until remaining) {
        if ((sample[index + offset].toInt() and 0xC0) != 0x80) return false
    }
    return true
}
