package com.mgafk.app.service
import com.mgafk.app.data.model.AbilityFormatter
import com.mgafk.app.data.model.AbilityLog
import com.mgafk.app.data.model.AlarmSchedule
import com.mgafk.app.data.model.AlertConfig
import com.mgafk.app.data.model.AlertMode
import com.mgafk.app.data.model.AlertSection
import com.mgafk.app.data.model.InventoryCropsItem
import com.mgafk.app.data.model.PetSnapshot
import com.mgafk.app.data.model.ShopSnapshot
import com.mgafk.app.data.model.isSilentAt
import com.mgafk.app.data.repository.MgApi
import com.mgafk.app.data.websocket.Constants
import java.time.LocalDateTime
class AlertNotifier(context: com.mgafk.app.desktop.DesktopContext) {
    var alarmSoundUri = ""
    var alarmSchedules: List<AlarmSchedule> = emptyList()
    var alarmVolume = 1f
    private val shopAlerts = ShopAlertTracker()
    private val petHungerAlerts = PetHungerAlertTracker()
    private val weatherTracker = WeatherAlertTracker()
    private var firedTroughLow = false
    private data class DisplayItem(val label: String, val spriteUrl: String? = null)
    // ── Public check methods ──

    /**
     * Alerts on the pets of [sessionId] that just got hungry or just ran out.
     *
     * Hunger updates arrive every few seconds, so the edges are what gets announced, never the
     * standing level - see [PetHungerAlertTracker]. The session is part of the call because one
     * notifier serves them all and each update carries a single session's pets.
     */
    fun checkPetHunger(sessionId: String, pets: List<PetSnapshot>, alerts: AlertConfig) {
        val hungerKey = "hunger<5"
        val hungerAlert = alerts.items[hungerKey] ?: return
        if (!hungerAlert.enabled) return

        // Pets whose species has no known max hunger have no percentage, so they are not judged.
        val measured = pets.mapNotNull { pet ->
            val maxHunger = Constants.maxHungerFor(pet.species) ?: return@mapNotNull null
            pet to (pet.hunger.toFloat() / maxHunger) * 100
        }
        val newAlerts = petHungerAlerts.newlyHungry(
            sessionId = sessionId,
            readings = measured.map { (pet, percent) ->
                PetHungerAlertTracker.PetHungerReading(pet.id, percent)
            },
            threshold = alerts.petHungerThreshold.toFloat(),
        )
        if (newAlerts.isEmpty()) return

        val measuredById = measured.associateBy { (pet, _) -> pet.id }
        val items = newAlerts.mapNotNull { alert ->
            val (pet, percent) = measuredById[alert.petId] ?: return@mapNotNull null
            val state = if (alert.stage == PetHungerAlertTracker.Stage.EMPTY) {
                "out of food"
            } else {
                "${"%.1f".format(percent)}%"
            }
            DisplayItem(
                label = "${pet.name} (${pet.species}): $state",
                spriteUrl = MgApi.findPet(pet.species.lowercase())?.sprite,
            )
        }
        dispatchAlert("Pet Hunger", items, alerts.resolveMode(AlertSection.PET, hungerKey))
    }

    fun checkWeather(weather: String, previousWeather: String, alerts: AlertConfig) {
        // Asked before the alert is looked up, so the weather is recorded either way: see
        // WeatherAlertTracker for why doing it the other way round silenced Amber Moon.
        if (!weatherTracker.isNewWeather(weather, previousWeather)) return
        val key = "weather:$weather"
        val alert = alerts.items[key] ?: return
        if (!alert.enabled) return

        val weatherEntry = MgApi.weatherInfo(weather)
        dispatchAlert(
            title = "Weather Change",
            items = listOf(DisplayItem(label = weather, spriteUrl = weatherEntry?.sprite)),
            mode = alerts.resolveMode(AlertSection.WEATHER, key),
        )
    }

    /**
     * @param restockedShopTypes shop types that just rolled a new stock, i.e. that are allowed to
     *   alert again for items they were already stocking - see [ShopAlertTracker].
     */
    fun checkShopItems(
        shops: List<ShopSnapshot>,
        alerts: AlertConfig,
        restockedShopTypes: Set<String> = emptySet(),
    ) {
        val stockedKeys = shops.flatMap { shop ->
            shop.itemNames.map { ShopAlertTracker.keyOf(shop.type, it) }
        }
        val pending = shopAlerts.newlyStocked(stockedKeys, restockedShopTypes) {
            alerts.items[it]?.enabled == true
        }.toMutableSet()
        if (pending.isEmpty()) return

        // Group fired items by their resolved mode so a CUSTOM section can mix
        // notification + alarm items within the same batch.
        val itemsByMode = mutableMapOf<AlertMode, MutableList<DisplayItem>>()

        for (shop in shops) {
            for (itemName in shop.itemNames) {
                val key = ShopAlertTracker.keyOf(shop.type, itemName)
                if (!pending.remove(key)) continue

                val entry = resolveShopEntry(shop.type, itemName)
                val display = DisplayItem(
                    label = entry?.name ?: itemName,
                    spriteUrl = entry?.sprite,
                )
                val mode = alerts.resolveMode(AlertSection.SHOP, key)
                itemsByMode.getOrPut(mode) { mutableListOf() }.add(display)
            }
        }

        for ((mode, group) in itemsByMode) {
            if (group.isNotEmpty()) {
                dispatchAlert("Shop Alert", group, mode)
            }
        }
    }

    fun checkFeedingTrough(trough: List<InventoryCropsItem>, alerts: AlertConfig) {
        val troughKey = "trough_low"
        val troughAlert = alerts.items[troughKey] ?: return
        if (!troughAlert.enabled) return

        val isLow = trough.size <= 1

        if (isLow && !firedTroughLow) {
            firedTroughLow = true
            dispatchAlert(
                title = "Feeding Trough",
                items = listOf(DisplayItem(label = "Only ${trough.size} item(s) left in trough")),
                mode = alerts.resolveMode(AlertSection.FEEDING_TROUGH, troughKey),
            )
        } else if (!isLow) {
            firedTroughLow = false
        }
    }

    /**
     * Fire a notification for a single pet ability proc, if that ability is
     * enabled in the alert config. Always a (silent-capable) notification -
     * ability procs are notification-only, with no alarm/custom mode. Called
     * once per genuinely-new proc, so no dedup tracking is needed here.
     */
    fun checkAbilityProc(log: AbilityLog, alerts: AlertConfig) {
        val key = "ability:${log.action}"
        if (alerts.items[key]?.enabled != true) return

        val abilityName = MgApi.abilityDisplayName(log.action)
        val petLabel = log.petName.ifBlank { log.petSpecies }
        val description = AbilityFormatter.format(log)
        val label = listOf(petLabel, description.orEmpty())
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { abilityName }

        dispatchAlert(abilityName, listOf(DisplayItem(label = label)), AlertMode.NOTIFICATION)
    }

    private fun resolveShopEntry(type: String, itemId: String): MgApi.GameEntry? {
        val map = when (type) {
            "seed" -> MgApi.getPlants()
            "tool" -> MgApi.getItems()
            "egg" -> MgApi.getEggs()
            "decor" -> MgApi.getDecors()
            else -> null
        }
        return map?.get(itemId) ?: MgApi.findItem(itemId)
    }


    private fun dispatchAlert(title: String, items: List<DisplayItem>, mode: AlertMode) {
        val body = items.joinToString("\n") { it.label }
        com.mgafk.app.desktop.DesktopAlerts.show(title, body, mode == AlertMode.ALARM && !alarmSchedules.isSilentAt(LocalDateTime.now()), alarmSoundUri, alarmVolume)
    }
    fun notifyUpdate(version: String, downloadUrl: String) = com.mgafk.app.desktop.DesktopAlerts.show("MGAUTO $version available", "Download from the update button in the menu.")
    fun notifyDisconnect(sessionName: String, code: Int?, reason: String) = com.mgafk.app.desktop.DesktopAlerts.show("$sessionName disconnected", "${code ?: ""} $reason")
    fun cancelDisconnectNotification(sessionName: String) { /* Tray balloons expire automatically. */ }
    fun stopAlarm() = com.mgafk.app.desktop.DesktopAlerts.stop()
    fun cleanup() = stopAlarm()
    fun testAlert(mode: AlertMode) = dispatchAlert("Test alert", listOf(DisplayItem("MGAUTO desktop alerts are working")), mode)
    fun previewAlarmSound() = com.mgafk.app.desktop.DesktopAlerts.preview(alarmSoundUri, alarmVolume)
    fun stopPreviewSound() = com.mgafk.app.desktop.DesktopAlerts.stop()
}
