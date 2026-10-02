package com.mgafk.app.data.repository

import com.mgafk.app.data.model.AbilityLog
import com.mgafk.app.data.model.InventorySeedItem
import com.mgafk.app.data.model.InventorySnapshot
import com.mgafk.app.data.model.PetSnapshot
import com.mgafk.app.data.model.Session
import com.mgafk.app.data.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPersistenceTest {
    @Test
    fun `persistent snapshot keeps configuration and strips live state`() {
        val source = Session(
            name = "AFK",
            cookie = "token",
            room = "room-1",
            wantConnected = true,
            connected = true,
            status = SessionStatus.CONNECTED,
            playerId = "p_123",
            playerName = "Live player",
            weather = "Rain",
            pets = listOf(PetSnapshot(id = "pet-1", species = "Bee")),
            logs = listOf(AbilityLog(action = "GoldGranter")),
            inventory = InventorySnapshot(seeds = listOf(InventorySeedItem("Carrot", 99))),
            projectAEnabled = true,
            projectASelectedSeeds = setOf("Carrot"),
            projectGEnabled = true,
            projectGSelectedSeeds = setOf("Bamboo"),
        )

        val saved = SessionRepository.persistedSession(source)

        assertEquals("AFK", saved.name)
        assertEquals("", saved.cookie)
        assertEquals("room-1", saved.room)
        assertFalse(saved.wantConnected)
        assertTrue(saved.projectAEnabled)
        assertEquals(setOf("Carrot"), saved.projectASelectedSeeds)
        assertTrue(saved.projectGEnabled)
        assertEquals(setOf("Bamboo"), saved.projectGSelectedSeeds)

        assertFalse(saved.connected)
        assertEquals(SessionStatus.IDLE, saved.status)
        assertEquals("", saved.playerId)
        assertEquals("", saved.playerName)
        assertEquals("", saved.weather)
        assertTrue(saved.pets.isEmpty())
        assertTrue(saved.logs.isEmpty())
        assertTrue(saved.inventory.seeds.isEmpty())
    }
}
