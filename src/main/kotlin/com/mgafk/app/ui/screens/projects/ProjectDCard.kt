package com.mgafk.app.ui.screens.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mgafk.app.data.model.PetTeam
import com.mgafk.app.data.model.Session
import com.mgafk.app.data.repository.MgApi
import com.mgafk.app.data.repository.PetTeams
import com.mgafk.app.data.repository.ProjectAutomationPolicy
import com.mgafk.app.ui.components.AppCard
import com.mgafk.app.ui.theme.Accent
import com.mgafk.app.ui.theme.SurfaceBorder
import com.mgafk.app.ui.theme.TextMuted
import com.mgafk.app.ui.theme.TextPrimary

@Composable
fun ProjectDCard(
    session: Session,
    apiReady: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onWeatherTeamsChange: (String, Set<String>) -> Unit,
    onDefaultTeamChange: (String?) -> Unit,
    onSellingTeamChange: (String?) -> Unit,
    onHatchingTeamChange: (String?) -> Unit,
) {
    val activeIds = session.pets.map { it.id }
    val activeTeam = session.petTeams.firstOrNull { PetTeams.isActive(it, activeIds) }
    val weathers = remember(apiReady) {
        if (!apiReady) emptyList() else MgApi.getWeathers().values
            .filterNot { ProjectAutomationPolicy.useDefaultTeam(ProjectAutomationPolicy.weatherKey(it.id)) }
            .sortedBy { it.name.lowercase() }
    }

    AppCard(title = "Project D — Team Auto Change") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Automatic weather teams", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    "Active: " + (activeTeam?.name ?: "custom / unknown"),
                    color = Accent,
                    fontSize = 11.sp,
                )
            }
            Switch(checked = session.projectDEnabled, onCheckedChange = onEnabledChange)
        }

        TeamRolePicker("Default team", session.petTeams, session.projectDDefaultTeamId, onDefaultTeamChange)
        TeamRolePicker("Selling team", session.petTeams, session.projectDSellingTeamId, onSellingTeamChange)
        TeamRolePicker("Hatching team", session.petTeams, session.projectDHatchingTeamId, onHatchingTeamChange)

        HorizontalDivider(color = SurfaceBorder)
        Text(
            "Weather teams — select one or more saved teams for each weather. The first assigned team in your saved-team order is deployed; if it is removed, the next assigned team is used.",
            color = TextMuted,
            fontSize = 11.sp,
        )

        if (!apiReady) {
            Text("Loading weather data…", color = TextMuted, fontSize = 11.sp)
        } else if (weathers.isEmpty()) {
            Text("No weather definitions available.", color = TextMuted, fontSize = 11.sp)
        } else if (session.petTeams.isEmpty()) {
            Text("Create pet teams in Pets first.", color = TextMuted, fontSize = 11.sp)
        } else {
            weathers.forEach { weather ->
                val weatherKey = ProjectAutomationPolicy.weatherKey(weather.id)
                val assigned = session.projectDWeatherTeams.entries
                    .firstOrNull { it.key.equals(weatherKey, ignoreCase = true) }?.value.orEmpty()
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(weather.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    session.petTeams.forEach { team ->
                        val checked = team.id in assigned
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onWeatherTeamsChange(
                                    weatherKey,
                                    if (checked) assigned - team.id else assigned + team.id,
                                )
                            },
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { value ->
                                    onWeatherTeamsChange(
                                        weatherKey,
                                        if (value) assigned + team.id else assigned - team.id,
                                    )
                                },
                            )
                            Text(team.name.ifBlank { team.id }, color = TextPrimary, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamRolePicker(
    title: String,
    teams: List<PetTeam>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(null) }) {
            RadioButton(selected = selectedId == null, onClick = { onSelect(null) })
            Text("None", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
        }
        teams.forEach { team ->
            Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(team.id) }) {
                RadioButton(selected = selectedId == team.id, onClick = { onSelect(team.id) })
                Text(team.name.ifBlank { team.id }, color = TextPrimary, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}
