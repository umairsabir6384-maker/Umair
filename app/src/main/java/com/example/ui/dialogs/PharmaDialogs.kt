package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Batch
import com.example.data.model.Customer
import com.example.data.model.Medicine
import com.example.data.model.SaleTransaction
import com.example.data.model.Supplier
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.CartItem
import com.example.ui.viewmodel.PharmaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddSaleDialog(
    viewModel: PharmaViewModel,
    medicines: List<Medicine>,
    batches: List<Batch>,
    customers: List<Customer>,
    cartItems: List<CartItem>,
    onDismiss: () -> Unit
) {
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerNameText by remember { mutableStateOf("") }
    var customerPhoneText by remember { mutableStateOf("") }

    var selectedMedicine by remember { mutableStateOf<Medicine?>(null) }
    var selectedBatch by remember { mutableStateOf<Batch?>(null) }
    var itemQuantityText by remember { mutableStateOf("1") }

    var discountPercentText by remember { mutableStateOf("0") }
    var taxPercentText by remember { mutableStateOf("0") }
    var paidAmountText by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("CASH") } // CASH, CREDIT, CARD, UPI
    var saleNotes by remember { mutableStateOf("") }

    val subtotal = cartItems.sumOf { it.totalPrice }
    val discPct = discountPercentText.toDoubleOrNull() ?: 0.0
    val discAmount = subtotal * (discPct / 100.0)
    val afterDisc = subtotal - discAmount
    val taxPct = taxPercentText.toDoubleOrNull() ?: 0.0
    val taxAmount = afterDisc * (taxPct / 100.0)
    val grandTotal = afterDisc + taxAmount

    val availableBatchesForMed = remember(selectedMedicine, batches) {
        if (selectedMedicine == null) emptyList()
        else batches.filter { it.medicineId == selectedMedicine!!.id && it.currentStock > 0 }
            .sortedBy { it.expiryDateEpochMs } // FEFO rule
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "New Sale Invoice (POS)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "FEFO auto-suggested dispensing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_sale_dialog")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Customer Selection
                    Text(
                        text = "1. Customer / Account",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Customer selector pills
                    ScrollableTabRow(
                        selectedTabIndex = if (selectedCustomer == null) 0 else (customers.indexOf(selectedCustomer) + 1).coerceAtLeast(0),
                        edgePadding = 0.dp,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedCustomer == null,
                            onClick = { selectedCustomer = null },
                            text = { Text("Walk-in Customer", fontSize = 12.sp) }
                        )
                        customers.forEach { cust ->
                            Tab(
                                selected = selectedCustomer?.id == cust.id,
                                onClick = {
                                    selectedCustomer = cust
                                    customerNameText = cust.name
                                    customerPhoneText = cust.phone
                                },
                                text = {
                                    Text(
                                        text = "${cust.name}${if (cust.outstandingBalance > 0) " ($${cust.outstandingBalance.toInt()} Due)" else ""}",
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }
                    }

                    if (selectedCustomer == null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customerNameText,
                                onValueChange = { customerNameText = it },
                                label = { Text("Walk-in Name (Optional)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = customerPhoneText,
                                onValueChange = { customerPhoneText = it },
                                label = { Text("Phone (Optional)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                        }
                    }

                    // 2. Add Medicine to Cart
                    Text(
                        text = "2. Select Medicine & FEFO Batch",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Medicine Selector Scrollable row
                    Text("Choose Medicine:", style = MaterialTheme.typography.labelSmall)
                    ScrollableTabRow(
                        selectedTabIndex = if (selectedMedicine == null) -1 else medicines.indexOf(selectedMedicine).coerceAtLeast(0),
                        edgePadding = 0.dp,
                        divider = {}
                    ) {
                        medicines.forEach { med ->
                            Tab(
                                selected = selectedMedicine?.id == med.id,
                                onClick = {
                                    selectedMedicine = med
                                    val medBatches = batches.filter { it.medicineId == med.id && it.currentStock > 0 }.sortedBy { it.expiryDateEpochMs }
                                    selectedBatch = medBatches.firstOrNull()
                                },
                                text = { Text(med.name, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                            )
                        }
                    }

                    if (selectedMedicine != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "${selectedMedicine!!.name} • Salt: ${selectedMedicine!!.genericFormula}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )

                                Text("Select Batch (FEFO Sorted):", style = MaterialTheme.typography.labelSmall)
                                if (availableBatchesForMed.isEmpty()) {
                                    Text("⚠️ Out of Stock! Please inward stock in Purchases tab.", color = StatusDanger, fontSize = 12.sp)
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        availableBatchesForMed.forEach { b ->
                                            val isSelected = selectedBatch?.id == b.id
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { selectedBatch = b },
                                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text("Lot: ${b.batchNumber}  •  Exp: ${b.expiryDateFormatted}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        Text("Stock Available: ${b.currentStock} units", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                                                    }
                                                    Text("$${String.format("%.2f", b.salePrice)}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = itemQuantityText,
                                            onValueChange = { itemQuantityText = it },
                                            label = { Text("Quantity") },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )

                                        Button(
                                            onClick = {
                                                val qty = itemQuantityText.toIntOrNull() ?: 1
                                                if (selectedBatch != null && qty > 0) {
                                                    viewModel.addToCart(selectedMedicine!!, selectedBatch!!, qty)
                                                    itemQuantityText = "1"
                                                }
                                            },
                                            modifier = Modifier.weight(1f).height(54.dp),
                                            enabled = selectedBatch != null && (selectedBatch?.currentStock ?: 0) > 0
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Add To Cart")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Cart Items List
                    Text(
                        text = "3. Bill Cart Items (${cartItems.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (cartItems.isEmpty()) {
                        Text("No medicines added to bill yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            cartItems.forEach { item ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.medicine.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Batch ${item.batch.batchNumber} • Exp: ${item.batch.expiryDateFormatted}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${item.quantity} x $${String.format("%.2f", item.unitPrice)} = $${String.format("%.2f", item.totalPrice)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { viewModel.updateCartQuantity(item.batch.id, item.quantity - 1) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                            }
                                            Text("${item.quantity}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
                                            IconButton(
                                                onClick = { viewModel.updateCartQuantity(item.batch.id, item.quantity + 1) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = { viewModel.removeFromCart(item.batch.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = StatusDanger, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Pricing & Payment Mode
                    Text(
                        text = "4. Discounts, Taxes & Settlement",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = discountPercentText,
                            onValueChange = { discountPercentText = it },
                            label = { Text("Discount %") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = taxPercentText,
                            onValueChange = { taxPercentText = it },
                            label = { Text("Tax/GST %") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    // Payment Mode Pills
                    Text("Payment Method:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("CASH", "CREDIT", "CARD", "UPI").forEach { mode ->
                            val isSel = paymentMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        paymentMode = mode
                                        if (mode == "CASH" || mode == "CARD" || mode == "UPI") {
                                            paidAmountText = String.format("%.2f", grandTotal)
                                        } else {
                                            paidAmountText = "0.00"
                                        }
                                    },
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = mode,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = paidAmountText,
                        onValueChange = { paidAmountText = it },
                        label = { Text("Paid Amount Now ($)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = saleNotes,
                        onValueChange = { saleNotes = it },
                        label = { Text("Notes / Doctor Prescription Ref") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Grand Total Summary Box
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal:", fontSize = 12.sp)
                                Text("$${String.format("%.2f", subtotal)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if (discPct > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Discount ($discPct%):", fontSize = 12.sp, color = StatusSuccess)
                                    Text("-$${String.format("%.2f", discAmount)}", fontSize = 12.sp, color = StatusSuccess, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (taxPct > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Tax ($taxPct%):", fontSize = 12.sp)
                                    Text("+$${String.format("%.2f", taxAmount)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Grand Total:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("$${String.format("%.2f", grandTotal)}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            val paid = paidAmountText.toDoubleOrNull() ?: if (paymentMode == "CREDIT") 0.0 else grandTotal
                            val bal = maxOf(0.0, grandTotal - paid)
                            if (bal > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Balance Due (Credit):", fontSize = 12.sp, color = StatusDanger, fontWeight = FontWeight.Bold)
                                    Text("$${String.format("%.2f", bal)}", fontSize = 12.sp, color = StatusDanger, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val paid = paidAmountText.toDoubleOrNull() ?: if (paymentMode == "CREDIT") 0.0 else grandTotal
                            viewModel.submitSale(
                                customer = selectedCustomer,
                                customerName = customerNameText,
                                customerPhone = customerPhoneText,
                                discountPercent = discPct,
                                taxPercent = taxPct,
                                paidAmount = paid,
                                paymentMode = paymentMode,
                                notes = saleNotes
                            )
                        },
                        modifier = Modifier.weight(1.5f).height(48.dp).testTag("confirm_sale_button"),
                        enabled = cartItems.isNotEmpty()
                    ) {
                        Text("Generate Invoice", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddPurchaseDialog(
    viewModel: PharmaViewModel,
    suppliers: List<Supplier>,
    medicines: List<Medicine>,
    onDismiss: () -> Unit
) {
    var selectedSupplier by remember { mutableStateOf(suppliers.firstOrNull()) }
    var customSupplierName by remember { mutableStateOf("") }
    var selectedMedicine by remember { mutableStateOf(medicines.firstOrNull()) }

    var batchNumberText by remember { mutableStateOf("BCH-" + (1000..9999).random()) }
    var expiryFormattedText by remember { mutableStateOf("2027-06-30") }
    var purchaseRateText by remember { mutableStateOf("12.50") }
    var mrpText by remember { mutableStateOf("18.00") }
    var quantityText by remember { mutableStateOf("50") }
    var paidAmountText by remember { mutableStateOf("625.00") }
    var purchaseNotes by remember { mutableStateOf("") }

    val rate = purchaseRateText.toDoubleOrNull() ?: 0.0
    val qty = quantityText.toIntOrNull() ?: 0
    val totalCost = rate * qty

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Record Stock Inward (Purchase)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Inward medicine stock from distributor & supplier",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                // Supplier Selector
                Text("Select Distributor / Supplier:", style = MaterialTheme.typography.labelSmall)
                ScrollableTabRow(
                    selectedTabIndex = if (selectedSupplier == null) -1 else suppliers.indexOf(selectedSupplier).coerceAtLeast(0),
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    suppliers.forEach { sup ->
                        Tab(
                            selected = selectedSupplier?.id == sup.id,
                            onClick = { selectedSupplier = sup },
                            text = { Text(sup.name, fontSize = 12.sp) }
                        )
                    }
                }

                // Medicine Selector
                Text("Select Medicine:", style = MaterialTheme.typography.labelSmall)
                ScrollableTabRow(
                    selectedTabIndex = if (selectedMedicine == null) -1 else medicines.indexOf(selectedMedicine).coerceAtLeast(0),
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    medicines.forEach { med ->
                        Tab(
                            selected = selectedMedicine?.id == med.id,
                            onClick = { selectedMedicine = med },
                            text = { Text(med.name, fontSize = 12.sp) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = batchNumberText,
                        onValueChange = { batchNumberText = it },
                        label = { Text("Batch / Lot No.") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = expiryFormattedText,
                        onValueChange = { expiryFormattedText = it },
                        label = { Text("Expiry (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = purchaseRateText,
                        onValueChange = { purchaseRateText = it },
                        label = { Text("Purchase Rate ($)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = mrpText,
                        onValueChange = { mrpText = it },
                        label = { Text("Retail MRP ($)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = {
                            quantityText = it
                            val newQty = it.toIntOrNull() ?: 0
                            paidAmountText = String.format("%.2f", rate * newQty)
                        },
                        label = { Text("Inward Qty (Units)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = paidAmountText,
                        onValueChange = { paidAmountText = it },
                        label = { Text("Paid to Vendor ($)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                OutlinedTextField(
                    value = purchaseNotes,
                    onValueChange = { purchaseNotes = it },
                    label = { Text("Delivery Challan / Bill Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Purchase Inward Value:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("$${String.format("%.2f", totalCost)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (selectedMedicine != null) {
                                viewModel.submitPurchase(
                                    supplier = selectedSupplier,
                                    supplierName = selectedSupplier?.name ?: customSupplierName,
                                    medicine = selectedMedicine!!,
                                    batchNumber = batchNumberText,
                                    expiryFormatted = expiryFormattedText,
                                    purchaseRate = rate,
                                    mrp = mrpText.toDoubleOrNull() ?: (rate * 1.3),
                                    quantity = qty,
                                    paidAmount = paidAmountText.toDoubleOrNull() ?: totalCost,
                                    notes = purchaseNotes
                                )
                            }
                        },
                        modifier = Modifier.weight(1.5f).height(48.dp).testTag("confirm_purchase_button"),
                        enabled = selectedMedicine != null && qty > 0 && rate > 0
                    ) {
                        Text("Inward Stock", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RecordRecoveryDialog(
    viewModel: PharmaViewModel,
    customers: List<Customer>,
    initialCustomer: Customer?,
    onDismiss: () -> Unit
) {
    val eligibleCustomers = remember(customers) { customers.filter { it.outstandingBalance > 0 } }
    var selectedCustomer by remember { mutableStateOf(initialCustomer ?: eligibleCustomers.firstOrNull()) }
    var amountText by remember { mutableStateOf(selectedCustomer?.let { String.format("%.2f", it.outstandingBalance) } ?: "") }
    var paymentMode by remember { mutableStateOf("CASH") } // CASH, BANK_TRANSFER, ONLINE, CHEQUE
    var referenceNo by remember { mutableStateOf("TXN-" + (10000..99999).random()) }
    var recoveryNotes by remember { mutableStateOf("Customer debt recovery payment") }
    var autoSendWhatsApp by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Record Customer Debt Recovery",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Collect outstanding dues, credit balances & auto-sync WhatsApp receipt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                Text("Select Customer with Due Balance:", style = MaterialTheme.typography.labelSmall)
                if (eligibleCustomers.isEmpty()) {
                    Text("✅ No customers have outstanding balances!", color = StatusSuccess, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                } else {
                    ScrollableTabRow(
                        selectedTabIndex = if (selectedCustomer == null) -1 else eligibleCustomers.indexOf(selectedCustomer).coerceAtLeast(0),
                        edgePadding = 0.dp,
                        divider = {}
                    ) {
                        eligibleCustomers.forEach { cust ->
                            Tab(
                                selected = selectedCustomer?.id == cust.id,
                                onClick = {
                                    selectedCustomer = cust
                                    amountText = String.format("%.2f", cust.outstandingBalance)
                                },
                                text = { Text("${cust.name} ($${cust.outstandingBalance.toInt()})", fontSize = 12.sp) }
                            )
                        }
                    }
                }

                if (selectedCustomer != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(selectedCustomer!!.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (selectedCustomer!!.phone.isNotBlank()) {
                                    Text("Phone: ${selectedCustomer!!.phone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("Overdue: ${selectedCustomer!!.daysOverdue} days", color = StatusDanger, fontSize = 12.sp)
                            }
                            Text(
                                text = "$${String.format("%.2f", selectedCustomer!!.outstandingBalance)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = StatusDanger
                            )
                        }
                    }

                    // Quick Settlement Shortcuts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.25, 0.50, 1.0).forEach { ratio ->
                            val amt = selectedCustomer!!.outstandingBalance * ratio
                            OutlinedButton(
                                onClick = { amountText = String.format("%.2f", amt) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (ratio == 1.0) "Full (100%)" else "${(ratio * 100).toInt()}%", fontSize = 11.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount Collected ($)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // WhatsApp Auto-Sync Toggle Card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (autoSendWhatsApp) Color(0xFFE8F8F0) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, if (autoSendWhatsApp) Color(0xFF25D366).copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedCustomer!!.phone.isNotBlank()) {
                                    autoSendWhatsApp = !autoSendWhatsApp
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Message,
                                contentDescription = "WhatsApp",
                                tint = if (autoSendWhatsApp) Color(0xFF25D366) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Send WhatsApp Receipt to Customer",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (autoSendWhatsApp) Color(0xFF075E54) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (selectedCustomer!!.phone.isNotBlank())
                                        "Automated message sent to ${selectedCustomer!!.phone} when data is connected"
                                    else "⚠️ Customer has no phone recorded",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Checkbox(
                                checked = autoSendWhatsApp && selectedCustomer!!.phone.isNotBlank(),
                                onCheckedChange = { autoSendWhatsApp = it },
                                enabled = selectedCustomer!!.phone.isNotBlank()
                            )
                        }
                    }

                    // Payment Mode Selector
                    Text("Payment Received Via:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("CASH", "BANK_TRANSFER", "ONLINE", "CHEQUE").forEach { mode ->
                            val isSel = paymentMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { paymentMode = mode },
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = mode.replace("_", " "),
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = referenceNo,
                        onValueChange = { referenceNo = it },
                        label = { Text("Reference / Receipt No.") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = recoveryNotes,
                        onValueChange = { recoveryNotes = it },
                        label = { Text("Notes / Ledger memo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (selectedCustomer != null) {
                                val amt = amountText.toDoubleOrNull() ?: 0.0
                                viewModel.submitRecoveryPayment(
                                    customer = selectedCustomer!!,
                                    amountPaid = amt,
                                    paymentMode = paymentMode,
                                    referenceNo = referenceNo,
                                    notes = recoveryNotes,
                                    autoSendWhatsApp = autoSendWhatsApp
                                )
                            }
                        },
                        colors = if (autoSendWhatsApp && selectedCustomer?.phone?.isNotBlank() == true)
                            ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        else ButtonDefaults.buttonColors(),
                        modifier = Modifier.weight(1.5f).height(48.dp).testTag("confirm_recovery_button"),
                        enabled = selectedCustomer != null && (amountText.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        if (autoSendWhatsApp && selectedCustomer?.phone?.isNotBlank() == true) {
                            Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save & WhatsApp", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Text("Record Recovery", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddReturnDialog(
    viewModel: PharmaViewModel,
    medicines: List<Medicine>,
    batches: List<Batch>,
    customers: List<Customer>,
    suppliers: List<Supplier>,
    onDismiss: () -> Unit
) {
    var returnType by remember { mutableStateOf("SUPPLIER_RETURN_EXPIRY") } // SUPPLIER_RETURN_EXPIRY, CUSTOMER_RETURN, BATCH_RECALL
    var selectedMedicine by remember { mutableStateOf(medicines.firstOrNull()) }
    var selectedBatch by remember { mutableStateOf<Batch?>(null) }
    var partyNameText by remember { mutableStateOf("") }
    var returnReason by remember { mutableStateOf("EXPIRED_STOCK") } // EXPIRED_STOCK, DAMAGED_PACKAGING, CUSTOMER_EXCHANGE, WRONG_ITEM
    var restockAction by remember { mutableStateOf("RETURN_TO_SUPPLIER") } // RETURN_TO_SUPPLIER, DISCARD_DESTROY, RESTOCK_INVENTORY
    var quantityText by remember { mutableStateOf("10") }
    var unitRateText by remember { mutableStateOf("12.00") }
    var returnNotes by remember { mutableStateOf("") }

    val medBatches = remember(selectedMedicine, batches) {
        if (selectedMedicine == null) emptyList()
        else batches.filter { it.medicineId == selectedMedicine!!.id }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Process Medicine Return / Recall",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Manage expired lots, damaged stock, customer exchanges & vendor claims",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                // Return Type Tabs
                Text("Return Category:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "SUPPLIER_RETURN_EXPIRY" to "Supplier Expiry Claim",
                        "CUSTOMER_RETURN" to "Customer Return",
                        "BATCH_RECALL" to "Lot Recall"
                    ).forEach { (key, label) ->
                        val isSel = returnType == key
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    returnType = key
                                    when (key) {
                                        "SUPPLIER_RETURN_EXPIRY" -> {
                                            returnReason = "EXPIRED_STOCK"
                                            restockAction = "RETURN_TO_SUPPLIER"
                                            partyNameText = suppliers.firstOrNull()?.name ?: "Premier Pharma"
                                        }
                                        "CUSTOMER_RETURN" -> {
                                            returnReason = "CUSTOMER_EXCHANGE"
                                            restockAction = "RESTOCK_INVENTORY"
                                            partyNameText = customers.firstOrNull()?.name ?: "Retail Client"
                                        }
                                        "BATCH_RECALL" -> {
                                            returnReason = "MANUFACTURER_RECALL"
                                            restockAction = "DISCARD_DESTROY"
                                            partyNameText = "Regulatory / Manufacturer"
                                        }
                                    }
                                },
                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // Party Name
                OutlinedTextField(
                    value = partyNameText,
                    onValueChange = { partyNameText = it },
                    label = { Text(if (returnType == "CUSTOMER_RETURN") "Customer Name" else "Distributor / Supplier Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Select Medicine
                Text("Select Medicine:", style = MaterialTheme.typography.labelSmall)
                ScrollableTabRow(
                    selectedTabIndex = if (selectedMedicine == null) -1 else medicines.indexOf(selectedMedicine).coerceAtLeast(0),
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    medicines.forEach { med ->
                        Tab(
                            selected = selectedMedicine?.id == med.id,
                            onClick = {
                                selectedMedicine = med
                                val bList = batches.filter { it.medicineId == med.id }
                                selectedBatch = bList.firstOrNull()
                                unitRateText = String.format("%.2f", selectedBatch?.purchasePrice ?: 10.0)
                            },
                            text = { Text(med.name, fontSize = 12.sp) }
                        )
                    }
                }

                // Select Batch if available
                if (medBatches.isNotEmpty()) {
                    Text("Select Batch Lot:", style = MaterialTheme.typography.labelSmall)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        medBatches.forEach { b ->
                            val isSel = selectedBatch?.id == b.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSel) 2.dp else 1.dp,
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedBatch = b
                                        unitRateText = String.format("%.2f", b.purchasePrice)
                                    },
                                color = if (isSel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Lot: ${b.batchNumber} (Exp: ${b.expiryDateFormatted})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Stock: ${b.currentStock}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Return Qty") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = unitRateText,
                        onValueChange = { unitRateText = it },
                        label = { Text("Credit / Unit Rate ($)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                // Restock / Disposition Action
                Text("Inventory Disposition Action:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "RETURN_TO_SUPPLIER" to "Supplier Claim",
                        "RESTOCK_INVENTORY" to "Restock Shelf",
                        "DISCARD_DESTROY" to "Quarantine / Discard"
                    ).forEach { (act, label) ->
                        val isSel = restockAction == act
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { restockAction = act },
                            color = if (isSel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    color = if (isSel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = returnNotes,
                    onValueChange = { returnNotes = it },
                    label = { Text("Reason & Return Memo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                val qty = quantityText.toIntOrNull() ?: 0
                val rate = unitRateText.toDoubleOrNull() ?: 0.0
                val totalRefund = qty * rate

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Return / Claim Value:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("$${String.format("%.2f", totalRefund)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (selectedMedicine != null) {
                                viewModel.submitReturn(
                                    returnType = returnType,
                                    partyName = partyNameText.ifBlank { "Direct Return" },
                                    batch = selectedBatch,
                                    medicine = selectedMedicine!!,
                                    batchNumber = selectedBatch?.batchNumber ?: "LOT-MANUAL",
                                    expiryFormatted = selectedBatch?.expiryDateFormatted ?: "2026-12",
                                    quantity = qty,
                                    unitRate = rate,
                                    restockAction = restockAction,
                                    reason = returnReason,
                                    notes = returnNotes
                                )
                            }
                        },
                        modifier = Modifier.weight(1.5f).height(48.dp).testTag("confirm_return_button"),
                        enabled = selectedMedicine != null && qty > 0
                    ) {
                        Text("Process Return", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddMedicineDialog(
    viewModel: PharmaViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var genericFormula by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Antibiotic") }
    var dosageForm by remember { mutableStateOf("Tablet") }
    var manufacturer by remember { mutableStateOf("GSK") }
    var minStock by remember { mutableStateOf("20") }
    var locationRack by remember { mutableStateOf("Rack A-01") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add Medicine to Catalog",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Enter medicine details, salt composition, and rack storage",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medicine Brand Name (e.g. Augmentin 625mg)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = genericFormula,
                    onValueChange = { genericFormula = it },
                    label = { Text("Generic Salt / Formula (e.g. Amoxicillin + Clavulanic Acid)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dosageForm,
                        onValueChange = { dosageForm = it },
                        label = { Text("Dosage Form (Tablet, Syrup)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = manufacturer,
                        onValueChange = { manufacturer = it },
                        label = { Text("Pharma Manufacturer") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minStock,
                        onValueChange = { minStock = it },
                        label = { Text("Min Reorder Qty") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                OutlinedTextField(
                    value = locationRack,
                    onValueChange = { locationRack = it },
                    label = { Text("Storage Location / Shelf Rack") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            viewModel.submitMedicine(
                                name = name,
                                genericFormula = genericFormula,
                                category = category,
                                dosageForm = dosageForm,
                                manufacturer = manufacturer,
                                minStockLevel = minStock.toIntOrNull() ?: 20,
                                locationRack = locationRack
                            )
                        },
                        modifier = Modifier.weight(1.5f).height(48.dp).testTag("confirm_medicine_button"),
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save Medicine", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SaleReceiptDialog(
    sale: SaleTransaction,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateFormatted = remember(sale.timestamp) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(sale.timestamp))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Pharmacy Brand
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PHARMAFLOW MEDICAL & PHARMACY",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Licensed Pharmacy • Retail & Wholesale", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("GST / Tax Reg # PH-992014-A", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Invoice #: ${sale.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Date: $dateFormatted", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusBadge(text = sale.paymentStatus, statusType = sale.paymentStatus)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text("Customer: ${sale.customerName}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    if (sale.customerPhone.isNotBlank()) {
                        Text("Phone: ${sale.customerPhone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Payment Mode: ${sale.paymentMode}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Breakdown
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gross Total:", fontSize = 13.sp)
                        Text("$${String.format("%.2f", sale.totalAmount)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (sale.discountAmount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Discount (${sale.discountPercent}%):", fontSize = 12.sp, color = StatusSuccess)
                            Text("-$${String.format("%.2f", sale.discountAmount)}", fontSize = 12.sp, color = StatusSuccess)
                        }
                    }
                    if (sale.taxAmount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tax (${sale.taxPercent}%):", fontSize = 12.sp)
                            Text("+$${String.format("%.2f", sale.taxAmount)}", fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Net Payable:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("$${String.format("%.2f", sale.netPayable)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Amount Paid:", fontSize = 12.sp)
                        Text("$${String.format("%.2f", sale.paidAmount)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (sale.balanceDue > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Outstanding Due:", fontSize = 12.sp, color = StatusDanger, fontWeight = FontWeight.Bold)
                            Text("$${String.format("%.2f", sale.balanceDue)}", fontSize = 12.sp, color = StatusDanger, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (sale.notes.isNotBlank()) {
                    Text("Notes: ${sale.notes}", fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val shareText = "PharmaFlow Invoice: ${sale.invoiceNumber}\nCustomer: ${sale.customerName}\nNet Amount: $${String.format("%.2f", sale.netPayable)}\nPaid: $${String.format("%.2f", sale.paidAmount)}\nBalance Due: $${String.format("%.2f", sale.balanceDue)}\nThank you for choosing PharmaFlow!"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Invoice Receipt"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
fun AiReminderDialog(
    reminderText: String,
    customer: Customer,
    isLoading: Boolean,
    viewModel: PharmaViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTone by remember { mutableStateOf("Polite") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(22.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AI Recovery Message Draft",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "For: ${customer.name} ($${customer.outstandingBalance.toInt()} Due)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                // Tone Selector
                Text("Select Message Tone:", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Polite", "Standard", "Firm", "Urgent").forEach { tone ->
                        val isSel = selectedTone == tone
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedTone = tone
                                    viewModel.generateAiRecoveryReminder(customer, tone)
                                },
                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = tone,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Text("Gemini is drafting optimal recovery notice...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = reminderText,
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Recovery Reminder", reminderText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Copy Text")
                    }

                    Button(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, reminderText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Send Payment Reminder via WhatsApp/SMS"))
                        },
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share / WhatsApp")
                    }
                }
            }
        }
    }
}
