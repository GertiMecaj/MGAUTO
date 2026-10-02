package com.mgafk.app.data.repository

import com.mgafk.app.desktop.DesktopContext as Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.mgafk.app.data.model.AlertConfig
import com.mgafk.app.data.model.AppSettings
import com.mgafk.app.data.model.migrated
import com.mgafk.app.data.model.PetTeam
import com.mgafk.app.data.model.Session
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import com.mgafk.app.data.AppJson

private val settingsFile = java.io.File(
    com.mgafk.app.desktop.DesktopContext.instance.filesDir,
    "settings.preferences_pb",
)
private val desktopStore = PreferenceDataStoreFactory.create(
    // Preferences are only configuration. If the protobuf is corrupt, recreate it cleanly;
    // do not retain a copy of the damaged file.
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    produceFile = { settingsFile },
)
private val Context.dataStore: DataStore<Preferences> get() = desktopStore

class SessionRepository(private val context: Context) {
    private val json = AppJson.storage

    companion object {
        private val KEY_SESSIONS = stringPreferencesKey("mgafk.sessions")

        /**
         * Only user/session configuration belongs on disk. Everything else is authoritative
         * live server state and is rehydrated on reconnect. Persisting that state on every
         * WebSocket patch caused unnecessary protobuf rewrites and could build a large queue of
         * DataStore edits during busy automation.
         */
        internal fun persistedSession(session: Session): Session = session.copy(
            // Credentials and connection intent are runtime-only. A restart requires login again.
            cookie = "",
            wantConnected = false,
            connected = false,
            busy = false,
            status = com.mgafk.app.data.model.SessionStatus.IDLE,
            error = "",
            reconnectCountdown = "",
            players = 0,
            connectedAt = 0,
            playerId = "",
            playerName = "",
            roomId = "",
            weather = "",
            pets = emptyList(),
            logs = emptyList(),
            shops = emptyList(),
            garden = emptyList(),
            gardenEggs = emptyList(),
            inventory = com.mgafk.app.data.model.InventorySnapshot(),
            seedSilo = emptyList(),
            decorShed = emptyList(),
            petHutch = emptyList(),
            feedingTrough = emptyList(),
            toolShack = emptyList(),
            storedEggs = emptyList(),
            chatMessages = emptyList(),
            playersList = emptyList(),
            gameVersion = "",
            freePlantTiles = 0,
            crystals = emptyList(),
            occupiedTiles = emptySet(),
            crystalsReadAtMs = 0L,
            favoritedItemIds = emptySet(),
            lastHatchedPet = null,
            lastHatchedEggId = "",
            wsLogs = emptyList(),
            magicDust = 0.0,
            hutchCapacitySlots = com.mgafk.app.data.repository.PriceCalculator.HUTCH_BASE_CAPACITY,
            siloCapacitySlots = com.mgafk.app.data.repository.PriceCalculator.SILO_BASE_CAPACITY,
            decorShedCapacitySlots = com.mgafk.app.data.repository.PriceCalculator.DECOR_SHED_BASE_CAPACITY,
            toolShackCapacitySlots = com.mgafk.app.data.repository.PriceCalculator.TOOL_SHACK_BASE_CAPACITY,
            availableStorages = emptySet(),
            hostPlayerId = "",
            bots = emptyList(),
            petTeams = emptyList(),
        )

        internal fun persistedSessions(sessions: List<Session>): List<Session> =
            sessions.map(::persistedSession)

        internal fun persistedAlerts(config: AlertConfig): AlertConfig =
            config.copy(collapsed = emptyMap())
        private val KEY_ACTIVE = stringPreferencesKey("mgafk.activeSession")
        private val KEY_ALERTS = stringPreferencesKey("mgafk.alerts")
        private val KEY_SHOP_TIP = booleanPreferencesKey("mgafk.shopTipDismissed")
        private val KEY_TROUGH_TIP = booleanPreferencesKey("mgafk.troughTipDismissed")
        private val KEY_PET_TIP = booleanPreferencesKey("mgafk.petTipDismissed")
        private val KEY_COLLAPSED_CARDS = stringPreferencesKey("mgafk.collapsedCards")
        private val KEY_SETTINGS = stringPreferencesKey("mgafk.settings")
        private val KEY_TEAM_TIP = booleanPreferencesKey("mgafk.teamTipDismissed")
        private val KEY_GARDEN_TIP = booleanPreferencesKey("mgafk.gardenTipDismissed")
        private val KEY_SEED_TIP = booleanPreferencesKey("mgafk.seedTipDismissed")
        private val KEY_EGG_TIP = booleanPreferencesKey("mgafk.eggTipDismissed")
        private val KEY_PLANT_TIP = booleanPreferencesKey("mgafk.plantTipDismissed")
        private val KEY_STORAGE_TIP = booleanPreferencesKey("mgafk.storageTipDismissed")
        private val KEY_NOTIFIED_VERSION = stringPreferencesKey("mgafk.lastNotifiedVersion")
    }

    suspend fun loadSessions(): List<Session> {
        val raw = context.dataStore.data.map { it[KEY_SESSIONS] }.first()
        if (raw.isNullOrBlank()) return listOf(Session())
        return try {
            persistedSessions(json.decodeFromString<List<Session>>(raw))
        } catch (_: Exception) {
            listOf(Session())
        }
    }

    fun persistenceSnapshot(sessions: List<Session>): List<Session> = persistedSessions(sessions)

    suspend fun saveSessions(sessions: List<Session>) {
        val encoded = json.encodeToString(persistedSessions(sessions))
        context.dataStore.edit { prefs ->
            // Avoid rewriting the protobuf when only transient/live game state changed.
            if (prefs[KEY_SESSIONS] != encoded) {
                prefs[KEY_SESSIONS] = encoded
            }
        }
    }

    // Active tab/session is UI runtime state, not a setting.
    suspend fun loadActiveSessionId(): String? = null
    suspend fun saveActiveSessionId(id: String) = Unit

    suspend fun loadAlerts(): AlertConfig {
        val raw = context.dataStore.data.map { it[KEY_ALERTS] }.first()
        if (raw.isNullOrBlank()) return AlertConfig()
        return try {
            persistedAlerts(json.decodeFromString<AlertConfig>(raw))
        } catch (_: Exception) {
            AlertConfig()
        }
    }

    suspend fun saveAlerts(config: AlertConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ALERTS] = json.encodeToString(persistedAlerts(config))
        }
    }

    suspend fun isShopTipDismissed(): Boolean = false
    suspend fun dismissShopTip() = Unit

    suspend fun isTroughTipDismissed(): Boolean = false
    suspend fun dismissTroughTip() = Unit

    suspend fun isPetTipDismissed(): Boolean = false
    suspend fun dismissPetTip() = Unit

    // Card expansion/collapse is UI history, not a setting.
    suspend fun loadCollapsedCards(): Map<String, Boolean> = emptyMap()
    suspend fun saveCollapsedCards(collapsed: Map<String, Boolean>) = Unit

    suspend fun loadSettings(): AppSettings {
        val raw = context.dataStore.data.map { it[KEY_SETTINGS] }.first()
        if (raw.isNullOrBlank()) return AppSettings()
        return try {
            json.decodeFromString<AppSettings>(raw).migrated()
        } catch (_: Exception) {
            AppSettings()
        }
    }

    suspend fun saveSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SETTINGS] = json.encodeToString(settings)
        }
    }

    suspend fun isTeamTipDismissed(): Boolean = false
    suspend fun dismissTeamTip() = Unit

    suspend fun isGardenTipDismissed(): Boolean = false
    suspend fun dismissGardenTip() = Unit

    suspend fun isSeedTipDismissed(): Boolean = false
    suspend fun dismissSeedTip() = Unit

    suspend fun isEggTipDismissed(): Boolean = false
    suspend fun dismissEggTip() = Unit

    suspend fun isPlantTipDismissed(): Boolean = false
    suspend fun dismissPlantTip() = Unit

    suspend fun isStorageTipDismissed(): Boolean = false
    suspend fun dismissStorageTip() = Unit

    // Update-notification history is runtime-only.
    suspend fun getLastNotifiedVersion(): String? = null
    suspend fun setLastNotifiedVersion(version: String) = Unit

    /**
     * Rewrite the DataStore to the strict settings-only allowlist. This removes legacy keys
     * and any live-state JSON written by older MGAUTO builds.
     */
    suspend fun sanitizeToSettingsOnly(
        sessions: List<Session>,
        alerts: AlertConfig,
        settings: AppSettings,
    ) {
        context.dataStore.edit { prefs ->
            prefs.clear()
            prefs[KEY_SESSIONS] = json.encodeToString(persistedSessions(sessions))
            prefs[KEY_ALERTS] = json.encodeToString(persistedAlerts(alerts))
            prefs[KEY_SETTINGS] = json.encodeToString(settings.migrated())
        }
    }
}
