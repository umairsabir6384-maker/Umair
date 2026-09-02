package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Batch
import com.example.data.model.Medicine
import com.example.data.model.ReturnTransaction
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
fun ReturnsScreen(
    viewModel: PharmaViewModel,
    batches: List<Batch>,
    medicines: List<Medicine>,
    returns: List<ReturnTransaction>,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf(0) } // 0: FEFO Expiry Radar, 1: Return Vouchers

    val nowEpoch = remember { System.currentTimeMillis() }
    val expiredBatches = remember(batches) { batches.filter { it.expiryDateEpochMs <= nowEpoch && it.currentStock > 0 } }
    val expiringBatches = remember(batches) {
        val next60Days = nowEpoch + (60L * 86400000L)
        batches.filter { it.expiryDateEpochMs in (nowEpoch + 1)..next60Days && it.currentStock > 0 }
            .sortedBy { it.expiryDateEpochMs }
    }

    val totalReturnVal = remember(returns) { returns.sumOf { it.totalRefundAmount } }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("returns_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddReturn() },
                containerColor = StatusDanger,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_return_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Process Return")
                    Text("Process Return", fontWeight = FontWeight.Bold)
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
            // Sub-tabs
            TabRow(
                selectedTabIndex = activeSubTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                divider = {}
            ) {
                Tab(
                    selected = activeSubTab == 0,
                    onClick = { activeSubTab = 0 },
                    text = {
                        Text(
                            "FEFO Expiry Radar (${expiredBatches.size + expiringBatches.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                )
                Tab(
                    selected = activeSubTab == 1,
                    onClick = { activeSubTab = 1 },
                    text = { Text("Processed Vouchers (${returns.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            if (activeSubTab == 0) {
                // Header Alert
                Card(
                    colors = CardDefaults.cardColors(containerColor = StatusDangerContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StatusDanger.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FEFO Expiration Risk Monitor",
                                fontWeight = FontWeight.Bold,
                                color = StatusDanger,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Return expired lots for 100% vendor credit or prioritize near-expiry stock.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (expiredBatches.isEmpty() && expiringBatches.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.HourglassBottom,
                        title = "Zero Expiry Risk!",
                        description = "All current pharmacy batches have healthy shelf life (>60 days)."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 72.dp)
                    ) {
                        if (expiredBatches.isNotEmpty()) {
                            item {
                                Text(
                                    "🔴 EXPIRED BATCHES (Immediate Action Required)",
                                    color = StatusDanger,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            items(expiredBatches, key = { it.id }) { b ->
                                ExpiryBatchActionCard(
                                    batch = b,
                                    isExpired = true,
                                    onReturnClaim = {
                                        viewModel.openAddReturn()
                                    }
                                )
                            }
                        }

                        if (expiringBatches.isNotEmpty()) {
                            item {
                                Text(
                                    "🟠 NEAR EXPIRY BATCHES (< 60 Days)",
                                    color = StatusWarning,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            items(expiringBatches, key = { it.id }) { b ->
                                ExpiryBatchActionCard(
                                    batch = b,
                                    isExpired = false,
                                    onReturnClaim = {
                                        viewModel.openAddReturn()
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // Return Vouchers History
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
                        Text("Total Return / Claim Value:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("$${String.format("%.2f", totalReturnVal)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                if (returns.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.AutoMirrored.Filled.AssignmentReturn,
                        title = "No Return Vouchers",
                        description = "Return transactions for expired items or customer exchanges will appear here."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 72.dp)
                    ) {
                        items(returns, key = { it.id }) { ret ->
                            ReturnVoucherCard(returnTx = ret)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpiryBatchActionCard(
    batch: Batch,
    isExpired: Boolean,
    onReturnClaim: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expiry_batch_${batch.batchNumber}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired) StatusDangerContainer.copy(alpha = 0.6f) else StatusWarningContainer.copy(alpha = 0.6f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isExpired) StatusDanger.copy(alpha = 0.3f) else StatusWarning.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(batch.medicineName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Batch: ${batch.batchNumber}  •  Distributor: ${batch.supplierName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusBadge(
                    text = if (isExpired) "EXPIRED" else "NEAR EXPIRY",
                    statusType = if (isExpired) "DANGER" else "WARNING"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Expiry Date: ${batch.expiryDateFormatted}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = if (isExpired) StatusDanger else StatusWarning)
                    Text("Stock Remaining: ${batch.currentStock} units", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    val stockValue = batch.currentStock * batch.purchasePrice
                    Text("Stock Value: $${String.format("%.2f", stockValue)}", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
            }

            HorizontalDivider(color = if (isExpired) StatusDanger.copy(alpha = 0.2f) else StatusWarning.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onReturnClaim,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isExpired) StatusDanger else StatusWarning),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.AssignmentReturn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isExpired) "Supplier Debit Claim" else "Return / Discount", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReturnVoucherCard(returnTx: ReturnTransaction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("return_card_${returnTx.returnNumber}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.GeometricBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(returnTx.returnNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Text(
                        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(returnTx.timestamp)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(text = returnTx.returnType.replace("_", " "), statusType = "WARNING")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Party: ${returnTx.partyName}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text("Reason: ${returnTx.reason.replace("_", " ")}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = "$${String.format("%.2f", returnTx.totalRefundAmount)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = StatusDanger
                )
            }
        }
    }
}
