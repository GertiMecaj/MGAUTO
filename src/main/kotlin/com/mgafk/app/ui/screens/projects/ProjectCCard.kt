package com.mgafk.app.ui.screens.projects

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mgafk.app.data.model.Session
import com.mgafk.app.data.repository.MgApi
import com.mgafk.app.ui.components.AppCard
import com.mgafk.app.ui.theme.Accent
import com.mgafk.app.ui.theme.TextMuted
import com.mgafk.app.ui.theme.TextPrimary

@Composable
fun ProjectCCard(session: Session, apiReady: Boolean, onEnabledChange: (Boolean) -> Unit) {
    val hungry = session.pets.filter { pet ->
        val max = MgApi.findPet(pet.species)?.coinsToFullyReplenishHunger ?: 1000
        max > 0 && pet.hunger / max.toDouble() < 0.50
    }

    AppCard(title = "Project C — Auto Pet Feeding") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Feed pets below 50% hunger", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    if (!apiReady) "Loading pet diets…"
                    else if (hungry.isEmpty()) "All active pets are above the feeding threshold"
                    else "${hungry.size} pet(s) currently need feeding",
                    color = if (hungry.isEmpty()) TextMuted else Accent,
                    fontSize = 11.sp,
                )
            }
            Switch(checked = session.projectCEnabled, onCheckedChange = onEnabledChange)
        }

        Text(
            "Uses each pet species' live diet. If compatible food is already harvested it is fed first; otherwise a mature compatible crop is harvested from the garden and fed after server confirmation.",
            color = TextMuted,
            fontSize = 11.sp,
        )
    }
}
