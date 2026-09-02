package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiPharmaService
import com.example.data.local.AppDatabase
import com.example.data.model.Batch
import com.example.data.model.Customer
import com.example.data.model.Medicine
import com.example.data.model.PurchaseTransaction
import com.example.data.model.RecoveryPayment
import com.example.data.model.ReturnTransaction
import com.example.data.model.SaleItem
import com.example.data.model.SaleTransaction
import com.example.data.model.Supplier
import com.example.data.repository.PharmaRepository
import com.example.data.repository.PurchaseItemInput
import com.example.data.repository.ReturnItemInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PharmaTab {
    DASHBOARD,
    SALES,
    PURCHASES,
    RECOVERY,
    RETURNS,
    AI_COPILOT,
    INVENTORY
}

data class CartItem(
    val medicine: Medicine,
    val batch: Batch,
    val quantity: Int,
    val unitPrice: Double
) {
    val totalPrice: Double get() = quantity * unitPrice
}

data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class PharmaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PharmaRepository

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        val aiService = GeminiPharmaService()
        repository = PharmaRepository(database.pharmaDao(), aiService)
    }

    // Selected Navigation Tab
    private val _currentTab = MutableStateFlow(PharmaTab.DASHBOARD)
    val currentTab: StateFlow<PharmaTab> = _currentTab.asStateFlow()

    // Global Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // DB Streams
    val medicines: StateFlow<List<Medicine>> = repository.allMedicines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val batches: StateFlow<List<Batch>> = repository.allBatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overdueCustomers: StateFlow<List<Customer>> = repository.overdueCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<Supplier>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<SaleTransaction>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchases: StateFlow<List<PurchaseTransaction>> = repository.allPurchases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recoveries: StateFlow<List<RecoveryPayment>> = repository.allRecoveries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val returns: StateFlow<List<ReturnTransaction>> = repository.allReturns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expiringBatches: StateFlow<List<Batch>> = repository.getExpiringBatches(60)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expiredBatches: StateFlow<List<Batch>> = repository.getExpiredBatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Financial KPIs
    val totalSales: StateFlow<Double> = repository.totalSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPurchases: StateFlow<Double> = repository.totalPurchases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalOutstandingRecovery: StateFlow<Double> = repository.totalOutstandingRecovery
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalReturns: StateFlow<Double> = repository.totalReturns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalRecovered: StateFlow<Double> = repository.totalRecovered
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Sale POS Cart State
    private val _saleCart = MutableStateFlow<List<CartItem>>(emptyList())
    val saleCart: StateFlow<List<CartItem>> = _saleCart.asStateFlow()

    // Dialog & UI Action States
    private val _showAddSaleDialog = MutableStateFlow(false)
    val showAddSaleDialog: StateFlow<Boolean> = _showAddSaleDialog.asStateFlow()

    private val _showAddPurchaseDialog = MutableStateFlow(false)
    val showAddPurchaseDialog: StateFlow<Boolean> = _showAddPurchaseDialog.asStateFlow()

    private val _showRecordRecoveryDialog = MutableStateFlow(false)
    val showRecordRecoveryDialog: StateFlow<Boolean> = _showRecordRecoveryDialog.asStateFlow()

    private val _showAddReturnDialog = MutableStateFlow(false)
    val showAddReturnDialog: StateFlow<Boolean> = _showAddReturnDialog.asStateFlow()

    private val _showAddMedicineDialog = MutableStateFlow(false)
    val showAddMedicineDialog: StateFlow<Boolean> = _showAddMedicineDialog.asStateFlow()

    private val _selectedCustomerForRecovery = MutableStateFlow<Customer?>(null)
    val selectedCustomerForRecovery: StateFlow<Customer?> = _selectedCustomerForRecovery.asStateFlow()

    private val _selectedSaleForReceipt = MutableStateFlow<SaleTransaction?>(null)
    val selectedSaleForReceipt: StateFlow<SaleTransaction?> = _selectedSaleForReceipt.asStateFlow()

    // AI States
    private val _aiReminderText = MutableStateFlow<String?>(null)
    val aiReminderText: StateFlow<String?> = _aiReminderText.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiChatMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                isUser = false,
                message = "Hello! I am PharmaFlow AI, your pharmacy business copilot. You can ask me to:\n• Find generic drug substitutes and salt compositions\n• Draft polite or firm customer debt recovery messages\n• Plan FEFO expiry returns and vendor debit claims\n• Analyze sales margins and reorder forecasts."
            )
        )
    )
    val aiChatMessages: StateFlow<List<AiChatMessage>> = _aiChatMessages.asStateFlow()

    // Notification Snackbar / Feedback State
    private val _userFeedback = MutableStateFlow<String?>(null)
    val userFeedback: StateFlow<String?> = _userFeedback.asStateFlow()

    fun setTab(tab: PharmaTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearFeedback() {
        _userFeedback.value = null
    }

    // --- Cart Actions for Sales ---
    fun addToCart(medicine: Medicine, batch: Batch, quantity: Int = 1) {
        val current = _saleCart.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.batch.id == batch.id }
        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            val newQty = minOf(batch.currentStock, existing.quantity + quantity)
            current[existingIndex] = existing.copy(quantity = newQty)
        } else {
            current.add(
                CartItem(
                    medicine = medicine,
                    batch = batch,
                    quantity = minOf(batch.currentStock, quantity),
                    unitPrice = batch.salePrice
                )
            )
        }
        _saleCart.value = current
    }

    fun removeFromCart(batchId: Long) {
        _saleCart.value = _saleCart.value.filter { it.batch.id != batchId }
    }

    fun updateCartQuantity(batchId: Long, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(batchId)
        } else {
            _saleCart.value = _saleCart.value.map { item ->
                if (item.batch.id == batchId) {
                    item.copy(quantity = minOf(item.batch.currentStock, newQuantity))
                } else item
            }
        }
    }

    fun clearCart() {
        _saleCart.value = emptyList()
    }

    // --- Business Workflow Triggers ---
    fun openAddSale() {
        _showAddSaleDialog.value = true
    }
    fun closeAddSale() {
        _showAddSaleDialog.value = false
        clearCart()
    }

    fun openAddPurchase() {
        _showAddPurchaseDialog.value = true
    }
    fun closeAddPurchase() {
        _showAddPurchaseDialog.value = false
    }

    fun openRecordRecovery(customer: Customer? = null) {
        _selectedCustomerForRecovery.value = customer
        _showRecordRecoveryDialog.value = true
    }
    fun closeRecordRecovery() {
        _showRecordRecoveryDialog.value = false
        _selectedCustomerForRecovery.value = null
    }

    fun openAddReturn() {
        _showAddReturnDialog.value = true
    }
    fun closeAddReturn() {
        _showAddReturnDialog.value = false
    }

    fun openAddMedicine() {
        _showAddMedicineDialog.value = true
    }
    fun closeAddMedicine() {
        _showAddMedicineDialog.value = false
    }

    fun openSaleReceipt(sale: SaleTransaction) {
        _selectedSaleForReceipt.value = sale
    }
    fun closeSaleReceipt() {
        _selectedSaleForReceipt.value = null
    }

    // Execute Sale
    fun submitSale(
        customer: Customer?,
        customerName: String,
        customerPhone: String,
        discountPercent: Double,
        taxPercent: Double,
        paidAmount: Double,
        paymentMode: String,
        notes: String
    ) {
        val cartItems = _saleCart.value
        if (cartItems.isEmpty()) {
            _userFeedback.value = "Cart is empty. Please add items."
            return
        }

        viewModelScope.launch {
            try {
                val saleItems = cartItems.map { cart ->
                    SaleItem(
                        saleId = 0,
                        medicineId = cart.medicine.id,
                        medicineName = cart.medicine.name,
                        batchId = cart.batch.id,
                        batchNumber = cart.batch.batchNumber,
                        expiryFormatted = cart.batch.expiryDateFormatted,
                        unitPrice = cart.unitPrice,
                        quantity = cart.quantity,
                        totalPrice = cart.totalPrice
                    )
                }

                val saleId = repository.processSale(
                    customerId = customer?.id,
                    customerName = if (customer != null) customer.name else customerName,
                    customerPhone = if (customer != null) customer.phone else customerPhone,
                    items = saleItems,
                    discountPercent = discountPercent,
                    taxPercent = taxPercent,
                    paidAmount = paidAmount,
                    paymentMode = paymentMode,
                    notes = notes
                )

                _userFeedback.value = "Sale Invoice generated successfully! (ID: #$saleId)"
                closeAddSale()
            } catch (e: Exception) {
                _userFeedback.value = "Failed to process sale: ${e.message}"
            }
        }
    }

    // Execute Purchase
    fun submitPurchase(
        supplier: Supplier?,
        supplierName: String,
        medicine: Medicine,
        batchNumber: String,
        expiryFormatted: String,
        purchaseRate: Double,
        mrp: Double,
        quantity: Int,
        paidAmount: Double,
        notes: String
    ) {
        if (quantity <= 0 || purchaseRate <= 0.0 || batchNumber.isBlank()) {
            _userFeedback.value = "Please provide valid purchase details."
            return
        }

        viewModelScope.launch {
            try {
                val item = PurchaseItemInput(
                    medicineId = medicine.id,
                    medicineName = medicine.name,
                    batchNumber = batchNumber.trim().uppercase(),
                    expiryFormatted = expiryFormatted.trim(),
                    purchaseRate = purchaseRate,
                    mrp = mrp,
                    quantity = quantity
                )

                val purchaseId = repository.processPurchase(
                    supplierId = supplier?.id,
                    supplierName = supplier?.name ?: supplierName,
                    items = listOf(item),
                    paidAmount = paidAmount,
                    notes = notes
                )

                _userFeedback.value = "Stock inward & purchase recorded! (PO #$purchaseId)"
                closeAddPurchase()
            } catch (e: Exception) {
                _userFeedback.value = "Error saving purchase: ${e.message}"
            }
        }
    }

    // Execute Recovery Payment
    fun submitRecoveryPayment(
        customer: Customer,
        amountPaid: Double,
        paymentMode: String,
        referenceNo: String,
        notes: String
    ) {
        if (amountPaid <= 0) {
            _userFeedback.value = "Enter a valid recovery payment amount."
            return
        }

        viewModelScope.launch {
            try {
                val recId = repository.recordRecoveryPayment(
                    customerId = customer.id,
                    customerName = customer.name,
                    customerPhone = customer.phone,
                    invoiceNumber = "DUE-SETTLEMENT",
                    amountPaid = amountPaid,
                    paymentMode = paymentMode,
                    referenceNumber = referenceNo,
                    notes = notes
                )

                _userFeedback.value = "Recovery of $${String.format("%.2f", amountPaid)} recorded from ${customer.name}!"
                closeRecordRecovery()
            } catch (e: Exception) {
                _userFeedback.value = "Error recording recovery: ${e.message}"
            }
        }
    }

    // Execute Return
    fun submitReturn(
        returnType: String,
        partyName: String,
        batch: Batch?,
        medicine: Medicine,
        batchNumber: String,
        expiryFormatted: String,
        quantity: Int,
        unitRate: Double,
        restockAction: String,
        reason: String,
        notes: String,
        customerIdForCredit: Long? = null
    ) {
        if (quantity <= 0 || unitRate < 0) {
            _userFeedback.value = "Please enter a valid quantity."
            return
        }

        viewModelScope.launch {
            try {
                val returnItem = ReturnItemInput(
                    batchId = batch?.id,
                    medicineId = medicine.id,
                    medicineName = medicine.name,
                    batchNumber = batch?.batchNumber ?: batchNumber,
                    expiryFormatted = batch?.expiryDateFormatted ?: expiryFormatted,
                    unitRate = unitRate,
                    quantity = quantity,
                    restockAction = restockAction
                )

                val retId = repository.processReturn(
                    returnType = returnType,
                    partyName = partyName,
                    items = listOf(returnItem),
                    reason = reason,
                    notes = notes,
                    customerIdForCreditRefund = customerIdForCredit
                )

                _userFeedback.value = "Medicine return voucher ($returnType) processed! (ID: #$retId)"
                closeAddReturn()
            } catch (e: Exception) {
                _userFeedback.value = "Error processing return: ${e.message}"
            }
        }
    }

    // Add New Medicine
    fun submitMedicine(
        name: String,
        genericFormula: String,
        category: String,
        dosageForm: String,
        manufacturer: String,
        minStockLevel: Int,
        locationRack: String
    ) {
        if (name.isBlank()) {
            _userFeedback.value = "Medicine name is required."
            return
        }

        viewModelScope.launch {
            try {
                val med = Medicine(
                    name = name.trim(),
                    genericFormula = genericFormula.trim(),
                    category = category.trim(),
                    dosageForm = dosageForm.trim(),
                    manufacturer = manufacturer.trim(),
                    minStockLevel = maxOf(1, minStockLevel),
                    locationRack = locationRack.trim()
                )
                repository.insertMedicine(med)
                _userFeedback.value = "Added $name to pharmacy catalog!"
                closeAddMedicine()
            } catch (e: Exception) {
                _userFeedback.value = "Failed to add medicine: ${e.message}"
            }
        }
    }

    // --- AI Features ---
    fun generateAiRecoveryReminder(customer: Customer, tone: String = "Polite") {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val reminder = repository.getAiRecoveryReminder(
                    customerName = customer.name,
                    amount = customer.outstandingBalance,
                    daysOverdue = customer.daysOverdue,
                    invoiceNo = "STATEMENT-${customer.id}",
                    tone = tone
                )
                _aiReminderText.value = reminder
            } catch (e: Exception) {
                _aiReminderText.value = "Could not generate reminder: ${e.message}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun clearAiReminder() {
        _aiReminderText.value = null
    }

    fun sendAiChatMessage(userMessage: String) {
        if (userMessage.isBlank()) return
        val current = _aiChatMessages.value.toMutableList()
        current.add(AiChatMessage(isUser = true, message = userMessage))
        _aiChatMessages.value = current
        _isAiLoading.value = true

        viewModelScope.launch {
            try {
                val response = repository.askAiPharma(userMessage)
                val updated = _aiChatMessages.value.toMutableList()
                updated.add(AiChatMessage(isUser = false, message = response))
                _aiChatMessages.value = updated
            } catch (e: Exception) {
                val updated = _aiChatMessages.value.toMutableList()
                updated.add(AiChatMessage(isUser = false, message = "AI error: ${e.message}"))
                _aiChatMessages.value = updated
            } finally {
                _isAiLoading.value = false
            }
        }
    }
}
