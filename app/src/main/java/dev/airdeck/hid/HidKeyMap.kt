package dev.airdeck.hid

import android.view.KeyEvent

data class HidKeyStroke(
    val usageId: Int,
    val shift: Boolean = false,
)

/**
 * Maps Android KeyEvent keycodes and printable ASCII characters to USB HID Usage Page 0x07 IDs.
 */
object HidKeyMap {
    const val KEY_NONE = 0x00
    const val KEY_A = 0x04
    const val KEY_B = 0x05
    const val KEY_C = 0x06
    const val KEY_D = 0x07
    const val KEY_E = 0x08
    const val KEY_F = 0x09
    const val KEY_G = 0x0A
    const val KEY_H = 0x0B
    const val KEY_I = 0x0C
    const val KEY_J = 0x0D
    const val KEY_K = 0x0E
    const val KEY_L = 0x0F
    const val KEY_M = 0x10
    const val KEY_N = 0x11
    const val KEY_O = 0x12
    const val KEY_P = 0x13
    const val KEY_Q = 0x14
    const val KEY_R = 0x15
    const val KEY_S = 0x16
    const val KEY_T = 0x17
    const val KEY_U = 0x18
    const val KEY_V = 0x19
    const val KEY_W = 0x1A
    const val KEY_X = 0x1B
    const val KEY_Y = 0x1C
    const val KEY_Z = 0x1D

    const val KEY_1 = 0x1E
    const val KEY_2 = 0x1F
    const val KEY_3 = 0x20
    const val KEY_4 = 0x21
    const val KEY_5 = 0x22
    const val KEY_6 = 0x23
    const val KEY_7 = 0x24
    const val KEY_8 = 0x25
    const val KEY_9 = 0x26
    const val KEY_0 = 0x27

    const val KEY_ENTER = 0x28
    const val KEY_ESC = 0x29
    const val KEY_BACKSPACE = 0x2A
    const val KEY_TAB = 0x2B
    const val KEY_SPACE = 0x2C
    const val KEY_MINUS = 0x2D
    const val KEY_EQUALS = 0x2E
    const val KEY_LEFT_BRACKET = 0x2F
    const val KEY_RIGHT_BRACKET = 0x30
    const val KEY_BACKSLASH = 0x31
    const val KEY_SEMICOLON = 0x33
    const val KEY_APOSTROPHE = 0x34
    const val KEY_GRAVE = 0x35
    const val KEY_COMMA = 0x36
    const val KEY_PERIOD = 0x37
    const val KEY_SLASH = 0x38
    const val KEY_CAPS_LOCK = 0x39

    const val KEY_F1 = 0x3A
    const val KEY_F2 = 0x3B
    const val KEY_F3 = 0x3C
    const val KEY_F4 = 0x3D
    const val KEY_F5 = 0x3E
    const val KEY_F6 = 0x3F
    const val KEY_F7 = 0x40
    const val KEY_F8 = 0x41
    const val KEY_F9 = 0x42
    const val KEY_F10 = 0x43
    const val KEY_F11 = 0x44
    const val KEY_F12 = 0x45

    const val KEY_INSERT = 0x49
    const val KEY_HOME = 0x4A
    const val KEY_PAGE_UP = 0x4B
    const val KEY_DELETE = 0x4C
    const val KEY_END = 0x4D
    const val KEY_PAGE_DOWN = 0x4E
    const val KEY_RIGHT = 0x4F
    const val KEY_LEFT = 0x50
    const val KEY_DOWN = 0x51
    const val KEY_UP = 0x52

    fun fromAndroidKeyCode(keyCode: Int): Int? = when (keyCode) {
        KeyEvent.KEYCODE_A -> KEY_A
        KeyEvent.KEYCODE_B -> KEY_B
        KeyEvent.KEYCODE_C -> KEY_C
        KeyEvent.KEYCODE_D -> KEY_D
        KeyEvent.KEYCODE_E -> KEY_E
        KeyEvent.KEYCODE_F -> KEY_F
        KeyEvent.KEYCODE_G -> KEY_G
        KeyEvent.KEYCODE_H -> KEY_H
        KeyEvent.KEYCODE_I -> KEY_I
        KeyEvent.KEYCODE_J -> KEY_J
        KeyEvent.KEYCODE_K -> KEY_K
        KeyEvent.KEYCODE_L -> KEY_L
        KeyEvent.KEYCODE_M -> KEY_M
        KeyEvent.KEYCODE_N -> KEY_N
        KeyEvent.KEYCODE_O -> KEY_O
        KeyEvent.KEYCODE_P -> KEY_P
        KeyEvent.KEYCODE_Q -> KEY_Q
        KeyEvent.KEYCODE_R -> KEY_R
        KeyEvent.KEYCODE_S -> KEY_S
        KeyEvent.KEYCODE_T -> KEY_T
        KeyEvent.KEYCODE_U -> KEY_U
        KeyEvent.KEYCODE_V -> KEY_V
        KeyEvent.KEYCODE_W -> KEY_W
        KeyEvent.KEYCODE_X -> KEY_X
        KeyEvent.KEYCODE_Y -> KEY_Y
        KeyEvent.KEYCODE_Z -> KEY_Z
        KeyEvent.KEYCODE_1 -> KEY_1
        KeyEvent.KEYCODE_2 -> KEY_2
        KeyEvent.KEYCODE_3 -> KEY_3
        KeyEvent.KEYCODE_4 -> KEY_4
        KeyEvent.KEYCODE_5 -> KEY_5
        KeyEvent.KEYCODE_6 -> KEY_6
        KeyEvent.KEYCODE_7 -> KEY_7
        KeyEvent.KEYCODE_8 -> KEY_8
        KeyEvent.KEYCODE_9 -> KEY_9
        KeyEvent.KEYCODE_0 -> KEY_0
        KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> KEY_ENTER
        KeyEvent.KEYCODE_ESCAPE -> KEY_ESC
        KeyEvent.KEYCODE_DEL -> KEY_BACKSPACE
        KeyEvent.KEYCODE_TAB -> KEY_TAB
        KeyEvent.KEYCODE_SPACE -> KEY_SPACE
        KeyEvent.KEYCODE_MINUS -> KEY_MINUS
        KeyEvent.KEYCODE_EQUALS -> KEY_EQUALS
        KeyEvent.KEYCODE_LEFT_BRACKET -> KEY_LEFT_BRACKET
        KeyEvent.KEYCODE_RIGHT_BRACKET -> KEY_RIGHT_BRACKET
        KeyEvent.KEYCODE_BACKSLASH -> KEY_BACKSLASH
        KeyEvent.KEYCODE_SEMICOLON -> KEY_SEMICOLON
        KeyEvent.KEYCODE_APOSTROPHE -> KEY_APOSTROPHE
        KeyEvent.KEYCODE_GRAVE -> KEY_GRAVE
        KeyEvent.KEYCODE_COMMA -> KEY_COMMA
        KeyEvent.KEYCODE_PERIOD -> KEY_PERIOD
        KeyEvent.KEYCODE_SLASH -> KEY_SLASH
        KeyEvent.KEYCODE_CAPS_LOCK -> KEY_CAPS_LOCK
        KeyEvent.KEYCODE_F1 -> KEY_F1
        KeyEvent.KEYCODE_F2 -> KEY_F2
        KeyEvent.KEYCODE_F3 -> KEY_F3
        KeyEvent.KEYCODE_F4 -> KEY_F4
        KeyEvent.KEYCODE_F5 -> KEY_F5
        KeyEvent.KEYCODE_F6 -> KEY_F6
        KeyEvent.KEYCODE_F7 -> KEY_F7
        KeyEvent.KEYCODE_F8 -> KEY_F8
        KeyEvent.KEYCODE_F9 -> KEY_F9
        KeyEvent.KEYCODE_F10 -> KEY_F10
        KeyEvent.KEYCODE_F11 -> KEY_F11
        KeyEvent.KEYCODE_F12 -> KEY_F12
        KeyEvent.KEYCODE_INSERT -> KEY_INSERT
        KeyEvent.KEYCODE_MOVE_HOME -> KEY_HOME
        KeyEvent.KEYCODE_PAGE_UP -> KEY_PAGE_UP
        KeyEvent.KEYCODE_FORWARD_DEL -> KEY_DELETE
        KeyEvent.KEYCODE_MOVE_END -> KEY_END
        KeyEvent.KEYCODE_PAGE_DOWN -> KEY_PAGE_DOWN
        KeyEvent.KEYCODE_DPAD_RIGHT -> KEY_RIGHT
        KeyEvent.KEYCODE_DPAD_LEFT -> KEY_LEFT
        KeyEvent.KEYCODE_DPAD_DOWN -> KEY_DOWN
        KeyEvent.KEYCODE_DPAD_UP -> KEY_UP
        else -> null
    }

    fun fromAsciiChar(ch: Char): HidKeyStroke? = when (ch) {
        in 'a'..'z' -> HidKeyStroke(KEY_A + (ch - 'a'), shift = false)
        in 'A'..'Z' -> HidKeyStroke(KEY_A + (ch - 'A'), shift = true)
        '1' -> HidKeyStroke(KEY_1)
        '!' -> HidKeyStroke(KEY_1, shift = true)
        '2' -> HidKeyStroke(KEY_2)
        '@' -> HidKeyStroke(KEY_2, shift = true)
        '3' -> HidKeyStroke(KEY_3)
        '#' -> HidKeyStroke(KEY_3, shift = true)
        '4' -> HidKeyStroke(KEY_4)
        '$' -> HidKeyStroke(KEY_4, shift = true)
        '5' -> HidKeyStroke(KEY_5)
        '%' -> HidKeyStroke(KEY_5, shift = true)
        '6' -> HidKeyStroke(KEY_6)
        '^' -> HidKeyStroke(KEY_6, shift = true)
        '7' -> HidKeyStroke(KEY_7)
        '&' -> HidKeyStroke(KEY_7, shift = true)
        '8' -> HidKeyStroke(KEY_8)
        '*' -> HidKeyStroke(KEY_8, shift = true)
        '9' -> HidKeyStroke(KEY_9)
        '(' -> HidKeyStroke(KEY_9, shift = true)
        '0' -> HidKeyStroke(KEY_0)
        ')' -> HidKeyStroke(KEY_0, shift = true)
        '\n', '\r' -> HidKeyStroke(KEY_ENTER)
        '\t' -> HidKeyStroke(KEY_TAB)
        ' ' -> HidKeyStroke(KEY_SPACE)
        '-' -> HidKeyStroke(KEY_MINUS)
        '_' -> HidKeyStroke(KEY_MINUS, shift = true)
        '=' -> HidKeyStroke(KEY_EQUALS)
        '+' -> HidKeyStroke(KEY_EQUALS, shift = true)
        '[' -> HidKeyStroke(KEY_LEFT_BRACKET)
        '{' -> HidKeyStroke(KEY_LEFT_BRACKET, shift = true)
        ']' -> HidKeyStroke(KEY_RIGHT_BRACKET)
        '}' -> HidKeyStroke(KEY_RIGHT_BRACKET, shift = true)
        '\\' -> HidKeyStroke(KEY_BACKSLASH)
        '|' -> HidKeyStroke(KEY_BACKSLASH, shift = true)
        ';' -> HidKeyStroke(KEY_SEMICOLON)
        ':' -> HidKeyStroke(KEY_SEMICOLON, shift = true)
        '\'' -> HidKeyStroke(KEY_APOSTROPHE)
        '"' -> HidKeyStroke(KEY_APOSTROPHE, shift = true)
        '`' -> HidKeyStroke(KEY_GRAVE)
        '~' -> HidKeyStroke(KEY_GRAVE, shift = true)
        ',' -> HidKeyStroke(KEY_COMMA)
        '<' -> HidKeyStroke(KEY_COMMA, shift = true)
        '.' -> HidKeyStroke(KEY_PERIOD)
        '>' -> HidKeyStroke(KEY_PERIOD, shift = true)
        '/' -> HidKeyStroke(KEY_SLASH)
        '?' -> HidKeyStroke(KEY_SLASH, shift = true)
        else -> null
    }
}
