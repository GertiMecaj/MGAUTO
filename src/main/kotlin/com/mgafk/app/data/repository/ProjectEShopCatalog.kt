package com.mgafk.app.data.repository

/**
 * Complete Project E catalog built from the live game-data API, not from the current shop roll.
 *
 * eligibleShops is authoritative when present. Older API entries omit it; those entries use
 * the game's normal category shop, matching MgApi's existing compatibility rule.
 */
data class ProjectEShopItem(
    val id: String,
    val name: String,
    val category: String,
    val rarity: String?,
    val eligibleShops: List<String>,
    val sprite: String?,
)

object ProjectEShopCatalog {
    fun allItems(): List<ProjectEShopItem> = build(
        plants = MgApi.getPlants(),
        items = MgApi.getItems(),
        eggs = MgApi.getEggs(),
        decors = MgApi.getDecors(),
    )

    internal fun build(
        plants: Map<String, MgApi.GameEntry>,
        items: Map<String, MgApi.GameEntry>,
        eggs: Map<String, MgApi.GameEntry>,
        decors: Map<String, MgApi.GameEntry>,
    ): List<ProjectEShopItem> {
        val result = linkedMapOf<String, ProjectEShopItem>()

        fun addCategory(
            entries: Map<String, MgApi.GameEntry>,
            category: String,
            defaultShop: String,
        ) {
            entries.forEach { (id, entry) ->
                result.putIfAbsent(
                    id,
                    ProjectEShopItem(
                        id = id,
                        name = entry.name,
                        category = category,
                        rarity = entry.rarity,
                        eligibleShops = entry.eligibleShops.ifEmpty { listOf(defaultShop) },
                        sprite = entry.sprite,
                    ),
                )
            }
        }

        addCategory(plants, "Seed", "Seed")
        addCategory(items, "Tool", "Tool")
        addCategory(eggs, "Egg", "Egg")
        addCategory(decors, "Decor", "Decor")

        val categoryOrder = mapOf("Seed" to 0, "Tool" to 1, "Egg" to 2, "Decor" to 3)
        return result.values.sortedWith(
            compareBy<ProjectEShopItem> { categoryOrder[it.category] ?: Int.MAX_VALUE }
                .thenBy { it.name.lowercase() }
                .thenBy { it.id.lowercase() }
        )
    }
}
