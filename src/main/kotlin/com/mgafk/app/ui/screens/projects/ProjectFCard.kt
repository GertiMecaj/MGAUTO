package com.mgafk.app.ui.screens.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mgafk.app.data.model.Session
import com.mgafk.app.ui.components.AppCard
import com.mgafk.app.ui.theme.Accent
import com.mgafk.app.ui.theme.TextMuted
import com.mgafk.app.ui.theme.TextPrimary

@Composable
fun ProjectFCard(
    session: Session,
    onEnabledChange: (Boolean) -> Unit,
) {
    val inventoryEggs = session.inventory.eggs.sumOf { it.quantity }
    val storedEggs = session.storedEggs.sumOf { it.quantity }
    val plantedEggs = session.gardenEggs.size

    AppCard(title = "Project F — Auto Eggs") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Retrieve, plant & hatch eggs", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    "Inventory ${inventoryEggs} • Stored ${storedEggs} • Garden ${plantedEggs}",
                    color = if (inventoryEggs + storedEggs + plantedEggs > 0) Accent else TextMuted,
                    fontSize = 11.sp,
                )
            }
            Switch(checked = session.projectFEnabled, onCheckedChange = onEnabledChange)
        }
        Text(
            "Retrieves eggs from any storage that actually contains them, plants on any free dirt plot even inside Project A's 13-plot reserve, and automatically hatches mature eggs.",
            color = TextMuted,
            fontSize = 11.sp,
        )
        Text(
            "Before hatching, Project D's Hatching Team is deployed and confirmed. After hatch, it stays active for 10 seconds, then the current weather/default team is restored.",
            color = TextMuted,
            fontSize = 11.sp,
        )
    }
}
