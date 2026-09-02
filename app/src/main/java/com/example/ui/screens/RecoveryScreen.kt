package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.model.RecoveryPayment
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer
import com.example.ui.viewmodel.PharmaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecoveryScreen(
    viewModel: PharmaViewModel,
    customers: List<Customer>,
    recoveries: List<RecoveryPayment>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeSubTab by remember { mutableStateOf(0) } // 0: Outstanding Debts, 1: Recovery Receipts History
    var agingFilter by remember { mutableStateOf("ALL") } // ALL, 0-30, 31-60, 60+
    var searchQuery by remember { mutableStateOf("") }

    val customersWithDues = remember(customers, searchQuery, agingFilter) {
        customers.filter { cust ->
            val hasDue = cust.outstandingBalance > 0
            val matchesQuery = searchQuery.isBlank() ||
                    cust.name.contains(searchQuery, ignoreCase = true) ||
                    cust.phone.contains(searchQuery, ignoreCase = true)

            val matchesAging = when (agingFilter) {
                "0-30" -> cust.daysOverdue <= 30
                "31-60" -> cust.daysOverdue in 31..60
                "60+" -> cust.daysOverdue > 60
                else -> true
            }

            hasDue && matchesQuery && matchesAging
        }.sortedByDescending { it.daysOverdue }
    }

    val totalOutstanding = remember(customersWithDues) { customersWithDues.sumOf { it.outstandingBalance } }
    val totalRecoveredHistory = remember(recoveries) { recoveries.sumOf { it.amountPaid } }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("recovery_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openRecordRecovery() },
                containerColor = StatusWarning,
                contentColor = Color.White,
                modifier = Modifier.testTag("record_recovery_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Record Payment")
                    Text("Collect Recovery", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Main Sub-tabs: Receivables vs Payment History
            TabRow(
                selectedTabIndex = activeSubTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                divider = {}
            ) {
                Tab(
                    selected = activeSubTab == 0,
                    onClick = { activeSubTab = 0 },
                    text = { Text("Outstanding Debts (${customers.count { it.outstandingBalance > 0 }})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = activeSubTab == 1,
                    onClick = { activeSubTab = 1 },
                    text = { Text("Recovery Receipts (${recoveries.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            if (activeSubTab == 0) {
                // Search box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().testTag("recovery_search_field"),
                    placeholder = { Text("Search client name, phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Aging Filter Tabs (0-30d, 31-60d, 60+d)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        listOf(
                            "ALL" to "All Dues",
                            "0-30" to "0–30 Days (Current)",
                            "31-60" to "31–60 Days (Moderate)",
                            "60+" to "60+ Days (🔴 High Risk)"
                        )
                    ) { (key, label) ->
                        val isSelected = agingFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { agingFilter = key },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (key == "60+") FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                // Summary Total Header
                Surface(
                    color = StatusWarningContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Pending Recovery:", fontSize = 12.sp, color = StatusWarning, fontWeight = FontWeight.SemiBold)
                            Text("${customersWithDues.size} Accounts Overdue", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = "$${String.format("%.2f", totalOutstanding)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = StatusWarning
                        )
                    }
                }

                if (customersWithDues.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Payments,
                        title = "All Accounts Settled!",
                        description = "No customers currently have overdue payment balances."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 72.dp)
                    ) {
                        items(customersWithDues, key = { it.id }) { cust ->
                            CustomerDebtCard(
                                customer = cust,
                                onRecordPayment = { viewModel.openRecordRecovery(cust) },
                                onAiReminder = { viewModel.generateAiRecoveryReminder(cust, "Polite") },
                                onCall = {
                                    if (cust.phone.isNotBlank()) {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cust.phone}"))
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // Recovery Payment History
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Amount Recovered:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("$${String.format("%.2f", totalRecoveredHistory)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                if (recoveries.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        title = "No Recoveries Recorded",
                        description = "When you collect payment dues from customers, receipts appear here."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 72.dp)
                    ) {
                        items(recoveries, key = { it.id }) { rec ->
                            RecoveryReceiptCard(recovery = rec)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerDebtCard(
    customer: Customer,
    onRecordPayment: () -> Unit,
    onAiReminder: () -> Unit,
    onCall: () -> Unit
) {
    val riskColor = when {
        customer.daysOverdue > 60 -> StatusDanger
        customer.daysOverdue > 30 -> StatusWarning
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("debt_card_${customer.name}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.GeometricBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (customer.phone.isNotBlank()) {
                        Text(customer.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${String.format("%.2f", customer.outstandingBalance)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = riskColor
                    )
                    Text(
                        text = "${customer.daysOverdue} Days Overdue",
                        color = riskColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            if (customer.address.isNotBlank()) {
                Text("Address: ${customer.address}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider(color = com.example.ui.theme.GeometricBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AI Reminder Button
                Button(
                    onClick = onAiReminder,
                    modifier = Modifier.weight(1.3f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Reminder", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Record Payment Button
                OutlinedButton(
                    onClick = onRecordPayment,
                    modifier = Modifier.weight(1.2f).height(40.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Collect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Quick Call
                IconButton(
                    onClick = onCall,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call Client", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun RecoveryReceiptCard(recovery: RecoveryPayment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.GeometricBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(recovery.receiptNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    StatusBadge(text = recovery.paymentMode, statusType = "SUCCESS")
                }
                Text("Received from: ${recovery.customerName}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text(
                    text = "Ref: ${recovery.referenceNumber.ifBlank { "N/A" }} • ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(recovery.timestamp))}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "+$${String.format("%.2f", recovery.amountPaid)}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = StatusSuccess
            )
        }
    }
}
