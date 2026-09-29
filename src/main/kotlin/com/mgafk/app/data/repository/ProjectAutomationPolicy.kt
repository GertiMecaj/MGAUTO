package com.mgafk.app.data.repository

import com.mgafk.app.data.websocket.Constants

/**
 * Pure rules shared by Projects A/B/D/F. Keeping these here makes the hard automation
 * boundaries directly unit-testable instead of burying them inside asynchronous ViewModel code.
 */
object ProjectAutomationPolicy {
    const val PROJECT_A_RESERVED_EMPTY_PLOTS = 13

    /** Project A is entirely paused at or below its 13-empty-plot reserve. */
    fun canProjectAOperate(freePlantTiles: Int): Boolean =
        freePlantTiles > PROJECT_A_RESERVED_EMPTY_PLOTS

    /** Project F intentionally ignores A's reserve; it only needs one genuinely empty dirt tile. */
    fun canProjectFPlant(freePlantTiles: Int): Boolean =
        freePlantTiles > 0

    /** Use the exact same weather representation emitted by RoomClient.LiveStatusChanged. */
    fun weatherKey(weatherId: String): String =
        Constants.formatWeather(weatherId)

    /** No active weather means Project D restores the configured Default Team. */
    fun useDefaultTeam(liveWeather: String): Boolean =
        liveWeather.equals(Constants.formatWeather(null), ignoreCase = true)

    fun isProjectBCropBlocked(
        species: String,
        mutations: List<String>,
        protectGoldSpecies: Set<String>,
        protectRainbowSpecies: Set<String>,
    ): Boolean =
        isHarvestBlocked(
            mutations = mutations,
            blockGold = species in protectGoldSpecies,
            blockRainbow = species in protectRainbowSpecies,
        )

    fun isHarvestBlocked(
        mutations: List<String>,
        blockGold: Boolean,
        blockRainbow: Boolean,
    ): Boolean {
        val hasGold = mutations.any { it.equals("Gold", ignoreCase = true) }
        val hasRainbow = mutations.any { it.equals("Rainbow", ignoreCase = true) }
        return (blockGold && hasGold) || (blockRainbow && hasRainbow)
    }
}
