package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.dialogs.AddMedicineDialog
import com.example.ui.dialogs.AddPurchaseDialog
import com.example.ui.dialogs.AddReturnDialog
import com.example.ui.dialogs.AddSaleDialog
import com.example.ui.dialogs.AiReminderDialog
import com.example.ui.dialogs.RecordRecoveryDialog
import com.example.ui.dialogs.SaleReceiptDialog
import com.example.ui.screens.AiCopilotScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.PurchasesScreen
import com.example.ui.screens.RecoveryScreen
import com.example.ui.screens.ReturnsScreen
import com.example.ui.screens.SalesScreen
import com.example.ui.theme.PharmaFlowTheme
import com.example.ui.viewmodel.PharmaTab
import com.example.ui.viewmodel.PharmaViewModel

data class NavItem(
    val tab: PharmaTab,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    private val viewModel: PharmaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PharmaFlowTheme {
                PharmaApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PharmaApp(viewModel: PharmaViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val userFeedback by viewModel.userFeedback.collectAsState()

    // Database states
    val medicines by viewModel.medicines.collectAsState()
    val batches by viewModel.batches.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val overdueCustomers by viewModel.overdueCustomers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    val recoveries by viewModel.recoveries.collectAsState()
    val returns by viewModel.returns.collectAsState()
    val expiringBatches by viewModel.expiringBatches.collectAsState()

    // Financial sums
    val salesSum by viewModel.totalSales.collectAsState()
    val purchasesSum by viewModel.totalPurchases.collectAsState()
    val outstandingRecovery by viewModel.totalOutstandingRecovery.collectAsState()
    val returnsSum by viewModel.totalReturns.collectAsState()

    // Dialog & Cart states
    val saleCart by viewModel.saleCart.collectAsState()
    val showAddSale by viewModel.showAddSaleDialog.collectAsState()
    val showAddPurchase by viewModel.showAddPurchaseDialog.collectAsState()
    val showRecordRecovery by viewModel.showRecordRecoveryDialog.collectAsState()
    val showAddReturn by viewModel.showAddReturnDialog.collectAsState()
    val showAddMedicine by viewModel.showAddMedicineDialog.collectAsState()
    val selectedCustomerForRecovery by viewModel.selectedCustomerForRecovery.collectAsState()
    val selectedSaleForReceipt by viewModel.selectedSaleForReceipt.collectAsState()

    // AI states
    val aiReminderText by viewModel.aiReminderText.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiChatMessages by viewModel.aiChatMessages.collectAsState()

    LaunchedEffect(userFeedback) {
        userFeedback?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    val navItems = listOf(
        NavItem(PharmaTab.DASHBOARD, "Home", Icons.Default.Dashboard, "nav_dashboard"),
        NavItem(PharmaTab.SALES, "Sales", Icons.Default.ShoppingBag, "nav_sales"),
        NavItem(PharmaTab.PURCHASES, "Purchase", Icons.Default.AddShoppingCart, "nav_purchases"),
        NavItem(PharmaTab.RECOVERY, "Recovery", Icons.Default.Payments, "nav_recovery"),
        NavItem(PharmaTab.RETURNS, "Returns", Icons.AutoMirrored.Filled.AssignmentReturn, "nav_returns"),
        NavItem(PharmaTab.INVENTORY, "Medicines", Icons.Default.Medication, "nav_inventory"),
        NavItem(PharmaTab.AI_COPILOT, "AI Copilot", Icons.Default.AutoAwesome, "nav_ai_copilot")
    )

    val configuration = LocalConfiguration.current
    val isTabletOrLandscape = configuration.screenWidthDp >= 600

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentTab) {
                            PharmaTab.DASHBOARD -> "PharmaFlow AI"
                            PharmaTab.SALES -> "Sales & Invoices"
                            PharmaTab.PURCHASES -> "Stock Inward & Purchases"
                            PharmaTab.RECOVERY -> "Accounts Receivable & Recovery"
                            PharmaTab.RETURNS -> "Returns & FEFO Expiry Radar"
                            PharmaTab.INVENTORY -> "Medicine Formulary"
                            PharmaTab.AI_COPILOT -> "AI Pharmacy Copilot"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            if (!isTabletOrLandscape) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentTab == item.tab,
                            onClick = { viewModel.setTab(item.tab) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, fontSize = 10.sp, fontWeight = if (currentTab == item.tab) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isTabletOrLandscape) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("nav_rail")
                ) {
                    navItems.forEach { item ->
                        NavigationRailItem(
                            selected = currentTab == item.tab,
                            onClick = { viewModel.setTab(item.tab) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, fontSize = 10.sp) },
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (currentTab) {
                    PharmaTab.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        salesSum = salesSum,
                        purchasesSum = purchasesSum,
                        outstandingRecovery = outstandingRecovery,
                        returnsSum = returnsSum,
                        expiringBatches = expiringBatches,
                        overdueCustomers = overdueCustomers,
                        recentSales = sales
                    )
                    PharmaTab.SALES -> SalesScreen(
                        viewModel = viewModel,
                        sales = sales
                    )
                    PharmaTab.PURCHASES -> PurchasesScreen(
                        viewModel = viewModel,
                        purchases = purchases
                    )
                    PharmaTab.RECOVERY -> RecoveryScreen(
                        viewModel = viewModel,
                        customers = customers,
                        recoveries = recoveries
                    )
                    PharmaTab.RETURNS -> ReturnsScreen(
                        viewModel = viewModel,
                        batches = batches,
                        medicines = medicines,
                        returns = returns
                    )
                    PharmaTab.INVENTORY -> InventoryScreen(
                        viewModel = viewModel,
                        medicines = medicines,
                        batches = batches
                    )
                    PharmaTab.AI_COPILOT -> AiCopilotScreen(
                        viewModel = viewModel,
                        chatMessages = aiChatMessages,
                        isLoading = isAiLoading
                    )
                }
            }
        }
    }

    // --- Active Dialogs ---
    if (showAddSale) {
        AddSaleDialog(
            viewModel = viewModel,
            medicines = medicines,
            batches = batches,
            customers = customers,
            cartItems = saleCart,
            onDismiss = { viewModel.closeAddSale() }
        )
    }

    if (showAddPurchase) {
        AddPurchaseDialog(
            viewModel = viewModel,
            suppliers = suppliers,
            medicines = medicines,
            onDismiss = { viewModel.closeAddPurchase() }
        )
    }

    if (showRecordRecovery) {
        RecordRecoveryDialog(
            viewModel = viewModel,
            customers = customers,
            initialCustomer = selectedCustomerForRecovery,
            onDismiss = { viewModel.closeRecordRecovery() }
        )
    }

    if (showAddReturn) {
        AddReturnDialog(
            viewModel = viewModel,
            medicines = medicines,
            batches = batches,
            customers = customers,
            suppliers = suppliers,
            onDismiss = { viewModel.closeAddReturn() }
        )
    }

    if (showAddMedicine) {
        AddMedicineDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeAddMedicine() }
        )
    }

    selectedSaleForReceipt?.let { sale ->
        SaleReceiptDialog(
            sale = sale,
            onDismiss = { viewModel.closeSaleReceipt() }
        )
    }

    aiReminderText?.let { text ->
        selectedCustomerForRecovery?.let { cust ->
            AiReminderDialog(
                reminderText = text,
                customer = cust,
                isLoading = isAiLoading,
                viewModel = viewModel,
                onDismiss = { viewModel.clearAiReminder() }
            )
        } ?: run {
            val overdueCust = overdueCustomers.firstOrNull() ?: customers.firstOrNull()
            if (overdueCust != null) {
                AiReminderDialog(
                    reminderText = text,
                    customer = overdueCust,
                    isLoading = isAiLoading,
                    viewModel = viewModel,
                    onDismiss = { viewModel.clearAiReminder() }
                )
            }
        }
    }
}
