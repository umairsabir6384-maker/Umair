package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Batch
import com.example.data.model.Customer
import com.example.data.model.SaleTransaction
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.GeometricBorder
import com.example.ui.theme.GeometricDarkCard
import com.example.ui.theme.GeometricMintAccent
import com.example.ui.theme.PharmaPrimaryLight
import com.example.ui.theme.PharmaSecondaryLight
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.ui.theme.StatusGold
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer
import com.example.ui.viewmodel.PharmaTab
import com.example.ui.viewmodel.PharmaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: PharmaViewModel,
    salesSum: Double,
    customersCount: Int,
    outstandingRecovery: Double,
    returnsSum: Double,
    expiringBatches: List<Batch>,
    overdueCustomers: List<Customer>,
    recentSales: List<SaleTransaction>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Geometric Balance Top Branding Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PharmaPrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalPharmacy,
                            contentDescription = "Pharma Logo",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "PharmaFlow AI",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.US).format(Date()),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, GeometricBorder, CircleShape)
                        .clickable { viewModel.setTab(PharmaTab.AI_COPILOT) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Account",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Geometric Balance 2x2 Metric Cards Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Daily Sales",
                        amount = "$${String.format("%.2f", salesSum)}",
                        subtitle = "Billed revenue",
                        icon = Icons.Default.ShoppingCart,
                        containerColor = StatusSuccessContainer,
                        contentColor = StatusSuccess,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setTab(PharmaTab.SALES) }
                    )
                    StatCard(
                        title = "Pending Recovery",
                        amount = "$${String.format("%.2f", outstandingRecovery)}",
                        subtitle = "${overdueCustomers.size} accounts due",
                        icon = Icons.Default.Payments,
                        containerColor = StatusWarningContainer,
                        contentColor = StatusWarning,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setTab(PharmaTab.RECOVERY) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Customer Ledger",
                        amount = "$customersCount Accounts",
                        subtitle = "WhatsApp & Statements",
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        contentColor = PharmaSecondaryLight,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setTab(PharmaTab.LEDGER) }
                    )
                    StatCard(
                        title = "Returns / Claims",
                        amount = "$${String.format("%.2f", returnsSum)}",
                        subtitle = "${expiringBatches.size} lots at risk",
                        icon = Icons.AutoMirrored.Filled.AssignmentReturn,
                        containerColor = StatusDangerContainer,
                        contentColor = StatusDanger,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setTab(PharmaTab.RETURNS) }
                    )
                }
            }
        }

        // Geometric Balance 2x2 Action Navigation Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GeometricNavCard(
                        title = "New Sale",
                        icon = Icons.Default.ShoppingCart,
                        iconBackground = PharmaPrimaryLight,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.openAddSale() }
                    )
                    GeometricNavCard(
                        title = "Ledger & Sync",
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        iconBackground = PharmaSecondaryLight,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setTab(PharmaTab.LEDGER) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GeometricNavCard(
                        title = "Recovery",
                        icon = Icons.Default.Payments,
                        iconBackground = StatusGold,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.openRecordRecovery() }
                    )
                    GeometricNavCard(
                        title = "Returns",
                        icon = Icons.AutoMirrored.Filled.AssignmentReturn,
                        iconBackground = StatusDanger,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.openAddReturn() }
                    )
                }
            }
        }

        // Geometric Balance Dark AI Insight Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setTab(PharmaTab.AI_COPILOT) },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = GeometricDarkCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PharmaPrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI INSIGHT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GeometricMintAccent,
                            letterSpacing = 1.sp
                        )
                        val insightText = when {
                            expiringBatches.isNotEmpty() -> "${expiringBatches.size} medicines nearing expiry. Recall ready."
                            overdueCustomers.isNotEmpty() -> "${overdueCustomers.size} overdue accounts ready for AI recovery notices."
                            else -> "FEFO stock balanced. Formulary optimization active."
                        }
                        Text(
                            text = insightText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open AI Copilot",
                        tint = GeometricMintAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // FEFO Expiry Radar Warning Banner
        if (expiringBatches.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setTab(PharmaTab.RETURNS) },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusWarningContainer),
                    border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(StatusWarning.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FEFO Expiry Radar: ${expiringBatches.size} Batches at Risk",
                                fontWeight = FontWeight.Bold,
                                color = StatusWarning,
                                fontSize = 13.sp
                            )
                            val topExp = expiringBatches.firstOrNull()
                            Text(
                                text = if (topExp != null) "Lot ${topExp.batchNumber} (${topExp.medicineName}) expires on ${topExp.expiryDateFormatted}." else "Action needed to return or prioritize.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Overdue Debt Recovery Warning Banner
        if (overdueCustomers.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setTab(PharmaTab.RECOVERY) },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusDangerContainer),
                    border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(StatusDanger.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Overdue Recoveries: $${String.format("%.2f", outstandingRecovery)}",
                                fontWeight = FontWeight.Bold,
                                color = StatusDanger,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${overdueCustomers.size} customers past credit period. Tap to send AI payment reminders.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Recent Sales Invoices
        item {
            SectionHeader(
                title = "Recent Sales Invoices",
                actionText = "View All",
                onActionClick = { viewModel.setTab(PharmaTab.SALES) }
            )
        }

        if (recentSales.isEmpty()) {
            item {
                Text(
                    "No sales recorded yet. Click 'New Sale' to create your first bill.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(recentSales.take(5)) { sale ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openSaleReceipt(sale) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, GeometricBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(sale.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                StatusBadge(text = sale.paymentStatus, statusType = sale.paymentStatus)
                            }
                            Text(
                                text = "${sale.customerName} • ${SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(sale.timestamp))}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$${String.format("%.2f", sale.netPayable)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = PharmaPrimaryLight
                            )
                            if (sale.balanceDue > 0) {
                                Text("Due: $${String.format("%.2f", sale.balanceDue)}", color = StatusDanger, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GeometricNavCard(
    title: String,
    icon: ImageVector,
    iconBackground: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .testTag("action_${title.lowercase().replace(" ", "_")}")
            .clickable { onClick() },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, GeometricBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
