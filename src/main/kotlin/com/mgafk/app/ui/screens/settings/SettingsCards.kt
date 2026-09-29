package com.mgafk.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.mgafk.app.desktop.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mgafk.app.data.model.AlarmSchedule
import com.mgafk.app.data.model.AppSettings
import com.mgafk.app.data.model.PlantPlacementMode
import com.mgafk.app.data.model.PurchaseMode
import com.mgafk.app.data.model.WakeLockMode
import com.mgafk.app.data.model.isSilentAt
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import com.mgafk.app.ui.components.AppCard
import com.mgafk.app.ui.theme.Accent
import com.mgafk.app.ui.theme.SurfaceBorder
import com.mgafk.app.ui.theme.TextMuted
import com.mgafk.app.ui.theme.TextPrimary
import com.mgafk.app.ui.theme.TextSecondary

@Composable
fun SettingsCards(
    settings: AppSettings,
    availableStorages: Set<String> = emptySet(),
    onUpdate: (AppSettings) -> Unit,
    onPreviewAlarm: () -> Unit = {},
    onStopPreviewAlarm: () -> Unit = {},
) {
    BackgroundCard(settings = settings, onUpdate = onUpdate)
    ShopsSettingsCard(settings = settings, onUpdate = onUpdate)
    GardenSettingsCard(settings = settings, onUpdate = onUpdate)
    StoragesCard(settings = settings, availableStorages = availableStorages, onUpdate = onUpdate)
    GameplayCard(settings = settings, onUpdate = onUpdate)
    AlarmCard(
        settings = settings,
        onUpdate = onUpdate,
        onPreviewAlarm = onPreviewAlarm,
        onStopPreviewAlarm = onStopPreviewAlarm,
    )
    ReconnectionCard(settings = settings, onUpdate = onUpdate)
    DeveloperCard(settings = settings, onUpdate = onUpdate)
}

// ── Background & Battery ──

private data class DelayOption(val label: String, val value: Int)

private val SMART_DELAY_OPTIONS = listOf(
    DelayOption("5min", 5),
    DelayOption("15min", 15),
    DelayOption("30min", 30),
)

@Composable
private fun BackgroundCard(settings: AppSettings, onUpdate: (AppSettings) -> Unit) {
    AppCard(title = "Windows background operation", collapsible = true, persistKey = "settings_battery") {
        Text("Minimize to keep your sessions running. Close asks whether to keep running in the tray or quit.", color = TextSecondary)
        ToggleRow(title = "Prevent sleep while connected", description = "Keeps Windows awake while AFK sessions are active; the screen can still turn off.", checked = settings.wakeLockMode != WakeLockMode.OFF, onCheckedChange = { onUpdate(settings.copy(wakeLockMode = if (it) WakeLockMode.ALWAYS else WakeLockMode.OFF)) })
    }
}

@Composable
private fun ShopsSettingsCard(settings: AppSettings, onUpdate: (AppSettings) -> Unit) {
    AppCard(title = "Shops", collapsible = true, persistKey = "settings_shops") {
        Text("Purchase mode", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PurchaseMode.entries.forEach { mode ->
                val selected = mode == settings.purchaseMode
                val label = when (mode) {
                    PurchaseMode.SINGLE -> "Single"
                    PurchaseMode.BULK -> "Bulk"
                    PurchaseMode.HYBRID -> "Hybrid"
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected) Accent.copy(alpha = 0.12f)
                            else SurfaceBorder.copy(alpha = 0.2f)
                        )
                        .then(
                            if (selected) Modifier.border(1.dp, Accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            else Modifier.border(1.dp, SurfaceBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        )
                        .clickable { onUpdate(settings.copy(purchaseMode = mode)) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) Accent else TextSecondary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val hint = when (settings.purchaseMode) {
            PurchaseMode.SINGLE -> "Tap buys x1 only."
            PurchaseMode.BULK -> "Tap buys all remaining stock at once."
            PurchaseMode.HYBRID -> "Tap buys x1, hold buys all remaining stock."
        }
        Text(hint, fontSize = 10.sp, color = TextMuted)
    }
}

@Composable
private fun GardenSettingsCard(settings: AppSettings, onUpdate: (AppSettings) -> Unit) {
    AppCard(title = "Garden", collapsible = true, persistKey = "settings_garden") {
        Text("Manual planting", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PlantPlacementMode.entries.forEach { mode ->
                val selected = mode == settings.plantPlacementMode
                val label = when (mode) {
                    PlantPlacementMode.FREE_TILE -> "Free tiles"
                    PlantPlacementMode.GRID -> "Grid"
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected) Accent.copy(alpha = 0.12f)
                            else SurfaceBorder.copy(alpha = 0.2f)
                        )
                        .then(
                            if (selected) Modifier.border(1.dp, Accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            else Modifier.border(1.dp, SurfaceBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        )
                        .clickable { onUpdate(settings.copy(plantPlacementMode = mode)) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) Accent else TextSecondary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val hint = when (settings.plantPlacementMode) {
            PlantPlacementMode.FREE_TILE -> "Planting drops the seed on the first empty tile."
            PlantPlacementMode.GRID -> "Planting opens the garden grid so you pick the tile."
        }
        Text(hint, fontSize = 10.sp, color = TextMuted)
    }
}

// ── Storages ──

@Composable
private fun StoragesCard(
    settings: AppSettings,
    availableStorages: Set<String>,
    onUpdate: (AppSettings) -> Unit,
) {
    val hasSilo = "SeedSilo" in availableStorages
    val hasShed = "DecorShed" in availableStorages
    val hasShack = "ToolShack" in availableStorages

    AppCard(title = "Storages", collapsible = true, persistKey = "settings_storages") {
        if (!hasSilo && !hasShed && !hasShack) {
            Text(
                "Place a Seed Silo, Decor Shed or Tool Shack in your garden to enable auto-stock features.",
                fontSize = 11.sp,
                color = TextMuted,
                lineHeight = 15.sp,
            )
            return@AppCard
        }

        if (hasSilo) {
            ToggleRow(
                title = "Auto-stock Seed Silo",
                description = "Whenever a seed in your inventory matches a species already in the silo, move it in automatically.",
                checked = settings.autoStockSeedSilo,
                onCheckedChange = { onUpdate(settings.copy(autoStockSeedSilo = it)) },
            )
        }

        if (hasSilo && hasShed) Spacer(modifier = Modifier.height(10.dp))

        if (hasShed) {
            ToggleRow(
                title = "Auto-stock Decor Shed",
                description = "Whenever a decor in your inventory matches a decor already in the shed, move it in automatically.",
                checked = settings.autoStockDecorShed,
                onCheckedChange = { onUpdate(settings.copy(autoStockDecorShed = it)) },
            )
        }

        if ((hasSilo || hasShed) && hasShack) Spacer(modifier = Modifier.height(10.dp))

        if (hasShack) {
            ToggleRow(
                title = "Auto-stock Tool Shack",
                description = "Whenever a tool in your inventory matches a tool already in the shack, move it in automatically.",
                checked = settings.autoStockToolShack,
                onCheckedChange = { onUpdate(settings.copy(autoStockToolShack = it)) },
            )
        }
    }
}

// ── Gameplay ──

@Composable
private fun GameplayCard(settings: AppSettings, onUpdate: (AppSettings) -> Unit) {
    AppCard(title = "Gameplay", collapsible = true, persistKey = "settings_gameplay") {
        ToggleRow(
            title = "Instant hatch",
            description = "Skip the egg-opening animation and show the hatched pet result instantly.",
            checked = settings.instantHatch,
            onCheckedChange = { onUpdate(settings.copy(instantHatch = it)) },
        )
    }
}

// ── Alarm ──

private val DAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")
private val DAY_VALUES = listOf(1, 2, 3, 4, 5, 6, 7) // ISO 1=Mon..7=Sun

@Composable
private fun AlarmCard(
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit,
    onPreviewAlarm: () -> Unit,
    onStopPreviewAlarm: () -> Unit,
) {
    val context = LocalContext.current
    val schedules = settings.alarmSchedules

    val currentName = if (settings.alarmSoundUri.isBlank()) "Default tone" else java.io.File(settings.alarmSoundUri).name
    // Live "Silenced now" indicator - recompute every 30s.
    var nowTick by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowTick = LocalDateTime.now()
            delay(30_000L)
        }
    }
    val silencedNow = schedules.isSilentAt(nowTick)

    fun replaceSchedule(updated: AlarmSchedule) {
        onUpdate(settings.copy(alarmSchedules = schedules.map { if (it.id == updated.id) updated else it }))
    }

    AppCard(title = "Alarm", collapsible = true, persistKey = "settings_alarm") {
        // ── Sound subsection ──
        Text("Sound", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            "Sound played when an alert fires in Alarm mode.",
            fontSize = 11.sp,
            color = TextMuted,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceBorder.copy(alpha = 0.2f))
                .clickable {
                    com.mgafk.app.desktop.chooseSound()?.let { onUpdate(settings.copy(alarmSoundUri = it)) }
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Tap to change", fontSize = 12.sp, color = TextMuted)
            Spacer(modifier = Modifier.weight(1f))
            Text(currentName, fontSize = 12.sp, color = TextSecondary)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Volume slider + Test button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Volume", fontSize = 12.sp, color = TextPrimary)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${(settings.alarmVolume * 100).toInt()}%",
                fontSize = 11.sp,
                color = TextMuted,
            )
        }

        Slider(
            value = settings.alarmVolume,
            onValueChange = { onUpdate(settings.copy(alarmVolume = it.coerceIn(0f, 1f))) },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Accent,
                activeTrackColor = Accent,
                inactiveTrackColor = SurfaceBorder.copy(alpha = 0.5f),
            ),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Accent.copy(alpha = 0.12f))
                .border(1.dp, Accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .clickable { onPreviewAlarm() }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Test sound", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Accent)
        }

        // Stop preview when this composable leaves composition (e.g., user navigates away).
        DisposableEffect(Unit) {
            onDispose { onStopPreviewAlarm() }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Schedule subsection ──
        Text("Schedule", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            "Mute alarms during these hours. Notifications still arrive silently. " +
                "Add a window per routine - work nights and weekend lie-ins need different hours.",
            fontSize = 11.sp,
            color = TextMuted,
            lineHeight = 15.sp,
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (schedules.isEmpty()) {
            Text(
                "No mute window yet. Alarms always ring.",
                fontSize = 11.sp,
                color = TextMuted,
            )
        }

        schedules.forEachIndexed { index, schedule ->
            if (index > 0) Spacer(modifier = Modifier.height(10.dp))
            AlarmScheduleBlock(
                schedule = schedule,
                fallbackLabel = "Schedule ${index + 1}",
                mutedNow = schedule.isSilentAt(nowTick),
                context = context,
                onChange = { updated -> replaceSchedule(updated) },
                onDelete = {
                    onUpdate(settings.copy(alarmSchedules = schedules.filter { it.id != schedule.id }))
                },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Accent.copy(alpha = 0.12f))
                .border(1.dp, Accent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .clickable {
                    val next = AlarmSchedule(
                        label = "Schedule ${schedules.size + 1}",
                        enabled = true,
                    )
                    onUpdate(settings.copy(alarmSchedules = schedules + next))
                }
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("+ Add window", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Accent)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (silencedNow) "Silenced now: yes" else "Silenced now: no",
            fontSize = 11.sp,
            color = if (silencedNow) Accent else TextMuted,
        )
    }
}

/** One mute window: name, on/off, hours, active days. */
@Composable
private fun AlarmScheduleBlock(
    schedule: AlarmSchedule,
    fallbackLabel: String,
    mutedNow: Boolean,
    context: com.mgafk.app.desktop.DesktopContext,
    onChange: (AlarmSchedule) -> Unit,
    onDelete: () -> Unit,
) {
    // Greyed-out controls when the window is off
    val controlsAlpha = if (schedule.enabled) 1f else 0.4f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, SurfaceBorder.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = schedule.label,
                onValueChange = { onChange(schedule.copy(label = it)) },
                placeholder = { Text(fallbackLabel, fontSize = 12.sp, color = TextMuted) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = SurfaceBorder,
                    cursorColor = Accent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = schedule.enabled,
                onCheckedChange = { onChange(schedule.copy(enabled = it)) },
                colors = SwitchDefaults.colors(checkedTrackColor = Accent),
            )
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Delete window",
                tint = TextMuted,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onDelete() }
                    .padding(6.dp),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimePickerBox(
                label = "From",
                minutes = schedule.startMinute,
                enabled = schedule.enabled,
                alpha = controlsAlpha,
                modifier = Modifier.weight(1f),
                onPick = { picked -> onChange(schedule.copy(startMinute = picked)) },
                context = context,
            )
            TimePickerBox(
                label = "To",
                minutes = schedule.endMinute,
                enabled = schedule.enabled,
                alpha = controlsAlpha,
                modifier = Modifier.weight(1f),
                suffix = if (schedule.startMinute > schedule.endMinute) " (overnight)" else null,
                onPick = { picked -> onChange(schedule.copy(endMinute = picked)) },
                context = context,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text("Active days", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DAY_VALUES.forEachIndexed { index, dayValue ->
                val isSelected = dayValue in schedule.activeDays
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) Accent.copy(alpha = 0.12f * controlsAlpha)
                            else SurfaceBorder.copy(alpha = 0.2f * controlsAlpha)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Accent.copy(alpha = 0.5f * controlsAlpha)
                            else SurfaceBorder.copy(alpha = 0.4f * controlsAlpha),
                            RoundedCornerShape(8.dp),
                        )
                        .clickable(enabled = schedule.enabled) {
                            val newDays = if (isSelected) schedule.activeDays - dayValue
                            else schedule.activeDays + dayValue
                            onChange(schedule.copy(activeDays = newDays))
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = DAY_LABELS[index],
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Accent.copy(alpha = controlsAlpha)
                                else TextSecondary.copy(alpha = controlsAlpha),
                    )
                }
            }
        }

        if (mutedNow) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Muting now", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Accent)
        }
    }
}

@Composable
private fun TimePickerBox(
    label: String,
    minutes: Int,
    enabled: Boolean,
    alpha: Float,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    onPick: (Int) -> Unit,
    context: com.mgafk.app.desktop.DesktopContext,
) {
    val hour = minutes / 60
    val minute = minutes % 60
    val timeText = "%02d:%02d".format(hour, minute)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceBorder.copy(alpha = 0.2f * alpha))
            .clickable(enabled = enabled) {
                com.mgafk.app.desktop.pickTime(minutes)?.let(onPick)
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 12.sp, color = TextMuted.copy(alpha = alpha))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            timeText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary.copy(alpha = alpha),
        )
        if (suffix != null) {
            Text(suffix, fontSize = 10.sp, color = TextMuted.copy(alpha = alpha))
        }
    }
}

// ── Reconnection ──

private data class ReconnectDelayOption(val label: String, val ms: Long)

private val KICKED_DELAY_OPTIONS = listOf(
    ReconnectDelayOption("10s", 10000),
    ReconnectDelayOption("30s", 30000),
    ReconnectDelayOption("1min", 60000),
    ReconnectDelayOption("2min", 120000),
)

@Composable
private fun ReconnectionCard(settings: AppSettings, onUpdate: (AppSettings) -> Unit) {
    AppCard(title = "Reconnection", collapsible = true, persistKey = "settings_reconnect") {
        Text(
            "The app automatically retries when the connection is lost.",
            fontSize = 11.sp,
            color = TextMuted,
        )

        Spacer(modifier = Modifier.height(12.dp))

        ToggleRow(
            title = "Notify on disconnect",
            description = "Send a notification when a session loses connection.",
            checked = settings.notifyOnDisconnect,
            onCheckedChange = { onUpdate(settings.copy(notifyOnDisconnect = it)) },
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingRow(
            label = "Kicked by another session",
            description = "Wait time before reconnecting when the same account connects from another device.",
            options = KICKED_DELAY_OPTIONS,
            selectedMs = settings.retrySupersededDelayMs,
            onSelect = { onUpdate(settings.copy(retrySupersededDelayMs = it)) },
        )
    }
}

// ── Developer Options ──

@Composable
private fun DeveloperCard(settings: AppSettings, onUpdate: (AppSettings) -> Unit) {
    AppCard(title = "Developer Options", collapsible = true, persistKey = "settings_dev") {
        ToggleRow(
            title = "Show Debug menu",
            description = "Adds a Debug section in the navigation with WebSocket logs and test tools.",
            checked = settings.showDebugMenu,
            onCheckedChange = { onUpdate(settings.copy(showDebugMenu = it)) },
        )
    }
}

// ── Shared components ──

@Composable
private fun InfoButton(title: String, message: String) {
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .clickable { showDialog = true },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = "Info",
            tint = TextMuted,
            modifier = Modifier.size(16.dp),
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title, fontWeight = FontWeight.SemiBold) },
            text = { Text(message, fontSize = 13.sp, lineHeight = 18.sp) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK", color = Accent)
                }
            },
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceBorder.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
            )
            Text(
                description,
                fontSize = 10.sp,
                color = TextMuted,
                lineHeight = 14.sp,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Accent),
        )
    }
}

@Composable
private fun SettingRow(
    label: String,
    description: String,
    options: List<ReconnectDelayOption>,
    selectedMs: Long,
    onSelect: (Long) -> Unit,
) {
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        Text(description, fontSize = 10.sp, color = TextMuted, lineHeight = 14.sp)

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { option ->
                val selected = option.ms == selectedMs
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected) Accent.copy(alpha = 0.12f)
                            else SurfaceBorder.copy(alpha = 0.2f)
                        )
                        .then(
                            if (selected) Modifier.border(1.dp, Accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            else Modifier.border(1.dp, SurfaceBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        )
                        .clickable { onSelect(option.ms) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = option.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) Accent else TextSecondary,
                    )
                }
            }
        }
    }
}
