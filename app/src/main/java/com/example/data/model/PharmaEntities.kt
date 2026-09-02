package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val genericFormula: String,
    val category: String,
    val dosageForm: String, // Tablet, Syrup, Injection, Capsule, etc.
    val manufacturer: String,
    val minStockLevel: Int = 10,
    val locationRack: String = "Rack A-01",
    val description: String = ""
)

@Entity(tableName = "batches")
data class Batch(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val medicineName: String,
    val batchNumber: String,
    val expiryDateEpochMs: Long,
    val expiryDateFormatted: String, // e.g. "2026-11-30"
    val purchasePrice: Double,
    val mrp: Double,
    val salePrice: Double,
    val currentStock: Int,
    val supplierName: String = ""
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val address: String = "",
    val creditLimit: Double = 50000.0,
    val outstandingBalance: Double = 0.0,
    val lastPaymentDate: Long = System.currentTimeMillis(),
    val daysOverdue: Int = 0
)

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val contactPerson: String = "",
    val phone: String,
    val email: String = "",
    val address: String = "",
    val outstandingPayable: Double = 0.0
)

@Entity(tableName = "sale_transactions")
data class SaleTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val customerId: Long? = null,
    val customerName: String,
    val customerPhone: String = "",
    val totalAmount: Double,
    val discountPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxPercent: Double = 0.0,
    val taxAmount: Double = 0.0,
    val netPayable: Double,
    val paidAmount: Double,
    val balanceDue: Double,
    val paymentMode: String, // CASH, CREDIT, CARD, UPI_BANK
    val paymentStatus: String, // PAID, PARTIAL, DUE
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "sale_items")
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val medicineId: Long,
    val medicineName: String,
    val batchId: Long,
    val batchNumber: String,
    val expiryFormatted: String,
    val unitPrice: Double,
    val quantity: Int,
    val totalPrice: Double
)

@Entity(tableName = "purchase_transactions")
data class PurchaseTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billNumber: String,
    val supplierId: Long? = null,
    val supplierName: String,
    val totalAmount: Double,
    val paidAmount: Double,
    val balancePayable: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "purchase_items")
data class PurchaseItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long,
    val medicineId: Long,
    val medicineName: String,
    val batchNumber: String,
    val expiryFormatted: String,
    val purchaseRate: Double,
    val mrp: Double,
    val quantity: Int,
    val totalPrice: Double
)

@Entity(tableName = "recovery_payments")
data class RecoveryPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptNumber: String,
    val customerId: Long,
    val customerName: String,
    val customerPhone: String = "",
    val invoiceNumber: String = "",
    val amountPaid: Double,
    val paymentMode: String = "CASH", // CASH, BANK_TRANSFER, CHEQUE, ONLINE
    val referenceNumber: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "return_transactions")
data class ReturnTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val returnNumber: String,
    val returnType: String, // CUSTOMER_RETURN, SUPPLIER_RETURN_EXPIRY, SUPPLIER_RETURN_DAMAGE, BATCH_RECALL
    val partyName: String, // Customer or Supplier Name
    val totalRefundAmount: Double,
    val status: String = "PROCESSED", // PROCESSED, PENDING_CLAIM, CREDITED
    val reason: String = "EXPIRED_STOCK", // EXPIRED_STOCK, DAMAGED_PACKAGING, CUSTOMER_EXCHANGE, MANUFACTURER_RECALL
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "return_items")
data class ReturnItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val returnId: Long,
    val medicineId: Long,
    val medicineName: String,
    val batchNumber: String,
    val expiryFormatted: String,
    val unitRate: Double,
    val quantity: Int,
    val refundAmount: Double,
    val restockAction: String // DISCARD_DESTROY, RETURN_TO_SUPPLIER, RESTOCK_INVENTORY
)
