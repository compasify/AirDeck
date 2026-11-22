package dev.airdeck.hid

/**
 * Composite USB HID Report Descriptor for AirDeck:
 *  - Report ID 1: Standard 6-key rollover Keyboard (8 bytes)
 *  - Report ID 2: Standard Relative Mouse with 3 buttons, X, Y, and vertical wheel (4 bytes)
 *  - Report ID 3: Consumer Control for Smart TV / Media keys (2 bytes, 16-bit usage)
 */
object HidDescriptor {
    const val REPORT_ID_KEYBOARD = 1
    const val REPORT_ID_MOUSE = 2
    const val REPORT_ID_CONSUMER = 3

    const val MOUSE_BUTTON_LEFT = 0x01
    const val MOUSE_BUTTON_RIGHT = 0x02
    const val MOUSE_BUTTON_MIDDLE = 0x04

    // Modifier bitmask for Keyboard Report byte 0
    const val MOD_LEFT_CTRL = 0x01
    const val MOD_LEFT_SHIFT = 0x02
    const val MOD_LEFT_ALT = 0x04
    const val MOD_LEFT_META = 0x08
    const val MOD_RIGHT_CTRL = 0x10
    const val MOD_RIGHT_SHIFT = 0x20
    const val MOD_RIGHT_ALT = 0x40
    const val MOD_RIGHT_META = 0x80

    // Consumer Control 16-bit usages (Usage Page 0x0C)
    const val CONSUMER_POWER = 0x0030
    const val CONSUMER_MENU = 0x0040
    const val CONSUMER_SCAN_NEXT = 0x00B5
    const val CONSUMER_SCAN_PREV = 0x00B6
    const val CONSUMER_PLAY_PAUSE = 0x00CD
    const val CONSUMER_MUTE = 0x00E2
    const val CONSUMER_VOLUME_UP = 0x00E9
    const val CONSUMER_VOLUME_DOWN = 0x00EA
    const val CONSUMER_AC_SEARCH = 0x0221
    const val CONSUMER_AC_HOME = 0x0223
    const val CONSUMER_AC_BACK = 0x0224

    val DESCRIPTOR: ByteArray = byteArrayOf(
        // ---- Report ID 1: Keyboard (8 bytes) ----
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x06.toByte(), // Usage (Keyboard)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), REPORT_ID_KEYBOARD.toByte(),
        0x05.toByte(), 0x07.toByte(), // Usage Page (Keyboard/Keypad)
        0x19.toByte(), 0xE0.toByte(), // Usage Minimum (Left Control)
        0x29.toByte(), 0xE7.toByte(), // Usage Maximum (Right GUI)
        0x15.toByte(), 0x00.toByte(), // Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), // Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(), // Report Size (1)
        0x95.toByte(), 0x08.toByte(), // Report Count (8)
        0x81.toByte(), 0x02.toByte(), // Input (Data, Variable, Absolute) - Modifier byte
        0x95.toByte(), 0x01.toByte(), // Report Count (1)
        0x75.toByte(), 0x08.toByte(), // Report Size (8)
        0x81.toByte(), 0x01.toByte(), // Input (Constant) - Reserved byte
        0x95.toByte(), 0x06.toByte(), // Report Count (6)
        0x75.toByte(), 0x08.toByte(), // Report Size (8)
        0x15.toByte(), 0x00.toByte(), // Logical Minimum (0)
        0x25.toByte(), 0x65.toByte(), // Logical Maximum (101)
        0x05.toByte(), 0x07.toByte(), // Usage Page (Keyboard/Keypad)
        0x19.toByte(), 0x00.toByte(), // Usage Minimum (0)
        0x29.toByte(), 0x65.toByte(), // Usage Maximum (101)
        0x81.toByte(), 0x00.toByte(), // Input (Data, Array) - 6 keycodes
        0xC0.toByte(),                // End Collection

        // ---- Report ID 2: Standard Relative Mouse (4 bytes) ----
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x02.toByte(), // Usage (Mouse)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), REPORT_ID_MOUSE.toByte(),
        0x09.toByte(), 0x01.toByte(), // Usage (Pointer)
        0xA1.toByte(), 0x00.toByte(), // Collection (Physical)
        0x05.toByte(), 0x09.toByte(), // Usage Page (Button)
        0x19.toByte(), 0x01.toByte(), // Usage Minimum (Button 1)
        0x29.toByte(), 0x03.toByte(), // Usage Maximum (Button 3)
        0x15.toByte(), 0x00.toByte(), // Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), // Logical Maximum (1)
        0x95.toByte(), 0x03.toByte(), // Report Count (3)
        0x75.toByte(), 0x01.toByte(), // Report Size (1)
        0x81.toByte(), 0x02.toByte(), // Input (Data, Variable, Absolute)
        0x95.toByte(), 0x01.toByte(), // Report Count (1)
        0x75.toByte(), 0x05.toByte(), // Report Size (5) - Padding
        0x81.toByte(), 0x03.toByte(), // Input (Constant, Variable, Absolute)
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x30.toByte(), // Usage (X)
        0x09.toByte(), 0x31.toByte(), // Usage (Y)
        0x09.toByte(), 0x38.toByte(), // Usage (Wheel)
        0x15.toByte(), 0x81.toByte(), // Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(), // Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(), // Report Size (8)
        0x95.toByte(), 0x03.toByte(), // Report Count (3)
        0x81.toByte(), 0x06.toByte(), // Input (Data, Variable, Relative)
        0xC0.toByte(),                // End Collection (Physical)
        0xC0.toByte(),                // End Collection (Application)

        // ---- Report ID 3: Consumer Control (2 bytes) ----
        0x05.toByte(), 0x0C.toByte(), // Usage Page (Consumer)
        0x09.toByte(), 0x01.toByte(), // Usage (Consumer Control)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), REPORT_ID_CONSUMER.toByte(),
        0x19.toByte(), 0x00.toByte(), // Usage Minimum (0x0000)
        0x2A.toByte(), 0xFF.toByte(), 0x03.toByte(), // Usage Maximum (0x03FF)
        0x15.toByte(), 0x00.toByte(), // Logical Minimum (0)
        0x26.toByte(), 0xFF.toByte(), 0x03.toByte(), // Logical Maximum (0x03FF)
        0x95.toByte(), 0x01.toByte(), // Report Count (1)
        0x75.toByte(), 0x10.toByte(), // Report Size (16)
        0x81.toByte(), 0x00.toByte(), // Input (Data, Array, Absolute)
        0xC0.toByte(),                // End Collection
    )

    fun keyboardReport(modifierMask: Int, keys: List<Int>): ByteArray {
        val report = ByteArray(8)
        report[0] = (modifierMask and 0xFF).toByte()
        report[1] = 0
        val count = minOf(keys.size, 6)
        for (i in 0 until count) {
            report[2 + i] = (keys[i] and 0xFF).toByte()
        }
        return report
    }

    fun mouseReport(buttons: Int, dx: Int, dy: Int, wheel: Int = 0): ByteArray =
        byteArrayOf(
            (buttons and 0x07).toByte(),
            dx.coerceIn(-127, 127).toByte(),
            dy.coerceIn(-127, 127).toByte(),
            wheel.coerceIn(-127, 127).toByte(),
        )

    fun consumerReport(usageCode: Int): ByteArray =
        byteArrayOf(
            (usageCode and 0xFF).toByte(),
            ((usageCode shr 8) and 0xFF).toByte(),
        )
}
