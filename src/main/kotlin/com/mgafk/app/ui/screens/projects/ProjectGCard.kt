package com.mgafk.app.ui.screens.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mgafk.app.data.model.Session
import com.mgafk.app.data.repository.MgApi
import com.mgafk.app.data.repository.StorageCapacity
import com.mgafk.app.ui.components.AppCard
import com.mgafk.app.ui.theme.Accent
import com.mgafk.app.ui.theme.TextMuted
import com.mgafk.app.ui.theme.TextPrimary

@Composable
fun ProjectGCard(
    session: Session,
    apiReady: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onSeedSelectionChange: (Set<String>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val plants = remember(apiReady, query) {
        if (!apiReady) emptyList() else MgApi.getPlants().values
            .filter { query.isBlank() || it.name.contains(query, true) || it.id.contains(query, true) }
            .sortedBy { it.name }
    }

    val inventoryMatching = session.inventory.seeds
        .filter { it.species in session.projectGSelectedSeeds }
        .sumOf { it.quantity }
    val siloMatching = session.seedSilo
        .filter { it.species in session.projectGSelectedSeeds }
        .sumOf { it.quantity }
    val pending = inventoryMatching + siloMatching
    val inventorySlots = session.inventory.run {
        seeds.size + eggs.size + produce.size + plants.size + pets.size + tools.size + decors.size
    }
    val siloBlockedByFullInventory =
        inventoryMatching == 0 &&
            siloMatching > 0 &&
            inventorySlots >= StorageCapacity.INVENTORY_LIMIT

    AppCard(title = "Project G — Seed Cleaner") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto-delete selected seeds", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    when {
                        siloBlockedByFullInventory -> "Waiting for one free inventory slot"
                        pending > 0 -> "$pending matching seed(s) queued"
                        session.projectGEnabled -> "Watching inventory and Seed Silo"
                        else -> "Disabled"
                    },
                    color = if (pending > 0 && !siloBlockedByFullInventory) Accent else TextMuted,
                    fontSize = 11.sp,
                )
            }
            Switch(checked = session.projectGEnabled, onCheckedChange = onEnabledChange)
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search seeds") },
        )

        Text(
            "${session.projectGSelectedSeeds.size} selected • ${MgApi.getPlants().size} seeds in game data",
            color = TextMuted,
            fontSize = 11.sp,
        )

        Text(
            "Selected seeds are sent through the game's Wish action as soon as they appear. " +
                "Seeds found in the Seed Silo are retrieved one at a time, confirmed by server state, then deleted.",
            color = TextMuted,
            fontSize = 11.sp,
        )

        Text(
            "Project G takes priority over Project A, Project E seed purchases, and Seed Silo auto-stock for the same species.",
            color = TextMuted,
            fontSize = 10.sp,
        )

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            plants.forEach { seed ->
                val selected = seed.id in session.projectGSelectedSeeds
                val invQty = session.inventory.seeds.find { it.species == seed.id }?.quantity ?: 0
                val siloQty = session.seedSilo.find { it.species == seed.id }?.quantity ?: 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSeedSelectionChange(
                                if (selected) session.projectGSelectedSeeds - seed.id
                                else session.projectGSelectedSeeds + seed.id
                            )
                        }
                        .padding(vertical = 3.dp),
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { checked ->
                            onSeedSelectionChange(
                                if (checked) session.projectGSelectedSeeds + seed.id
                                else session.projectGSelectedSeeds - seed.id
                            )
                        },
                    )
                    Column {
                        Text(
                            seed.name,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            listOfNotNull(
                                seed.rarity,
                                if (invQty > 0) "Inventory: $invQty" else null,
                                if (siloQty > 0) "Seed Silo: $siloQty" else null,
                            ).joinToString(" • "),
                            color = TextMuted,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}
