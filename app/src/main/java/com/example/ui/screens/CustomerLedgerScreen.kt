package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.theme.GeometricBorder
import com.example.ui.theme.GeometricMintAccent
import com.example.ui.theme.PharmaPrimaryLight
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer
import com.example.ui.viewmodel.LedgerEntry
import com.example.ui.viewmodel.LedgerEntryType
import com.example.ui.viewmodel.PharmaViewModel
import com.example.util.WhatsAppHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomerLedgerScreen(
    viewModel: PharmaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsState()
    val selectedCustomer by viewModel.selectedLedgerCustomer.collectAsState()
    val ledgerEntries by viewModel.customerLedgerEntries.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var transactionFilter by remember { mutableStateOf("ALL") } // ALL, SALES, PAYMENTS
    var isDataOnline by remember { mutableStateOf(WhatsAppHelper.isDataOrWifiOnline(context)) }

    // Auto-select first customer if none is selected
    LaunchedEffect(customers) {
        if (selectedCustomer == null && customers.isNotEmpty()) {
            val dueCust = customers.firstOrNull { it.outstandingBalance > 0 } ?: customers.first()
            viewModel.selectLedgerCustomer(dueCust)
        }
    }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else customers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.phone.contains(searchQuery, ignoreCase = true)
        }
    }

    val displayedEntries = remember(ledgerEntries, transactionFilter) {
        when (transactionFilter) {
            "SALES" -> ledgerEntries.filter { it.type == LedgerEntryType.SALE_INVOICE }
            "PAYMENTS" -> ledgerEntries.filter { it.type == LedgerEntryType.PAYMENT_RECEIVING }
            else -> ledgerEntries
        }
    }

    val totalInvoiced = remember(ledgerEntries) {
        ledgerEntries.filter { it.type == LedgerEntryType.SALE_INVOICE }.sumOf { it.totalInvoiceAmount }
    }
    val totalPaid = remember(ledgerEntries) {
        ledgerEntries.filter { it.type == LedgerEntryType.PAYMENT_RECEIVING }.sumOf { it.creditAmount }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("customer_ledger_screen"),
        floatingActionButton = {
            selectedCustomer?.let { cust ->
                FloatingActionButton(
                    onClick = { viewModel.openRecordRecovery(cust) },
                    containerColor = PharmaPrimaryLight,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("ledger_add_receiving_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Receiving")
                        Text("Add Receiving", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Top Customer Search & Selector Bar
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ledger_customer_search"),
                        placeholder = { Text("Search customer ledger (name, phone)...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Customer Selection Pills
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(filteredCustomers, key = { it.id }) { cust ->
                            val isSelected = selectedCustomer?.id == cust.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectLedgerCustomer(cust) },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = cust.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                        if (cust.outstandingBalance > 0) {
                                            Text(
                                                text = "$${cust.outstandingBalance.toInt()}",
                                                color = StatusDanger,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 2. Selected Customer Profile & Action Hub Card
            item {
                if (selectedCustomer != null) {
                    val cust = selectedCustomer!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, GeometricBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Name & Contact Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(GeometricMintAccent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cust.name.take(2).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = PharmaPrimaryLight,
                                            fontSize = 16.sp
                                        )
                                    }
                                    Column {
                                        Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(
                                            text = cust.phone.ifBlank { "No phone recorded" },
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (cust.outstandingBalance > 0) {
                                    StatusBadge(text = "${cust.daysOverdue}d Overdue", statusType = "DANGER")
                                } else {
                                    StatusBadge(text = "Settled", statusType = "SUCCESS")
                                }
                            }

                            if (cust.address.isNotBlank()) {
                                Text(
                                    text = "📍 ${cust.address}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HorizontalDivider(color = GeometricBorder)

                            // 4-Button Quick Action Toolbar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Direct WhatsApp Chat Button
                                Button(
                                    onClick = {
                                        val welcomeMsg = "Hello ${cust.name}, this is PharmaFlow Pharmacy. Here is an update on your account ledger."
                                        WhatsAppHelper.sendWhatsAppMessage(context, cust.phone, welcomeMsg)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("ledger_whatsapp_chat_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "WhatsApp", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // Send Statement Button
                                OutlinedButton(
                                    onClick = { viewModel.triggerWhatsAppStatementForCustomer(cust) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("ledger_send_statement_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Statement", modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Statement", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // Phone Call Button
                                IconButton(
                                    onClick = {
                                        if (cust.phone.isNotBlank()) {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cust.phone}"))
                                            context.startActivity(intent)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 3. Financial Net Balances (Geometric 3-card summary)
            item {
                selectedCustomer?.let { cust ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total Invoiced Card
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, GeometricBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Total Invoiced", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", totalInvoiced)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Total Paid / Received Card
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = StatusSuccessContainer.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Received", fontSize = 10.sp, color = StatusSuccess, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", totalPaid)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = StatusSuccess
                                )
                            }
                        }

                        // Current Outstanding Balance Card
                        val hasDue = cust.outstandingBalance > 0
                        Surface(
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(14.dp),
                            color = if (hasDue) StatusDangerContainer else StatusSuccessContainer,
                            border = BorderStroke(1.dp, if (hasDue) StatusDanger.copy(alpha = 0.3f) else StatusSuccess.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Balance Due", fontSize = 10.sp, color = if (hasDue) StatusDanger else StatusSuccess, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", cust.outstandingBalance)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = if (hasDue) StatusDanger else StatusSuccess
                                )
                            }
                        }
                    }
                }
            }

            // 4. WhatsApp & Data Connectivity Sync Banner
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isDataOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = "Data status",
                                tint = if (isDataOnline) StatusSuccess else StatusWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = if (isDataOnline) "Auto-WhatsApp Sync Active" else "Offline / Data Inactive",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "When mobile data or Wi-Fi is on, customer messages dispatch instantly.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                isDataOnline = WhatsAppHelper.isDataOrWifiOnline(context)
                                selectedCustomer?.let { cust ->
                                    viewModel.triggerWhatsAppStatementForCustomer(cust)
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync Data", tint = PharmaPrimaryLight, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // 5. Transaction Filter Tabs (All, Invoices, Payments)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL" to "All Ledger (${ledgerEntries.size})",
                        "SALES" to "Sales Invoices (${ledgerEntries.count { it.type == LedgerEntryType.SALE_INVOICE }})",
                        "PAYMENTS" to "Receiving Receipts (${ledgerEntries.count { it.type == LedgerEntryType.PAYMENT_RECEIVING }})"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = transactionFilter == key,
                            onClick = { transactionFilter = key },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (transactionFilter == key) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }

            // 6. Chronological Ledger Items
            if (displayedEntries.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        title = "No Ledger Transactions",
                        description = "No billing invoices or debt recovery payments recorded yet for this customer."
                    )
                }
            } else {
                items(displayedEntries, key = { "${it.type}_${it.id}" }) { entry ->
                    LedgerEntryCard(
                        entry = entry,
                        customer = selectedCustomer,
                        onSendWhatsAppReceipt = {
                            if (selectedCustomer != null && entry.rawPayment != null) {
                                viewModel.triggerWhatsAppReceiptForPayment(selectedCustomer!!, entry.rawPayment)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LedgerEntryCard(
    entry: LedgerEntry,
    customer: Customer?,
    onSendWhatsAppReceipt: () -> Unit
) {
    val isPayment = entry.type == LedgerEntryType.PAYMENT_RECEIVING
    val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(entry.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, GeometricBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Reference & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusBadge(
                        text = if (isPayment) "RECEIVING" else "INVOICE",
                        statusType = if (isPayment) "SUCCESS" else "DEFAULT"
                    )
                    Text(
                        text = "#${entry.reference}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = formattedDate,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Amounts Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = entry.notes,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    if (isPayment && entry.paymentMode.isNotBlank()) {
                        Text(
                            text = "Mode: ${entry.paymentMode.replace("_", " ")}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (!isPayment && entry.creditAmount > 0) {
                        Text(
                            text = "Paid at POS: $${String.format(Locale.US, "%.2f", entry.creditAmount)} • Billed: $${String.format(Locale.US, "%.2f", entry.totalInvoiceAmount)}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isPayment) "-$${String.format(Locale.US, "%.2f", entry.creditAmount)}"
                        else "+$${String.format(Locale.US, "%.2f", entry.debitAmount)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = if (isPayment) StatusSuccess else StatusDanger
                    )
                    Text(
                        text = "Balance: $${String.format(Locale.US, "%.2f", entry.runningBalance)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // WhatsApp Share Action for Payment Receipts
            if (isPayment && customer != null && customer.phone.isNotBlank()) {
                HorizontalDivider(color = GeometricBorder.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onSendWhatsAppReceipt,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Message,
                            contentDescription = "WhatsApp",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sync & Send on WhatsApp",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF075E54)
                        )
                    }
                }
            }
        }
    }
}
