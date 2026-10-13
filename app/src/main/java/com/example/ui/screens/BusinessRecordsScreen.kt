package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.ShopViewModel

data class RecordTabItem(val title: String, val icon: ImageVector)

@Composable
fun BusinessRecordsScreen(
    viewModel: ShopViewModel,
    initialTab: Int = 0
) {
    var selectedTabIndex by remember { mutableIntStateOf(initialTab) }

    val tabs = listOf(
        RecordTabItem("Sales", Icons.Default.PointOfSale),
        RecordTabItem("Purchases", Icons.Default.ShoppingCart),
        RecordTabItem("Khata / Udhar", Icons.Default.MenuBook),
        RecordTabItem("Expenses", Icons.Default.AttachMoney)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = EmeraldPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("records_tab_row")
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = tab.title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    },
                    icon = {
                        Icon(tab.icon, contentDescription = tab.title)
                    },
                    modifier = Modifier.testTag("tab_${tab.title.lowercase().replace(" ", "_").replace("/", "_")}")
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTabIndex) {
                0 -> SalesTabContent(viewModel = viewModel)
                1 -> PurchasesTabContent(viewModel = viewModel)
                2 -> KhataTabContent(viewModel = viewModel)
                3 -> ExpensesTabContent(viewModel = viewModel)
            }
        }
    }
}
