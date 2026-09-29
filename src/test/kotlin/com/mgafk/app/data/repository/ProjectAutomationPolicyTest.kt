package com.mgafk.app.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectAutomationPolicyTest {

    @Test fun projectA_stopsAtExactlyThirteenEmptyPlots() {
        assertTrue(ProjectAutomationPolicy.canProjectAOperate(14))
        assertFalse(ProjectAutomationPolicy.canProjectAOperate(13))
        assertFalse(ProjectAutomationPolicy.canProjectAOperate(0))
    }

    @Test fun projectF_ignoresAReserveButStillNeedsARealFreePlot() {
        assertTrue(ProjectAutomationPolicy.canProjectFPlant(13))
        assertTrue(ProjectAutomationPolicy.canProjectFPlant(1))
        assertFalse(ProjectAutomationPolicy.canProjectFPlant(0))
    }

    @Test fun weatherKeys_matchLiveWeatherFormatting() {
        assertEquals("Snow", ProjectAutomationPolicy.weatherKey("Frost"))
        assertEquals("Rain", ProjectAutomationPolicy.weatherKey("Rain"))
        assertEquals("Clear Skies", ProjectAutomationPolicy.weatherKey("Sunny"))
        assertTrue(ProjectAutomationPolicy.useDefaultTeam("Clear Skies"))
        assertFalse(ProjectAutomationPolicy.useDefaultTeam("Snow"))
    }

    @Test fun goldAndRainbowFilters_areIndependent() {
        assertTrue(ProjectAutomationPolicy.isHarvestBlocked(listOf("Gold"), blockGold = true, blockRainbow = false))
        assertFalse(ProjectAutomationPolicy.isHarvestBlocked(listOf("Gold"), blockGold = false, blockRainbow = true))
        assertTrue(ProjectAutomationPolicy.isHarvestBlocked(listOf("Rainbow"), blockGold = false, blockRainbow = true))
        assertFalse(ProjectAutomationPolicy.isHarvestBlocked(listOf("Rainbow"), blockGold = true, blockRainbow = false))
        assertTrue(ProjectAutomationPolicy.isHarvestBlocked(listOf("Gold", "Rainbow"), blockGold = true, blockRainbow = false))
        assertFalse(ProjectAutomationPolicy.isHarvestBlocked(emptyList(), blockGold = true, blockRainbow = true))
    }
}
