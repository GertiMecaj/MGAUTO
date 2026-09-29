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
fun ProjectBCard(
    session: Session,
    apiReady: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onPlantSelectionChange: (Set<String>) -> Unit,
    onProtectGoldChange: (species: String, protect: Boolean) -> Unit,
    onProtectRainbowChange: (species: String, protect: Boolean) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val plants = remember(apiReady, query) {
        if (!apiReady) emptyList() else MgApi.getPlants().values
            .filter { query.isBlank() || it.name.contains(query, true) || it.id.contains(query, true) }
            .sortedBy { it.name.lowercase() }
    }
    val inventorySlots = session.inventory.run {
        seeds.size + eggs.size + produce.size + plants.size + pets.size + tools.size + decors.size
    }

    AppCard(title = "Project B — Auto Harvest") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto-harvest", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    if (inventorySlots >= StorageCapacity.INVENTORY_LIMIT)
                        "Inventory full — selling harvested crops"
                    else "Harvesting ready selected crops • ${inventorySlots}/${StorageCapacity.INVENTORY_LIMIT} slots",
                    color = if (inventorySlots >= StorageCapacity.INVENTORY_LIMIT) Accent else TextMuted,
                    fontSize = 11.sp,
                )
            }
            Switch(checked = session.projectBEnabled, onCheckedChange = onEnabledChange)
        }

        Text(
            "Select every species you want harvested. Mutation protection is configured separately for each selected species.",
            color = TextMuted,
            fontSize = 11.sp,
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search plants") },
        )

        Text(
            "${session.projectBSelectedPlants.size} selected • ${MgApi.getPlants().size} plants in game data",
            color = TextMuted,
            fontSize = 11.sp,
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            plants.forEach { plant ->
                val selected = plant.id in session.projectBSelectedPlants
                val protectGold = plant.id in session.projectBProtectGoldPlants
                val protectRainbow = plant.id in session.projectBProtectRainbowPlants

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onPlantSelectionChange(
                                if (selected) session.projectBSelectedPlants - plant.id
                                else session.projectBSelectedPlants + plant.id
                            )
                        }
                        .padding(vertical = 3.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { checked ->
                                onPlantSelectionChange(
                                    if (checked) session.projectBSelectedPlants + plant.id
                                    else session.projectBSelectedPlants - plant.id
                                )
                            },
                        )
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                plant.name,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            if (!plant.rarity.isNullOrBlank()) {
                                Text(plant.rarity, color = TextMuted, fontSize = 10.sp)
                            }
                        }
                    }

                    if (selected) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 38.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onProtectGoldChange(plant.id, !protectGold) },
                            ) {
                                Checkbox(
                                    checked = protectGold,
                                    onCheckedChange = { onProtectGoldChange(plant.id, it) },
                                )
                                Text(
                                    "Protect Gold",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onProtectRainbowChange(plant.id, !protectRainbow) },
                            ) {
                                Checkbox(
                                    checked = protectRainbow,
                                    onCheckedChange = { onProtectRainbowChange(plant.id, it) },
                                )
                                Text(
                                    "Protect Rainbow",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                            }
                        }

                        if (!protectGold && !protectRainbow) {
                            Text(
                                "Harvest all mutations",
                                color = Accent,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(start = 50.dp, bottom = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
