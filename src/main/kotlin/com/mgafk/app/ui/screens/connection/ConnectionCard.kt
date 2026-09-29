package com.mgafk.app.ui.screens.connection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mgafk.app.data.model.Session
import com.mgafk.app.data.model.SessionStatus
import com.mgafk.app.ui.components.AppCard
import com.mgafk.app.ui.components.StatusChip
import com.mgafk.app.ui.theme.Accent
import com.mgafk.app.ui.theme.StatusConnected
import com.mgafk.app.ui.theme.StatusError
import com.mgafk.app.ui.theme.SurfaceBorder
import com.mgafk.app.ui.theme.TextMuted
import com.mgafk.app.ui.theme.TextPrimary

@Composable
fun ConnectionCard(
    session: Session,
    onCookieChange: (String) -> Unit,
    onRoomChange: (String) -> Unit,
    onManualGameVersionEnabledChange: (Boolean) -> Unit,
    onManualGameVersionChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = SurfaceBorder,
        focusedBorderColor = Accent,
        cursorColor = Accent,
        unfocusedLabelColor = TextMuted,
    )

    AppCard(
        modifier = modifier,
        title = "Connection",
        trailing = { StatusChip(status = session.status) },
        collapsible = true,
        persistKey = "dashboard.connection",
    ) {
        // Token input
        OutlinedTextField(
            value = session.cookie,
            onValueChange = onCookieChange,
            label = { Text("Cookie / mc_jwt token", fontSize = 12.sp) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors,
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = session.room,
            onValueChange = onRoomChange,
            label = { Text("Room ID / code (blank = create room)", fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Manual game version",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                )
                Text(
                    if (session.manualGameVersionEnabled)
                        "Use the version below for connect and reconnect."
                    else if (session.gameVersion.isNotBlank())
                        "Automatic • current: " + session.gameVersion
                    else
                        "Automatic version discovery",
                    fontSize = 10.sp,
                    color = TextMuted,
                )
            }
            Switch(
                checked = session.manualGameVersionEnabled,
                onCheckedChange = onManualGameVersionEnabledChange,
                enabled = !session.connected && !session.busy,
            )
        }

        if (session.manualGameVersionEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = session.manualGameVersion,
                onValueChange = onManualGameVersionChange,
                label = { Text("Game version", fontSize = 12.sp) },
                placeholder = { Text("Paste exact version", fontSize = 11.sp) },
                supportingText = {
                    Text(
                        "Automatic version fetching is disabled while this override is on.",
                        fontSize = 10.sp,
                    )
                },
                singleLine = true,
                enabled = !session.connected && !session.busy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Login / Logout
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onLogin,
                modifier = Modifier.weight(1f),
                enabled = !session.connected && !session.busy,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Login in Magic Garden", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onLogout,
                enabled = session.cookie.isNotBlank() && !session.connected,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Logout", fontSize = 12.sp)
            }
        }
        Text(
            "Opens Magic Garden's live sign-in page and captures the mc_jwt automatically after a successful login.",
            fontSize = 10.sp,
            color = TextMuted,
        )

        // Error message
        if (session.error.isNotBlank() && session.status == SessionStatus.ERROR) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = session.error,
                color = StatusError,
                fontSize = 11.sp,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Connect / Disconnect
        Button(
            onClick = { if (session.connected) onDisconnect() else onConnect() },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            enabled = !session.busy && session.cookie.isNotBlank() && (!session.manualGameVersionEnabled || session.manualGameVersion.isNotBlank()),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (session.connected) StatusError else StatusConnected,
            ),
        ) {
            Text(
                text = when {
                    session.busy -> "Connecting..."
                    session.connected -> "Disconnect"
                    else -> "Connect"
                },
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }
}
