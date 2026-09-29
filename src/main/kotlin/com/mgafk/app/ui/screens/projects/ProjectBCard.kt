package com.mgafk.app.ui.screens.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    onBlockGoldChange: (Boolean) -> Unit,
    onBlockRainbowChange: (Boolean) -> Unit,
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

        Text("Harvest filters", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Row(modifier = Modifier.fillMaxWidth()) {
            Checkbox(checked = session.projectBBlockGold, onCheckedChange = onBlockGoldChange)
            Text("Protect Gold", color = TextPrimary, modifier = Modifier.padding(top = 12.dp))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Checkbox(checked = session.projectBBlockRainbow, onCheckedChange = onBlockRainbowChange)
            Text("Protect Rainbow", color = TextPrimary, modifier = Modifier.padding(top = 12.dp))
        }

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

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            plants.forEach { plant ->
                val selected = plant.id in session.projectBSelectedPlants
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        onPlantSelectionChange(
                            if (selected) session.projectBSelectedPlants - plant.id
                            else session.projectBSelectedPlants + plant.id
                        )
                    }.padding(vertical = 3.dp),
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { checked ->
                            onPlantSelectionChange(
                                if (checked) session.projectBSelectedPlants + plant.id
                                else session.projectBSelectedPlants - plant.id
                            )
                        },
                    )
                    Column {
                        Text(plant.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        if (!plant.rarity.isNullOrBlank()) Text(plant.rarity, color = TextMuted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
