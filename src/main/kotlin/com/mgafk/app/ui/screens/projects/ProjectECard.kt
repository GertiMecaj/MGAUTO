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
import com.mgafk.app.data.repository.ProjectEShopCatalog
import com.mgafk.app.data.repository.ShopItemBuyState
import com.mgafk.app.data.repository.buyState
import com.mgafk.app.ui.components.AppCard
import com.mgafk.app.ui.theme.Accent
import com.mgafk.app.ui.theme.TextMuted
import com.mgafk.app.ui.theme.TextPrimary

@Composable
fun ProjectECard(
    session: Session,
    apiReady: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onItemSelectionChange: (Set<String>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val fullCatalog = remember(apiReady) {
        if (apiReady) ProjectEShopCatalog.allItems() else emptyList()
    }
    val visibleItems = remember(apiReady, query, fullCatalog) {
        fullCatalog.filter { item ->
            query.isBlank() ||
                item.name.contains(query, ignoreCase = true) ||
                item.id.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true) ||
                item.eligibleShops.any { it.contains(query, ignoreCase = true) }
        }
    }

    val selectedAvailableNow = session.shops.asSequence()
        .flatMap { shop -> shop.itemNames.asSequence().map { item -> shop to item } }
        .filter { (shop, item) ->
            item in session.projectESelectedItems &&
                (shop.itemStocks[item] ?: 0) > 0 &&
                session.buyState(item) == ShopItemBuyState.Buyable
        }
        .map { it.second }
        .toSet()
        .size

    AppCard(title = "Project E — Auto Shop") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto-buy selected items", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    if (selectedAvailableNow > 0) {
                        selectedAvailableNow.toString() + " selected item type(s) available now"
                    } else {
                        "No selected items currently available"
                    },
                    color = if (selectedAvailableNow > 0) Accent else TextMuted,
                    fontSize = 11.sp,
                )
            }
            Switch(checked = session.projectEEnabled, onCheckedChange = onEnabledChange)
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search all shop items") },
        )

        Text(
            session.projectESelectedItems.size.toString() + " selected • " +
                fullCatalog.size.toString() + " purchasable items in game data",
            color = TextMuted,
            fontSize = 11.sp,
        )

        Text(
            "Only checked items are purchased. Selected stackable items are bought repeatedly whenever they are in stock; one-time and capped items stop automatically at their game limit.",
            color = TextMuted,
            fontSize = 11.sp,
        )

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            visibleItems.forEach { item ->
                val selected = item.id in session.projectESelectedItems
                val liveStock = session.shops.sumOf { shop ->
                    if (item.id in shop.itemNames) shop.itemStocks[item.id] ?: 0 else 0
                }
                val state = session.buyState(item.id)
                val status = when {
                    state == ShopItemBuyState.Owned -> "Owned"
                    state == ShopItemBuyState.MaxReached -> "Max"
                    liveStock > 0 -> "In stock: " + liveStock
                    else -> "Not currently stocked"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onItemSelectionChange(
                                if (selected) session.projectESelectedItems - item.id
                                else session.projectESelectedItems + item.id
                            )
                        }
                        .padding(vertical = 3.dp),
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { checked ->
                            onItemSelectionChange(
                                if (checked) session.projectESelectedItems + item.id
                                else session.projectESelectedItems - item.id
                            )
                        },
                    )
                    Column {
                        Text(
                            item.name,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            listOfNotNull(
                                item.category,
                                item.rarity,
                                item.eligibleShops.joinToString("/"),
                                status,
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
