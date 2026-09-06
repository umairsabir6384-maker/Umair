package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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

@Dao
interface PharmaDao {

    // --- Medicines ---
    @Query("SELECT * FROM medicines ORDER BY name ASC")
    fun getAllMedicines(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE name LIKE '%' || :query || '%' OR genericFormula LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    fun searchMedicines(query: String): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getMedicineById(id: Long): Medicine?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: Medicine): Long

    @Update
    suspend fun updateMedicine(medicine: Medicine)

    @Delete
    suspend fun deleteMedicine(medicine: Medicine)

    @Query("DELETE FROM batches WHERE medicineId = :medicineId")
    suspend fun deleteBatchesByMedicineId(medicineId: Long)

    // --- Batches & Inventory (FEFO Support) ---
    @Query("SELECT * FROM batches ORDER BY expiryDateEpochMs ASC")
    fun getAllBatches(): Flow<List<Batch>>

    @Query("SELECT * FROM batches WHERE medicineId = :medicineId AND currentStock > 0 ORDER BY expiryDateEpochMs ASC")
    fun getActiveBatchesForMedicine(medicineId: Long): Flow<List<Batch>>

    @Query("SELECT * FROM batches WHERE medicineId = :medicineId AND currentStock > 0 ORDER BY expiryDateEpochMs ASC")
    suspend fun getActiveBatchesForMedicineSync(medicineId: Long): List<Batch>

    @Query("SELECT * FROM batches WHERE id = :batchId")
    suspend fun getBatchById(batchId: Long): Batch?

    @Query("SELECT * FROM batches WHERE expiryDateEpochMs <= :thresholdMs AND currentStock > 0 ORDER BY expiryDateEpochMs ASC")
    fun getBatchesExpiringBefore(thresholdMs: Long): Flow<List<Batch>>

    @Query("SELECT * FROM batches WHERE expiryDateEpochMs < :currentMs AND currentStock > 0")
    fun getExpiredBatches(currentMs: Long): Flow<List<Batch>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: Batch): Long

    @Update
    suspend fun updateBatch(batch: Batch)

    @Query("UPDATE batches SET currentStock = currentStock - :quantity WHERE id = :batchId")
    suspend fun deductBatchStock(batchId: Long, quantity: Int)

    @Query("UPDATE batches SET currentStock = currentStock + :quantity WHERE id = :batchId")
    suspend fun incrementBatchStock(batchId: Long, quantity: Int)

    @Query("DELETE FROM batches WHERE id = :batchId")
    suspend fun deleteBatch(batchId: Long)

    // --- Customers & Recovery ---
    @Query("SELECT * FROM customers ORDER BY outstandingBalance DESC, name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE outstandingBalance > 0 ORDER BY outstandingBalance DESC")
    fun getCustomersWithOutstandingDues(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("DELETE FROM customers WHERE id = :customerId")
    suspend fun deleteCustomerById(customerId: Long)

    @Query("UPDATE customers SET outstandingBalance = outstandingBalance + :amount WHERE id = :customerId")
    suspend fun addCustomerCredit(customerId: Long, amount: Double)

    @Query("UPDATE customers SET outstandingBalance = MAX(0.0, outstandingBalance - :amount), lastPaymentDate = :paymentTime WHERE id = :customerId")
    suspend fun deductCustomerCredit(customerId: Long, amount: Double, paymentTime: Long)

    // --- Suppliers ---
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier): Long

    @Update
    suspend fun updateSupplier(supplier: Supplier)

    // --- Sales Transactions ---
    @Query("SELECT * FROM sale_transactions ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<SaleTransaction>>

    @Query("SELECT * FROM sale_transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getSalesForCustomer(customerId: Long): Flow<List<SaleTransaction>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun getItemsForSale(saleId: Long): Flow<List<SaleItem>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSaleSync(saleId: Long): List<SaleItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleTransaction(sale: SaleTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItem>)

    // --- Purchases ---
    @Query("SELECT * FROM purchase_transactions ORDER BY timestamp DESC")
    fun getAllPurchases(): Flow<List<PurchaseTransaction>>

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId")
    fun getItemsForPurchase(purchaseId: Long): Flow<List<PurchaseItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseTransaction(purchase: PurchaseTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseItems(items: List<PurchaseItem>)

    // --- Recovery Payments ---
    @Query("SELECT * FROM recovery_payments ORDER BY timestamp DESC")
    fun getAllRecoveryPayments(): Flow<List<RecoveryPayment>>

    @Query("SELECT * FROM recovery_payments WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getPaymentsForCustomer(customerId: Long): Flow<List<RecoveryPayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecoveryPayment(payment: RecoveryPayment): Long

    // --- Return Transactions ---
    @Query("SELECT * FROM return_transactions ORDER BY timestamp DESC")
    fun getAllReturns(): Flow<List<ReturnTransaction>>

    @Query("SELECT * FROM return_items WHERE returnId = :returnId")
    fun getItemsForReturn(returnId: Long): Flow<List<ReturnItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnTransaction(returnTx: ReturnTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnItems(items: List<ReturnItem>)

    // --- Aggregated Stats ---
    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM sale_transactions")
    fun getTotalSalesSum(): Flow<Double>

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM purchase_transactions")
    fun getTotalPurchasesSum(): Flow<Double>

    @Query("SELECT COALESCE(SUM(outstandingBalance), 0.0) FROM customers")
    fun getTotalOutstandingRecoveries(): Flow<Double>

    @Query("SELECT COALESCE(SUM(totalRefundAmount), 0.0) FROM return_transactions")
    fun getTotalReturnsSum(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amountPaid), 0.0) FROM recovery_payments")
    fun getTotalRecoveredAmount(): Flow<Double>
}
