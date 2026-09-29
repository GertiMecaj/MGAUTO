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
import com.mgafk.app.ui.components.AppCard
import com.mgafk.app.ui.theme.Accent
import com.mgafk.app.ui.theme.TextMuted
import com.mgafk.app.ui.theme.TextPrimary

@Composable
fun ProjectACard(
    session: Session,
    apiReady: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onSeedSelectionChange: (Set<String>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val plants = remember(apiReady, query) {
        if (!apiReady) emptyList() else MgApi.getPlants().values
            .filter { query.isBlank() || it.name.contains(query, true) || it.id.contains(query, true) }
            .sortedWith(compareByDescending<MgApi.GameEntry> { it.purchasePrice ?: 0L }.thenBy { it.name })
    }

    AppCard(title = "Project A — Auto Seeds") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto-buy & plant", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    if (session.freePlantTiles <= 13) "Planting paused — 13 empty plots protected"
                    else "Planting active — ${session.freePlantTiles} empty plots",
                    color = if (session.freePlantTiles <= 13) TextMuted else Accent,
                    fontSize = 11.sp,
                )
            }
            Switch(checked = session.projectAEnabled, onCheckedChange = onEnabledChange)
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search seeds") },
        )

        Text(
            "${session.projectASelectedSeeds.size} selected • ${MgApi.getPlants().size} seeds in game data",
            color = TextMuted,
            fontSize = 11.sp,
        )

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            plants.forEach { seed ->
                val selected = seed.id in session.projectASelectedSeeds
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        onSeedSelectionChange(
                            if (selected) session.projectASelectedSeeds - seed.id
                            else session.projectASelectedSeeds + seed.id
                        )
                    }.padding(vertical = 3.dp),
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { checked ->
                            onSeedSelectionChange(
                                if (checked) session.projectASelectedSeeds + seed.id
                                else session.projectASelectedSeeds - seed.id
                            )
                        },
                    )
                    Column {
                        Text(seed.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(
                            listOfNotNull(seed.rarity, seed.purchasePrice?.let { "${it} coins" }).joinToString(" • "),
                            color = TextMuted,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}
