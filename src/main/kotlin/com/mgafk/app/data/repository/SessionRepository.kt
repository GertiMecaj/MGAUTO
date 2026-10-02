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
    corruptionHandler = ReplaceFileCorruptionHandler {
        // Keep the broken bytes for diagnosis/recovery, then let DataStore recreate a clean file.
        runCatching {
            if (settingsFile.exists()) {
                settingsFile.copyTo(
                    java.io.File(
                        settingsFile.parentFile,
                        "settings.preferences_pb.corrupt-" + System.currentTimeMillis(),
                    ),
                    overwrite = false,
                )
            }
        }
        emptyPreferences()
    },
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

    suspend fun loadActiveSessionId(): String? {
        return context.dataStore.data.map { it[KEY_ACTIVE] }.first()
    }

    suspend fun saveActiveSessionId(id: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE] = id
        }
    }

    suspend fun loadAlerts(): AlertConfig {
        val raw = context.dataStore.data.map { it[KEY_ALERTS] }.first()
        if (raw.isNullOrBlank()) return AlertConfig()
        return try {
            json.decodeFromString<AlertConfig>(raw)
        } catch (_: Exception) {
            AlertConfig()
        }
    }

    suspend fun saveAlerts(config: AlertConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ALERTS] = json.encodeToString(config)
        }
    }

    suspend fun isShopTipDismissed(): Boolean {
        return context.dataStore.data.map { it[KEY_SHOP_TIP] ?: false }.first()
    }

    suspend fun dismissShopTip() {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHOP_TIP] = true
        }
    }

    suspend fun isTroughTipDismissed(): Boolean {
        return context.dataStore.data.map { it[KEY_TROUGH_TIP] ?: false }.first()
    }

    suspend fun dismissTroughTip() {
        context.dataStore.edit { prefs ->
            prefs[KEY_TROUGH_TIP] = true
        }
    }

    suspend fun isPetTipDismissed(): Boolean {
        return context.dataStore.data.map { it[KEY_PET_TIP] ?: false }.first()
    }

    suspend fun dismissPetTip() {
        context.dataStore.edit { prefs ->
            prefs[KEY_PET_TIP] = true
        }
    }

    suspend fun loadCollapsedCards(): Map<String, Boolean> {
        val raw = context.dataStore.data.map { it[KEY_COLLAPSED_CARDS] }.first()
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            json.decodeFromString<Map<String, Boolean>>(raw)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    suspend fun saveCollapsedCards(collapsed: Map<String, Boolean>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_COLLAPSED_CARDS] = json.encodeToString(collapsed)
        }
    }

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

    suspend fun isTeamTipDismissed(): Boolean {
        return context.dataStore.data.map { it[KEY_TEAM_TIP] ?: false }.first()
    }

    suspend fun dismissTeamTip() {
        context.dataStore.edit { prefs ->
            prefs[KEY_TEAM_TIP] = true
        }
    }

    suspend fun isGardenTipDismissed(): Boolean {
        return context.dataStore.data.map { it[KEY_GARDEN_TIP] ?: false }.first()
    }

    suspend fun dismissGardenTip() {
        context.dataStore.edit { prefs ->
            prefs[KEY_GARDEN_TIP] = true
        }
    }

    suspend fun isSeedTipDismissed(): Boolean {
        return context.dataStore.data.map { it[KEY_SEED_TIP] ?: false }.first()
    }

    suspend fun dismissSeedTip() {
        context.dataStore.edit { prefs ->
            prefs[KEY_SEED_TIP] = true
        }
    }

    suspend fun isEggTipDismissed(): Boolean {
        return context.dataStore.data.map { it[KEY_EGG_TIP] ?: false }.first()
    }

    suspend fun dismissEggTip() {
        context.dataStore.edit { prefs ->
            prefs[KEY_EGG_TIP] = true
        }
    }

    suspend fun isPlantTipDismissed(): Boolean {
        return context.dataStore.data.map { it[KEY_PLANT_TIP] ?: false }.first()
    }

    suspend fun dismissPlantTip() {
        context.dataStore.edit { prefs ->
            prefs[KEY_PLANT_TIP] = true
        }
    }

    suspend fun isStorageTipDismissed(): Boolean {
        return context.dataStore.data.map { it[KEY_STORAGE_TIP] ?: false }.first()
    }

    suspend fun dismissStorageTip() {
        context.dataStore.edit { prefs ->
            prefs[KEY_STORAGE_TIP] = true
        }
    }

    suspend fun getLastNotifiedVersion(): String? {
        return context.dataStore.data.map { it[KEY_NOTIFIED_VERSION] }.first()
    }

    suspend fun setLastNotifiedVersion(version: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NOTIFIED_VERSION] = version
        }
    }
}
