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
import com.mgafk.app.data.repository.ShopItemBuyState
import com.mgafk.app.data.repository.buyState
import com.mgafk.app.ui.components.AppCard
import com.mgafk.app.ui.theme.Accent
import com.mgafk.app.ui.theme.TextMuted
import com.mgafk.app.ui.theme.TextPrimary

@Composable
fun ProjectECard(
    session: Session,
    onEnabledChange: (Boolean) -> Unit,
) {
    val available = session.shops.sumOf { shop ->
        shop.itemNames.count { item ->
            (shop.itemStocks[item] ?: 0) > 0 && session.buyState(item) == ShopItemBuyState.Buyable
        }
    }

    AppCard(title = "Project E — Auto Shop") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Buy all available shop items", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    if (available > 0) "$available buyable item type(s) currently available"
                    else "No buyable items currently available",
                    color = if (available > 0) Accent else TextMuted,
                    fontSize = 11.sp,
                )
            }
            Switch(checked = session.projectEEnabled, onCheckedChange = onEnabledChange)
        }
        Text(
            "Checks every live shop and keeps buying available items as shop confirmations arrive. One-time and capped items are skipped after their ownership limit is reached.",
            color = TextMuted,
            fontSize = 11.sp,
        )
    }
}
