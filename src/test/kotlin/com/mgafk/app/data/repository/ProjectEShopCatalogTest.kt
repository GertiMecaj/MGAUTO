package com.mgafk.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectEShopCatalogTest {
    private fun entry(
        id: String,
        name: String,
        shops: List<String> = emptyList(),
    ) = MgApi.GameEntry(
        id = id,
        name = name,
        sprite = null,
        eligibleShops = shops,
    )

    @Test fun catalog_includesEveryPurchasableCategoryAndUsesDefaultShops() {
        val catalog = ProjectEShopCatalog.build(
            plants = mapOf("Carrot" to entry("Carrot", "Carrot")),
            items = mapOf("GoldPotion" to entry("GoldPotion", "Gold Potion", listOf("Dawn", "Tool"))),
            eggs = mapOf("RareEgg" to entry("RareEgg", "Rare Egg")),
            decors = mapOf("PetHutch" to entry("PetHutch", "Pet Hutch")),
        )

        assertEquals(setOf("Carrot", "GoldPotion", "RareEgg", "PetHutch"), catalog.map { it.id }.toSet())
        assertEquals(listOf("Seed"), catalog.first { it.id == "Carrot" }.eligibleShops)
        assertEquals(listOf("Dawn", "Tool"), catalog.first { it.id == "GoldPotion" }.eligibleShops)
        assertEquals(listOf("Egg"), catalog.first { it.id == "RareEgg" }.eligibleShops)
        assertEquals(listOf("Decor"), catalog.first { it.id == "PetHutch" }.eligibleShops)
    }

    @Test fun catalog_isStableAndDeduplicatedByItemId() {
        val duplicate = entry("Same", "Same Item")
        val catalog = ProjectEShopCatalog.build(
            plants = mapOf("Same" to duplicate),
            items = mapOf("Same" to duplicate),
            eggs = emptyMap(),
            decors = emptyMap(),
        )

        assertEquals(1, catalog.count { it.id == "Same" })
        assertTrue(catalog.first().category == "Seed")
    }
}
