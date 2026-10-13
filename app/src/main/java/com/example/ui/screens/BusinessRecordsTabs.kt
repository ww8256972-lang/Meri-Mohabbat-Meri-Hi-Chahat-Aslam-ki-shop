package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CustomerEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.PaymentMode
import com.example.data.model.ProductEntity
import com.example.data.model.PurchaseEntity
import com.example.data.model.SaleEntity
import com.example.data.model.SupplierEntity
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.ShopViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------
// 1. SALES TAB CONTENT
// ---------------------------------------------------------------------------
@Composable
fun SalesTabContent(viewModel: ShopViewModel) {
    val sales by viewModel.sales.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    var showCreateSaleDialog by remember { mutableStateOf(false) }
    var selectedSaleForReceipt by remember { mutableStateOf<SaleEntity?>(null) }
    var saleToCancel by remember { mutableStateOf<SaleEntity?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Action Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sales Records (${sales.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val totalSalesAmt = sales.sumOf { it.totalAmount }
                Text(
                    text = "Total Sales: ₹${String.format(Locale.getDefault(), "%.2f", totalSalesAmt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = { showCreateSaleDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btn_record_sale")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Record Sale", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (sales.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PointOfSale,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Sales Recorded",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "When you sell bidi bundles or packets, record them here to track revenue and customer balances.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showCreateSaleDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("+ Record First Sale")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sales, key = { it.id }) { sale ->
                    SaleItemCard(
                        sale = sale,
                        onViewReceipt = { selectedSaleForReceipt = sale },
                        onCancel = { saleToCancel = sale }
                    )
                }
            }
        }
    }

    if (showCreateSaleDialog) {
        CreateSaleDialog(
            products = products,
            customers = customers,
            onDismiss = { showCreateSaleDialog = false },
            onSave = { customer, items, discount, paid, mode, notes ->
                viewModel.processSale(
                    customer = customer,
                    items = items,
                    discount = discount,
                    paidAmount = paid,
                    paymentMode = mode,
                    dueDate = null,
                    notes = notes,
                    onSuccess = { createdSale ->
                        showCreateSaleDialog = false
                        selectedSaleForReceipt = createdSale
                    }
                )
            }
        )
    }

    selectedSaleForReceipt?.let { sale ->
        ReceiptDialog(
            sale = sale,
            onDismiss = { selectedSaleForReceipt = null },
            onShare = { text ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(intent, "Share Sale Receipt"))
            }
        )
    }

    saleToCancel?.let { sale ->
        AlertDialog(
            onDismissRequest = { saleToCancel = null },
            title = { Text("Cancel Sale ${sale.invoiceNumber}?", fontWeight = FontWeight.Bold) },
            text = { Text("Cancelling this sale will return the sold stock back to your inventory and adjust customer balances.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelSale(sale.id)
                        saleToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Yes, Cancel Sale", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { saleToCancel = null }) { Text("Close") }
            }
        )
    }
}

@Composable
fun SaleItemCard(
    sale: SaleEntity,
    onViewReceipt: () -> Unit,
    onCancel: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = sale.invoiceNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = sdf.format(Date(sale.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "₹${String.format(Locale.getDefault(), "%.2f", sale.totalAmount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Customer: ${sale.customerName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Paid: ₹${String.format(Locale.getDefault(), "%.2f", sale.paidAmount)} (${sale.paymentMode.name})",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusGreen
                )

                if (sale.dueAmount > 0) {
                    Text(
                        text = "Pending Due: ₹${String.format(Locale.getDefault(), "%.2f", sale.dueAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onViewReceipt,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Receipt", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Cancel", tint = StatusRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2. PURCHASES TAB CONTENT
// ---------------------------------------------------------------------------
@Composable
fun PurchasesTabContent(viewModel: ShopViewModel) {
    val purchases by viewModel.purchases.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()

    var showCreatePurchaseDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Purchase Invoices (${purchases.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val totalBuyAmt = purchases.sumOf { it.totalAmount }
                Text(
                    text = "Total Purchases: ₹${String.format(Locale.getDefault(), "%.2f", totalBuyAmt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = { showCreatePurchaseDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Record Purchase", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (purchases.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Purchases Recorded",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Record raw tobacco, bidi bundles, or cartons bought from suppliers to automatically increase your stock.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showCreatePurchaseDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("+ Record First Purchase")
                        }
                    }
                }
            }
        } else {
            val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(purchases, key = { it.id }) { purchase ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(purchase.invoiceNumber, fontWeight = FontWeight.Bold)
                                    Text(sdf.format(Date(purchase.purchaseDate)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    "₹${String.format(Locale.getDefault(), "%.2f", purchase.totalAmount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = EmeraldPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Supplier: ${purchase.supplierName}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Paid: ₹${String.format(Locale.getDefault(), "%.2f", purchase.paidAmount)}", fontSize = 12.sp, color = StatusGreen)
                                if (purchase.dueAmount > 0) {
                                    Text("Supplier Balance: ₹${String.format(Locale.getDefault(), "%.2f", purchase.dueAmount)}", fontSize = 12.sp, color = StatusRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePurchaseDialog) {
        CreatePurchaseDialog(
            products = products,
            suppliers = suppliers,
            onDismiss = { showCreatePurchaseDialog = false },
            onSave = { supplier, items, paid, mode, notes ->
                viewModel.processPurchase(
                    supplier = supplier,
                    items = items,
                    paidAmount = paid,
                    paymentMode = mode,
                    notes = notes,
                    onSuccess = { showCreatePurchaseDialog = false }
                )
            }
        )
    }
}

// ---------------------------------------------------------------------------
// 3. KHATA / UDHAR TAB CONTENT (CUSTOMERS & SUPPLIERS)
// ---------------------------------------------------------------------------
@Composable
fun KhataTabContent(viewModel: ShopViewModel) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var activeSubTab by remember { mutableStateOf("Customers") }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var customerForPayment by remember { mutableStateOf<CustomerEntity?>(null) }
    var supplierForPayment by remember { mutableStateOf<SupplierEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Toggle Between Customer Udhar and Supplier Payables
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeSubTab == "Customers",
                onClick = { activeSubTab = "Customers" },
                label = { Text("Customer Udhar (${customers.size})") },
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = activeSubTab == "Suppliers",
                onClick = { activeSubTab = "Suppliers" },
                label = { Text("Supplier Payables (${suppliers.size})") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeSubTab == "Customers") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val totalUdhar = customers.sumOf { it.currentDueBalance }
                Column {
                    Text("Total Pending Udhar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${String.format(Locale.getDefault(), "%.2f", totalUdhar)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = StatusRed)
                }

                Button(
                    onClick = { showAddCustomerDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Customer")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (customers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No customers added yet. Add regular buyers to record Udhar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(customers, key = { it.id }) { customer ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    if (customer.phone.isNotBlank()) {
                                        Text(customer.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        "Due: ₹${String.format(Locale.getDefault(), "%.2f", customer.currentDueBalance)}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (customer.currentDueBalance > 0) StatusRed else StatusGreen,
                                        fontSize = 13.sp
                                    )
                                }

                                Row {
                                    if (customer.phone.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                                                context.startActivity(intent)
                                            }
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = "Call", tint = EmeraldPrimary)
                                        }
                                    }

                                    Button(
                                        onClick = { customerForPayment = customer },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Receive ₹", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Suppliers view
            val totalPayables = suppliers.sumOf { it.currentPayableBalance }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Supplier Payables", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${String.format(Locale.getDefault(), "%.2f", totalPayables)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = StatusRed)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (suppliers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No suppliers recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suppliers, key = { it.id }) { supplier ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(supplier.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        "Payable: ₹${String.format(Locale.getDefault(), "%.2f", supplier.currentPayableBalance)}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (supplier.currentPayableBalance > 0) StatusRed else StatusGreen,
                                        fontSize = 13.sp
                                    )
                                }

                                Button(
                                    onClick = { supplierForPayment = supplier },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Pay ₹", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCustomerDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = { Text("Add Customer / Buyer", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Customer Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address / Village / Market") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.saveCustomer(
                                CustomerEntity(
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    address = address.trim()
                                )
                            )
                            showAddCustomerDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save Customer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) { Text("Cancel") }
            }
        )
    }

    customerForPayment?.let { customer ->
        var paymentAmtStr by remember { mutableStateOf("") }
        var paymentNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { customerForPayment = null },
            title = { Text("Receive Payment from ${customer.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Current Due: ₹${String.format(Locale.getDefault(), "%.2f", customer.currentDueBalance)}", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = paymentAmtStr,
                        onValueChange = { paymentAmtStr = it },
                        label = { Text("Amount Received (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = paymentNote,
                        onValueChange = { paymentNote = it },
                        label = { Text("Note (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = paymentAmtStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.recordCustomerPayment(
                                customer = customer,
                                amount = amt,
                                mode = PaymentMode.CASH,
                                ref = "",
                                note = paymentNote.ifBlank { "Cash payment collected" }
                            )
                            customerForPayment = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Record Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerForPayment = null }) { Text("Cancel") }
            }
        )
    }

    supplierForPayment?.let { supplier ->
        var payAmtStr by remember { mutableStateOf("") }
        var payNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { supplierForPayment = null },
            title = { Text("Pay Supplier: ${supplier.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Current Payable: ₹${String.format(Locale.getDefault(), "%.2f", supplier.currentPayableBalance)}", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = payAmtStr,
                        onValueChange = { payAmtStr = it },
                        label = { Text("Amount Paid (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = payNote,
                        onValueChange = { payNote = it },
                        label = { Text("Note (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = payAmtStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.recordSupplierPayment(supplier, amt, payNote.ifBlank { "Supplier payment made" })
                            supplierForPayment = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Record Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { supplierForPayment = null }) { Text("Cancel") }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// 4. EXPENSES TAB CONTENT
// ---------------------------------------------------------------------------
@Composable
fun ExpensesTabContent(viewModel: ShopViewModel) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val totalExpenses = expenses.sumOf { it.amount }
            Column {
                Text(
                    text = "Shop Expenses (${expenses.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total Expenses: ₹${String.format(Locale.getDefault(), "%.2f", totalExpenses)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusRed,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = { showAddExpenseDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Expense")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No expenses recorded yet. Tap '+ Add Expense' to log transport, thread, leaves, rent, or daily costs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(expenses, key = { it.id }) { expense ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(expense.category, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                if (expense.note.isNotBlank()) {
                                    Text(expense.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(sdf.format(Date(expense.expenseDate)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "₹${String.format(Locale.getDefault(), "%.2f", expense.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = StatusRed
                                )
                                IconButton(onClick = { viewModel.deleteExpense(expense.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusRed.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddExpenseDialog) {
        var category by remember { mutableStateOf("Bidi Leaves / Tobacco") }
        var amountStr by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }

        val commonCategories = listOf(
            "Bidi Leaves / Tobacco",
            "Packaging Thread & Paper",
            "Transport / Petrol",
            "Shop Rent",
            "Helper Wages",
            "Tea & Refreshments",
            "Other"
        )

        AlertDialog(
            onDismissRequest = { showAddExpenseDialog = false },
            title = { Text("Add Business Expense", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Category", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    commonCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.addExpense(
                                ExpenseEntity(
                                    category = category,
                                    amount = amt,
                                    note = note.trim()
                                )
                            )
                            showAddExpenseDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save Expense")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExpenseDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// DIALOGS & HELPERS (CREATE SALE, CREATE PURCHASE, RECEIPT)
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSaleDialog(
    products: List<ProductEntity>,
    customers: List<CustomerEntity>,
    onDismiss: () -> Unit,
    onSave: (
        customer: CustomerEntity?,
        items: List<Pair<ProductEntity, Double>>,
        discount: Double,
        paid: Double,
        mode: PaymentMode,
        notes: String
    ) -> Unit
) {
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerExpanded by remember { mutableStateOf(false) }

    val itemQuantities = remember { mutableStateMapOf<Long, Double>() }
    var discountStr by remember { mutableStateOf("") }
    var paidAmountStr by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var notes by remember { mutableStateOf("") }

    // Pre-calculate totals
    val subtotal = products.filter { (itemQuantities[it.id] ?: 0.0) > 0.0 }
        .sumOf { (itemQuantities[it.id] ?: 0.0) * it.sellingPrice }
    val discount = discountStr.toDoubleOrNull() ?: 0.0
    val total = (subtotal - discount).coerceAtLeast(0.0)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Record New Sale", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                // Customer Selection
                Text("Customer", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(
                    expanded = customerExpanded,
                    onExpandedChange = { customerExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.name ?: "Cash / Walk-in Customer",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = customerExpanded,
                        onDismissRequest = { customerExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Cash / Walk-in Customer") },
                            onClick = {
                                selectedCustomer = null
                                customerExpanded = false
                            }
                        )
                        customers.forEach { customer ->
                            DropdownMenuItem(
                                text = { Text("${customer.name} (Due: ₹${customer.currentDueBalance})") },
                                onClick = {
                                    selectedCustomer = customer
                                    customerExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Select Products & Quantities
                Text("Products to Sell", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                if (products.isEmpty()) {
                    Text("No products in catalog. Add products in the 'Products' tab first.", color = StatusRed, fontSize = 12.sp)
                } else {
                    products.forEach { product ->
                        val qty = itemQuantities[product.id] ?: 0.0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("₹${product.sellingPrice}/${product.unit} (Stock: ${product.currentStock})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (qty > 0) itemQuantities[product.id] = (qty - 1).coerceAtLeast(0.0)
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                }

                                Text(
                                    text = String.format(Locale.getDefault(), "%.0f", qty),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )

                                IconButton(
                                    onClick = {
                                        itemQuantities[product.id] = qty + 1
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Total Summary
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bill Total:")
                            Text("₹${String.format(Locale.getDefault(), "%.2f", total)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EmeraldPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Paid Amount
                OutlinedTextField(
                    value = paidAmountStr,
                    onValueChange = { paidAmountStr = it },
                    label = { Text("Amount Paid Now (₹)") },
                    placeholder = { Text("Enter ₹ or leave blank for full pay") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Payment Mode
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(PaymentMode.CASH, PaymentMode.UPI_ONLINE, PaymentMode.CREDIT_KHATA).forEach { mode ->
                        FilterChip(
                            selected = paymentMode == mode,
                            onClick = { paymentMode = mode },
                            label = { Text(if (mode == PaymentMode.CREDIT_KHATA) "UDHAR" else if (mode == PaymentMode.UPI_ONLINE) "UPI" else "CASH", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val selectedItems = products.filter { (itemQuantities[it.id] ?: 0.0) > 0.0 }
                                .map { it to (itemQuantities[it.id] ?: 0.0) }
                            if (selectedItems.isNotEmpty()) {
                                val paid = if (paidAmountStr.isBlank()) {
                                    if (paymentMode == PaymentMode.CREDIT_KHATA) 0.0 else total
                                } else {
                                    paidAmountStr.toDoubleOrNull() ?: 0.0
                                }
                                onSave(selectedCustomer, selectedItems, discount, paid, paymentMode, notes)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Save Sale", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePurchaseDialog(
    products: List<ProductEntity>,
    suppliers: List<SupplierEntity>,
    onDismiss: () -> Unit,
    onSave: (
        supplier: SupplierEntity?,
        items: List<Triple<ProductEntity, Double, Double>>,
        paid: Double,
        mode: PaymentMode,
        notes: String
    ) -> Unit
) {
    var supplierName by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var quantityStr by remember { mutableStateOf("10") }
    var unitCostStr by remember { mutableStateOf(selectedProduct?.purchaseCost?.toString() ?: "0") }
    var paidStr by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp).verticalScroll(rememberScrollState())
            ) {
                Text("Record Purchase", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text("Supplier Name") },
                    placeholder = { Text("e.g. National Bidi Works") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Select Product", style = MaterialTheme.typography.labelSmall)
                if (products.isEmpty()) {
                    Text("No products added yet. Add product first.", color = StatusRed, fontSize = 12.sp)
                } else {
                    products.forEach { prod ->
                        FilterChip(
                            selected = selectedProduct?.id == prod.id,
                            onClick = {
                                selectedProduct = prod
                                unitCostStr = prod.purchaseCost.toString()
                            },
                            label = { Text("${prod.name} (${prod.unit})", fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = unitCostStr,
                        onValueChange = { unitCostStr = it },
                        label = { Text("Cost Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                val qty = quantityStr.toDoubleOrNull() ?: 0.0
                val cost = unitCostStr.toDoubleOrNull() ?: 0.0
                val totalCost = qty * cost

                Spacer(modifier = Modifier.height(10.dp))
                Text("Total Cost: ₹${String.format(Locale.getDefault(), "%.2f", totalCost)}", fontWeight = FontWeight.Bold, color = EmeraldPrimary)

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = paidStr,
                    onValueChange = { paidStr = it },
                    label = { Text("Amount Paid Now (₹)") },
                    placeholder = { Text("Enter ₹ or leave blank for full") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val prod = selectedProduct
                            if (prod != null && qty > 0) {
                                val paid = if (paidStr.isBlank()) totalCost else (paidStr.toDoubleOrNull() ?: totalCost)
                                val supplierObj = if (supplierName.isNotBlank()) SupplierEntity(name = supplierName.trim(), phone = "") else null
                                onSave(
                                    supplierObj,
                                    listOf(Triple(prod, qty, cost)),
                                    paid,
                                    PaymentMode.CASH,
                                    "Purchase of ${prod.name}"
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Save Purchase")
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptDialog(
    sale: SaleEntity,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()) }

    val receiptText = remember(sale) {
        val sb = StringBuilder()
        sb.appendLine("===============================")
        sb.appendLine("       ASLAM RECORD")
        sb.appendLine("  Bidi Business Cash Memo")
        sb.appendLine("===============================")
        sb.appendLine("Invoice: ${sale.invoiceNumber}")
        sb.appendLine("Date: ${sdf.format(Date(sale.createdAt))}")
        sb.appendLine("Customer: ${sale.customerName}")
        sb.appendLine("-------------------------------")
        sb.appendLine("Total Bill: ₹${String.format(Locale.getDefault(), "%.2f", sale.totalAmount)}")
        sb.appendLine("Paid Amount: ₹${String.format(Locale.getDefault(), "%.2f", sale.paidAmount)} (${sale.paymentMode.name})")
        if (sale.dueAmount > 0) {
            sb.appendLine("Balance Due: ₹${String.format(Locale.getDefault(), "%.2f", sale.dueAmount)}")
        }
        sb.appendLine("-------------------------------")
        sb.appendLine("Thank you for your business!")
        sb.appendLine("===============================")
        sb.toString()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                Text("Sale Receipt", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = receiptText,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                    Button(
                        onClick = { onShare(receiptText) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share via WhatsApp / SMS")
                    }
                }
            }
        }
    }
}
