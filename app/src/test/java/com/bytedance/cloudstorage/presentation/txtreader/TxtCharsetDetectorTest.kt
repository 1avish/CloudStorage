package com.bytedance.cloudstorage.presentation.txtreader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TxtCharsetDetectorTest {

    @Test
    fun validUtf8Sample_allowsTruncatedMultibyteSuffix() {
        val completeChar = byteArrayOf(0xE6.toByte(), 0xB5.toByte(), 0x8B.toByte())
        val truncatedChar = byteArrayOf(0xE6.toByte(), 0xAE.toByte())
        val sample = ByteArray(CHARSET_SNIFF_SIZE)

        var offset = 0
        repeat((CHARSET_SNIFF_SIZE - truncatedChar.size) / completeChar.size) {
            completeChar.copyInto(sample, offset)
            offset += completeChar.size
        }
        truncatedChar.copyInto(sample, offset)

        assertTrue(isValidUtf8Sample(sample))
    }

    @Test
    fun validUtf8Sample_rejectsGbkBytes() {
        val gbkChineseBytes = byteArrayOf(0xC4.toByte(), 0xE3.toByte())

        assertFalse(isValidUtf8Sample(gbkChineseBytes))
    }

    @Test
    fun validUtf8Sample_acceptsAscii() {
        assertTrue(isValidUtf8Sample("plain ascii".encodeToByteArray()))
    }
}
