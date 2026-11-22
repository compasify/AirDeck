package dev.airdeck.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class DeckLayoutPreference(val key: String) {
    Auto("auto"),
    Touchpad("touchpad"),
    Split("split"),
    Keyboard("keyboard"),
    TvRemote("tv_remote"),
    AirMouse("air_mouse"),
    Numpad("numpad"),
    Presenter("presenter");

    companion object {
        fun fromKey(raw: String?): DeckLayoutPreference =
            entries.firstOrNull { it.key == raw } ?: Auto
    }
}

data class AirDeckSettings(
    val pointerSpeed: Float = 1.2f,
    val scrollSpeed: Float = 1.0f,
    val airMouseSensitivity: Float = 1.4f,
    val naturalScroll: Boolean = true,
    val touchpadOnTop: Boolean = false, // false = Laptop style (Keyboard top, Touchpad bottom)
    val hapticsEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val showMouseButtons: Boolean = true,
    val showTvBar: Boolean = true,
    val layoutPreference: DeckLayoutPreference = DeckLayoutPreference.Auto,
    val lastHostAddress: String? = null,
)

private val Context.airDeckDataStore by preferencesDataStore(name = "airdeck_prefs")

class AirDeckPrefs(private val context: Context) {

    private object Keys {
        val pointerSpeed = floatPreferencesKey("pointer_speed")
        val scrollSpeed = floatPreferencesKey("scroll_speed")
        val airMouseSensitivity = floatPreferencesKey("air_mouse_sensitivity")
        val naturalScroll = booleanPreferencesKey("natural_scroll")
        val touchpadOnTop = booleanPreferencesKey("touchpad_on_top")
        val hapticsEnabled = booleanPreferencesKey("haptics_enabled")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val showMouseButtons = booleanPreferencesKey("show_mouse_buttons")
        val showTvBar = booleanPreferencesKey("show_tv_bar")
        val layoutPreference = stringPreferencesKey("layout_preference")
        val lastHostAddress = stringPreferencesKey("last_host_address")
    }

    val settings: Flow<AirDeckSettings> = context.airDeckDataStore.data.map { prefs: Preferences ->
        AirDeckSettings(
            pointerSpeed = (prefs[Keys.pointerSpeed] ?: 1.2f).coerceIn(0.4f, 3.0f),
            scrollSpeed = (prefs[Keys.scrollSpeed] ?: 1.0f).coerceIn(0.4f, 3.0f),
            airMouseSensitivity = (prefs[Keys.airMouseSensitivity] ?: 1.4f).coerceIn(0.4f, 3.0f),
            naturalScroll = prefs[Keys.naturalScroll] ?: true,
            touchpadOnTop = prefs[Keys.touchpadOnTop] ?: false,
            hapticsEnabled = prefs[Keys.hapticsEnabled] ?: true,
            keepScreenOn = prefs[Keys.keepScreenOn] ?: true,
            showMouseButtons = prefs[Keys.showMouseButtons] ?: true,
            showTvBar = prefs[Keys.showTvBar] ?: true,
            layoutPreference = DeckLayoutPreference.fromKey(prefs[Keys.layoutPreference]),
            lastHostAddress = prefs[Keys.lastHostAddress],
        )
    }

    suspend fun setPointerSpeed(value: Float) {
        context.airDeckDataStore.edit { it[Keys.pointerSpeed] = value.coerceIn(0.4f, 3.0f) }
    }

    suspend fun setScrollSpeed(value: Float) {
        context.airDeckDataStore.edit { it[Keys.scrollSpeed] = value.coerceIn(0.4f, 3.0f) }
    }

    suspend fun setAirMouseSensitivity(value: Float) {
        context.airDeckDataStore.edit { it[Keys.airMouseSensitivity] = value.coerceIn(0.4f, 3.0f) }
    }

    suspend fun setNaturalScroll(value: Boolean) {
        context.airDeckDataStore.edit { it[Keys.naturalScroll] = value }
    }

    suspend fun setTouchpadOnTop(value: Boolean) {
        context.airDeckDataStore.edit { it[Keys.touchpadOnTop] = value }
    }

    suspend fun setHapticsEnabled(value: Boolean) {
        context.airDeckDataStore.edit { it[Keys.hapticsEnabled] = value }
    }

    suspend fun setKeepScreenOn(value: Boolean) {
        context.airDeckDataStore.edit { it[Keys.keepScreenOn] = value }
    }

    suspend fun setShowMouseButtons(value: Boolean) {
        context.airDeckDataStore.edit { it[Keys.showMouseButtons] = value }
    }

    suspend fun setShowTvBar(value: Boolean) {
        context.airDeckDataStore.edit { it[Keys.showTvBar] = value }
    }

    suspend fun setLayoutPreference(pref: DeckLayoutPreference) {
        context.airDeckDataStore.edit { it[Keys.layoutPreference] = pref.key }
    }

    suspend fun setLastHostAddress(address: String) {
        context.airDeckDataStore.edit { it[Keys.lastHostAddress] = address }
    }
}
