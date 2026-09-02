package com.example.data.repository

import com.example.data.ai.GeminiPharmaService
import com.example.data.local.PharmaDao
import com.example.data.model.Batch
import com.example.data.model.Customer
import com.example.data.model.Medicine
import com.example.data.model.PurchaseItem
import com.example.data.model.PurchaseTransaction
import com.example.data.model.RecoveryPayment
import com.example.data.model.ReturnItem
import com.example.data.model.ReturnTransaction
import com.example.data.model.SaleItem
import com.example.data.model.SaleTransaction
import com.example.data.model.Supplier
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PharmaRepository(
    private val dao: PharmaDao,
    private val aiService: GeminiPharmaService
) {

    // Medicines & Batches
    val allMedicines: Flow<List<Medicine>> = dao.getAllMedicines()
    val allBatches: Flow<List<Batch>> = dao.getAllBatches()
    val allCustomers: Flow<List<Customer>> = dao.getAllCustomers()
    val overdueCustomers: Flow<List<Customer>> = dao.getCustomersWithOutstandingDues()
    val allSuppliers: Flow<List<Supplier>> = dao.getAllSuppliers()
    val allSales: Flow<List<SaleTransaction>> = dao.getAllSales()
    val allPurchases: Flow<List<PurchaseTransaction>> = dao.getAllPurchases()
    val allRecoveries: Flow<List<RecoveryPayment>> = dao.getAllRecoveryPayments()
    val allReturns: Flow<List<ReturnTransaction>> = dao.getAllReturns()

    // Aggregates
    val totalSales: Flow<Double> = dao.getTotalSalesSum()
    val totalPurchases: Flow<Double> = dao.getTotalPurchasesSum()
    val totalOutstandingRecovery: Flow<Double> = dao.getTotalOutstandingRecoveries()
    val totalReturns: Flow<Double> = dao.getTotalReturnsSum()
    val totalRecovered: Flow<Double> = dao.getTotalRecoveredAmount()

    fun searchMedicines(query: String): Flow<List<Medicine>> = dao.searchMedicines(query)

    fun getBatchesForMedicine(medicineId: Long): Flow<List<Batch>> = dao.getActiveBatchesForMedicine(medicineId)

    suspend fun getBatchesForMedicineSync(medicineId: Long): List<Batch> = dao.getActiveBatchesForMedicineSync(medicineId)

    fun getExpiringBatches(daysAhead: Int = 60): Flow<List<Batch>> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, daysAhead)
        return dao.getBatchesExpiringBefore(cal.timeInMillis)
    }

    fun getExpiredBatches(): Flow<List<Batch>> {
        return dao.getExpiredBatches(System.currentTimeMillis())
    }

    suspend fun insertMedicine(medicine: Medicine): Long = dao.insertMedicine(medicine)

    suspend fun updateMedicine(medicine: Medicine) = dao.updateMedicine(medicine)

    suspend fun deleteMedicine(medicine: Medicine) = dao.deleteMedicine(medicine)

    suspend fun insertBatch(batch: Batch): Long = dao.insertBatch(batch)

    suspend fun updateBatch(batch: Batch) = dao.updateBatch(batch)

    suspend fun deleteBatch(batchId: Long) = dao.deleteBatch(batchId)

    suspend fun insertCustomer(customer: Customer): Long = dao.insertCustomer(customer)

    suspend fun updateCustomer(customer: Customer) = dao.updateCustomer(customer)

    suspend fun insertSupplier(supplier: Supplier): Long = dao.insertSupplier(supplier)

    // Complete Sale Transaction Workflow
    suspend fun processSale(
        customerId: Long?,
        customerName: String,
        customerPhone: String,
        items: List<SaleItem>,
        discountPercent: Double,
        taxPercent: Double,
        paidAmount: Double,
        paymentMode: String,
        notes: String
    ): Long {
        val totalAmount = items.sumOf { it.totalPrice }
        val discountAmount = totalAmount * (discountPercent / 100.0)
        val afterDiscount = totalAmount - discountAmount
        val taxAmount = afterDiscount * (taxPercent / 100.0)
        val netPayable = afterDiscount + taxAmount
        val balanceDue = maxOf(0.0, netPayable - paidAmount)

        val paymentStatus = when {
            balanceDue <= 0.001 -> "PAID"
            paidAmount > 0 -> "PARTIAL"
            else -> "DUE"
        }

        val invoiceNumber = "INV-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())

        val sale = SaleTransaction(
            invoiceNumber = invoiceNumber,
            customerId = customerId,
            customerName = customerName.ifBlank { "Walk-in Retail Customer" },
            customerPhone = customerPhone,
            totalAmount = totalAmount,
            discountPercent = discountPercent,
            discountAmount = discountAmount,
            taxPercent = taxPercent,
            taxAmount = taxAmount,
            netPayable = netPayable,
            paidAmount = paidAmount,
            balanceDue = balanceDue,
            paymentMode = paymentMode,
            paymentStatus = paymentStatus,
            timestamp = System.currentTimeMillis(),
            notes = notes
        )

        val saleId = dao.insertSaleTransaction(sale)

        // Save sale items and deduct inventory stock
        val itemsWithSaleId = items.map { it.copy(saleId = saleId) }
        dao.insertSaleItems(itemsWithSaleId)

        items.forEach { item ->
            dao.deductBatchStock(item.batchId, item.quantity)
        }

        // If customer exists and there is balance due, add to customer outstanding ledger
        if (customerId != null && balanceDue > 0) {
            dao.addCustomerCredit(customerId, balanceDue)
        }

        return saleId
    }

    // Complete Purchase Stock Inward Workflow
    suspend fun processPurchase(
        supplierId: Long?,
        supplierName: String,
        items: List<PurchaseItemInput>,
        paidAmount: Double,
        notes: String
    ): Long {
        val totalAmount = items.sumOf { it.purchaseRate * it.quantity }
        val balancePayable = maxOf(0.0, totalAmount - paidAmount)
        val billNumber = "PO-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())

        val purchase = PurchaseTransaction(
            billNumber = billNumber,
            supplierId = supplierId,
            supplierName = supplierName.ifBlank { "Direct Vendor" },
            totalAmount = totalAmount,
            paidAmount = paidAmount,
            balancePayable = balancePayable,
            timestamp = System.currentTimeMillis(),
            notes = notes
        )

        val purchaseId = dao.insertPurchaseTransaction(purchase)

        val purchaseItems = items.map { item ->
            // Insert or update batch stock
            val epochMs = try {
                val parts = item.expiryFormatted.split("-")
                val cal = Calendar.getInstance()
                if (parts.size >= 2) {
                    val year = parts[0].toIntOrNull() ?: 2026
                    val month = (parts[1].toIntOrNull() ?: 1) - 1
                    val day = if (parts.size >= 3) parts[2].toIntOrNull() ?: 1 else 1
                    cal.set(year, month, day, 23, 59, 59)
                }
                cal.timeInMillis
            } catch (e: Exception) {
                System.currentTimeMillis() + 86400000L * 365
            }

            dao.insertBatch(
                Batch(
                    medicineId = item.medicineId,
                    medicineName = item.medicineName,
                    batchNumber = item.batchNumber,
                    expiryDateEpochMs = epochMs,
                    expiryDateFormatted = item.expiryFormatted,
                    purchasePrice = item.purchaseRate,
                    mrp = item.mrp,
                    salePrice = item.mrp * 0.95, // standard wholesale margin default
                    currentStock = item.quantity,
                    supplierName = supplierName
                )
            )

            PurchaseItem(
                purchaseId = purchaseId,
                medicineId = item.medicineId,
                medicineName = item.medicineName,
                batchNumber = item.batchNumber,
                expiryFormatted = item.expiryFormatted,
                purchaseRate = item.purchaseRate,
                mrp = item.mrp,
                quantity = item.quantity,
                totalPrice = item.purchaseRate * item.quantity
            )
        }

        dao.insertPurchaseItems(purchaseItems)
        return purchaseId
    }

    // Record Debt Recovery Payment
    suspend fun recordRecoveryPayment(
        customerId: Long,
        customerName: String,
        customerPhone: String,
        invoiceNumber: String,
        amountPaid: Double,
        paymentMode: String,
        referenceNumber: String,
        notes: String
    ): Long {
        val receiptNumber = "REC-" + SimpleDateFormat("yyMMdd-HHmm", Locale.US).format(Date())
        val payment = RecoveryPayment(
            receiptNumber = receiptNumber,
            customerId = customerId,
            customerName = customerName,
            customerPhone = customerPhone,
            invoiceNumber = invoiceNumber,
            amountPaid = amountPaid,
            paymentMode = paymentMode,
            referenceNumber = referenceNumber,
            timestamp = System.currentTimeMillis(),
            notes = notes
        )

        val paymentId = dao.insertRecoveryPayment(payment)
        dao.deductCustomerCredit(customerId, amountPaid, System.currentTimeMillis())
        return paymentId
    }

    // Process Return (Customer Return or Supplier Expiry Return)
    suspend fun processReturn(
        returnType: String, // CUSTOMER_RETURN or SUPPLIER_RETURN_EXPIRY / SUPPLIER_RETURN_DAMAGE
        partyName: String,
        items: List<ReturnItemInput>,
        reason: String,
        notes: String,
        customerIdForCreditRefund: Long? = null
    ): Long {
        val returnNumber = "RET-" + SimpleDateFormat("yyMMdd-HHmmss", Locale.US).format(Date())
        val totalRefund = items.sumOf { it.unitRate * it.quantity }

        val returnTx = ReturnTransaction(
            returnNumber = returnNumber,
            returnType = returnType,
            partyName = partyName,
            totalRefundAmount = totalRefund,
            status = "PROCESSED",
            reason = reason,
            timestamp = System.currentTimeMillis(),
            notes = notes
        )

        val returnId = dao.insertReturnTransaction(returnTx)

        val returnItems = items.map { item ->
            // If restockAction is RESTOCK_INVENTORY, add back to batch stock
            if (item.restockAction == "RESTOCK_INVENTORY" && item.batchId != null) {
                dao.incrementBatchStock(item.batchId, item.quantity)
            } else if (item.restockAction == "RETURN_TO_SUPPLIER" && item.batchId != null) {
                // If returning to supplier, deduct from active pharmacy inventory
                dao.deductBatchStock(item.batchId, item.quantity)
            }

            ReturnItem(
                returnId = returnId,
                medicineId = item.medicineId,
                medicineName = item.medicineName,
                batchNumber = item.batchNumber,
                expiryFormatted = item.expiryFormatted,
                unitRate = item.unitRate,
                quantity = item.quantity,
                refundAmount = item.unitRate * item.quantity,
                restockAction = item.restockAction
            )
        }

        dao.insertReturnItems(returnItems)

        // If customer return and credit adjust was requested
        if (returnType == "CUSTOMER_RETURN" && customerIdForCreditRefund != null) {
            dao.deductCustomerCredit(customerIdForCreditRefund, totalRefund, System.currentTimeMillis())
        }

        return returnId
    }

    // AI Helpers
    suspend fun askAiPharma(prompt: String): String = aiService.generatePharmaInsight(prompt)

    suspend fun getAiRecoveryReminder(customerName: String, amount: Double, daysOverdue: Int, invoiceNo: String, tone: String): String {
        return aiService.generateRecoveryReminder(customerName, amount, daysOverdue, invoiceNo, tone)
    }

    suspend fun findGenericSubstitutes(query: String): String = aiService.findGenericSubstitutes(query)

    suspend fun analyzeExpiryRisk(medicineName: String, batchNo: String, stock: Int, daysToExpiry: Int): String {
        return aiService.analyzeExpiryRisk(medicineName, batchNo, stock, daysToExpiry)
    }
}

data class PurchaseItemInput(
    val medicineId: Long,
    val medicineName: String,
    val batchNumber: String,
    val expiryFormatted: String,
    val purchaseRate: Double,
    val mrp: Double,
    val quantity: Int
)

data class ReturnItemInput(
    val batchId: Long? = null,
    val medicineId: Long,
    val medicineName: String,
    val batchNumber: String,
    val expiryFormatted: String,
    val unitRate: Double,
    val quantity: Int,
    val restockAction: String // DISCARD_DESTROY, RETURN_TO_SUPPLIER, RESTOCK_INVENTORY
)
