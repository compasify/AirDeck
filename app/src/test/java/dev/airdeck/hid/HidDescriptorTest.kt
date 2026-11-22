package dev.airdeck.hid

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HidDescriptorTest {

    @Test
    fun descriptorHasBalancedCollections() {
        val bytes = HidDescriptor.DESCRIPTOR
        var depth = 0
        var i = 0
        while (i < bytes.size) {
            val header = bytes[i].toInt() and 0xFF
            val sizeCode = header and 0x03
            val dataLen = if (sizeCode == 3) 4 else sizeCode
            val tag = (header ushr 2) and 0x3F
            // Main items: Collection = 0xA0 (tag 0x28), End Collection = 0xC0 (tag 0x30)
            if (header and 0xFC == 0xA0) depth++
            if (header and 0xFC == 0xC0) depth--
            assertTrue("Collection depth went negative at index $i", depth >= 0)
            i += 1 + dataLen
        }
        assertEquals("Unclosed HID collections", 0, depth)
        assertEquals("Descriptor byte traversal should land exactly at end", bytes.size, i)
    }

    @Test
    fun keyboardReportEncodesModifiersAndSixKeyCap() {
        val report = HidDescriptor.keyboardReport(
            modifierMask = HidDescriptor.MOD_LEFT_CTRL or HidDescriptor.MOD_LEFT_SHIFT,
            keys = listOf(
                HidKeyMap.KEY_A,
                HidKeyMap.KEY_B,
                HidKeyMap.KEY_C,
                HidKeyMap.KEY_D,
                HidKeyMap.KEY_E,
                HidKeyMap.KEY_F,
                HidKeyMap.KEY_G, // 7th key should be truncated
            ),
        )
        assertEquals(8, report.size)
        assertEquals((HidDescriptor.MOD_LEFT_CTRL or HidDescriptor.MOD_LEFT_SHIFT).toByte(), report[0])
        assertEquals(0.toByte(), report[1])
        assertEquals(HidKeyMap.KEY_A.toByte(), report[2])
        assertEquals(HidKeyMap.KEY_F.toByte(), report[7])
    }

    @Test
    fun mouseReportClampsAndEncodesButtons() {
        val report = HidDescriptor.mouseReport(
            buttons = HidDescriptor.MOUSE_BUTTON_LEFT or HidDescriptor.MOUSE_BUTTON_RIGHT,
            dx = 200,
            dy = -250,
            wheel = 5,
        )
        assertArrayEquals(
            byteArrayOf(0x03, 127, -127, 5),
            report,
        )
    }

    @Test
    fun consumerReportEncodesLittleEndian16Bit() {
        val home = HidDescriptor.consumerReport(HidDescriptor.CONSUMER_AC_HOME) // 0x0223
        assertArrayEquals(byteArrayOf(0x23, 0x02), home)

        val back = HidDescriptor.consumerReport(HidDescriptor.CONSUMER_AC_BACK) // 0x0224
        assertArrayEquals(byteArrayOf(0x24, 0x02), back)

        val volUp = HidDescriptor.consumerReport(HidDescriptor.CONSUMER_VOLUME_UP) // 0x00E9
        assertArrayEquals(byteArrayOf(0xE9.toByte(), 0x00), volUp)
    }

    @Test
    fun asciiMappingCoversLettersDigitsAndSymbols() {
        val lower = HidKeyMap.fromAsciiChar('a')
        assertNotNull(lower)
        assertEquals(HidKeyMap.KEY_A, lower!!.usageId)
        assertFalse(lower.shift)

        val upper = HidKeyMap.fromAsciiChar('Z')
        assertNotNull(upper)
        assertEquals(HidKeyMap.KEY_Z, upper!!.usageId)
        assertTrue(upper.shift)

        val atSign = HidKeyMap.fromAsciiChar('@')
        assertNotNull(atSign)
        assertEquals(HidKeyMap.KEY_2, atSign!!.usageId)
        assertTrue(atSign.shift)

        val enter = HidKeyMap.fromAsciiChar('\n')
        assertNotNull(enter)
        assertEquals(HidKeyMap.KEY_ENTER, enter!!.usageId)
        assertFalse(enter.shift)
    }
}
