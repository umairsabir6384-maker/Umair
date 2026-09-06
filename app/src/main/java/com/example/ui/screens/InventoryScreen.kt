package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Batch
import com.example.data.model.Medicine
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MedicineImageView
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.PharmaViewModel

@Composable
fun InventoryScreen(
    viewModel: PharmaViewModel,
    medicines: List<Medicine>,
    batches: List<Batch>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var medicineToDelete by remember { mutableStateOf<Medicine?>(null) }

    val categories = remember(medicines) {
        listOf("ALL") + medicines.map { it.category }.distinct()
    }

    val filteredMedicines = remember(medicines, searchQuery, selectedCategory) {
        medicines.filter { med ->
            val matchesQuery = searchQuery.isBlank() ||
                    med.name.contains(searchQuery, ignoreCase = true) ||
                    med.genericFormula.contains(searchQuery, ignoreCase = true) ||
                    med.manufacturer.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategory == "ALL" || med.category.equals(selectedCategory, ignoreCase = true)

            matchesQuery && matchesCategory
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("inventory_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddMedicine() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_medicine_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Medicine")
                    Text("Add Medicine", fontWeight = FontWeight.Bold)
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().testTag("inventory_search_field"),
                placeholder = { Text("Search brand name, salt formula, company...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Category Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) }
                    )
                }
            }

            // Summary Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredMedicines.size} Medicines in Formulary",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${batches.sumOf { it.currentStock }} Units Total Stock",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (filteredMedicines.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Medication,
                    title = "No Medicines Found",
                    description = "Add medicines to your pharmacy formulary with rack locations."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 72.dp)
                ) {
                    items(filteredMedicines, key = { it.id }) { med ->
                        val medBatches = batches.filter { it.medicineId == med.id }
                        val totalStock = medBatches.sumOf { it.currentStock }
                        MedicineCatalogCard(
                            medicine = med,
                            batches = medBatches,
                            totalStock = totalStock,
                            onDeleteClick = { medicineToDelete = med }
                        )
                    }
                }
            }
        }
    }

    if (medicineToDelete != null) {
        val med = medicineToDelete!!
        AlertDialog(
            onDismissRequest = { medicineToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Medicine") },
            text = {
                Text("Are you sure you want to delete \"${med.name}\"?\n\nAll associated batch inventory and history will also be permanently deleted.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMedicine(med)
                        medicineToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_medicine_btn")
                ) {
                    Text("Delete Medicine")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { medicineToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MedicineCatalogCard(
    medicine: Medicine,
    batches: List<Batch>,
    totalStock: Int,
    onDeleteClick: () -> Unit
) {
    val isLowStock = totalStock <= medicine.minStockLevel

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("medicine_card_${medicine.name}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.GeometricBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Picture Section for Medicine
                MedicineImageView(
                    imageUri = medicine.imageUri,
                    dosageForm = medicine.dosageForm,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentDescription = medicine.name
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = medicine.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("delete_medicine_${medicine.id}_btn")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Medicine",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = "Salt: ${medicine.genericFormula}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${medicine.manufacturer} • ${medicine.dosageForm}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StatusBadge(
                            text = if (totalStock == 0) "OUT OF STOCK" else if (isLowStock) "LOW STOCK" else "$totalStock Units",
                            statusType = if (totalStock == 0) "DANGER" else if (isLowStock) "WARNING" else "SUCCESS"
                        )
                    }
                }
            }

            HorizontalDivider(color = com.example.ui.theme.GeometricBorder.copy(alpha = 0.6f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📍 Rack: ${medicine.locationRack}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val nearestExpiry = batches.filter { it.currentStock > 0 }.minByOrNull { it.expiryDateEpochMs }
                if (nearestExpiry != null) {
                    Text(
                        text = "Exp: ${nearestExpiry.expiryDateFormatted} (${batches.size} batches)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StatusWarning
                    )
                } else {
                    Text(
                        text = "${batches.size} Batches",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
