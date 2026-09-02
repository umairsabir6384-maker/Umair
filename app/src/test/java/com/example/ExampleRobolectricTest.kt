package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.GeminiPharmaService
import com.example.data.local.AppDatabase
import com.example.data.local.PharmaDao
import com.example.data.model.Batch
import com.example.data.model.Customer
import com.example.data.model.Medicine
import com.example.data.model.SaleItem
import com.example.data.repository.PharmaRepository
import com.example.data.repository.PurchaseItemInput
import com.example.data.repository.ReturnItemInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: PharmaDao
    private lateinit var repository: PharmaRepository
    private lateinit var aiService: GeminiPharmaService

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.pharmaDao()
        aiService = GeminiPharmaService()
        repository = PharmaRepository(dao, aiService)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `verify app name resource`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PharmaFlow AI", appName)
    }

    @Test
    fun `test complete sales and inventory deduction flow`() = runBlocking {
        val medId = dao.insertMedicine(
            Medicine(
                name = "Augmentin 625mg",
                genericFormula = "Amoxicillin 500mg + Clavulanic Acid 125mg",
                category = "Antibiotic",
                dosageForm = "Tablet",
                manufacturer = "GSK",
                minStockLevel = 20,
                locationRack = "Rack A-01"
            )
        )

        val batchId = dao.insertBatch(
            Batch(
                medicineId = medId,
                medicineName = "Augmentin 625mg",
                batchNumber = "AUG-901",
                expiryDateEpochMs = System.currentTimeMillis() + 86400000L * 90,
                expiryDateFormatted = "2026-12-31",
                purchasePrice = 12.00,
                mrp = 18.00,
                salePrice = 17.50,
                currentStock = 100,
                supplierName = "Premier Pharma"
            )
        )

        val custId = dao.insertCustomer(
            Customer(
                name = "Dr. Robert Smith",
                phone = "+1 555 0192",
                address = "St. Jude Clinic",
                outstandingBalance = 0.0,
                creditLimit = 1000.0,
                daysOverdue = 0
            )
        )

        // Process Sale of 10 units on partial credit
        val saleItems = listOf(
            SaleItem(
                saleId = 0,
                medicineId = medId,
                medicineName = "Augmentin 625mg",
                batchId = batchId,
                batchNumber = "AUG-901",
                expiryFormatted = "2026-12-31",
                unitPrice = 17.50,
                quantity = 10,
                totalPrice = 175.00
            )
        )

        repository.processSale(
            customerId = custId,
            customerName = "Dr. Robert Smith",
            customerPhone = "+1 555 0192",
            items = saleItems,
            discountPercent = 10.0,
            taxPercent = 5.0,
            paidAmount = 50.0, // Partial paid
            paymentMode = "CASH",
            notes = "Clinic regular order"
        )

        // Verify batch stock reduced from 100 to 90
        val batches = dao.getAllBatches().first()
        val updatedBatch = batches.first { it.id == batchId }
        assertEquals(90, updatedBatch.currentStock)

        // Verify customer credit updated with remaining due balance
        val customers = dao.getAllCustomers().first()
        val updatedCust = customers.first { it.id == custId }
        assertTrue(updatedCust.outstandingBalance > 0)
    }

    @Test
    fun `test debt recovery recording flow`() = runBlocking {
        val custId = dao.insertCustomer(
            Customer(
                name = "City Care Hospital",
                phone = "+1 555 3301",
                address = "Downtown",
                outstandingBalance = 500.0,
                creditLimit = 2000.0,
                daysOverdue = 45
            )
        )

        repository.recordRecoveryPayment(
            customerId = custId,
            customerName = "City Care Hospital",
            customerPhone = "+1 555 3301",
            invoiceNumber = "INV-OLD-99",
            amountPaid = 300.0,
            paymentMode = "BANK_TRANSFER",
            referenceNumber = "TRX-BANK-001",
            notes = "Partial settlement"
        )

        val updatedCust = dao.getAllCustomers().first().first { it.id == custId }
        assertEquals(200.0, updatedCust.outstandingBalance, 0.01)
    }

    @Test
    fun `test supplier expiry return claim flow`() = runBlocking {
        val medId = dao.insertMedicine(
            Medicine(
                name = "Amoxil 500mg",
                genericFormula = "Amoxicillin",
                category = "Antibiotic",
                dosageForm = "Capsule",
                manufacturer = "GSK",
                minStockLevel = 10,
                locationRack = "Rack B-02"
            )
        )

        val batchId = dao.insertBatch(
            Batch(
                medicineId = medId,
                medicineName = "Amoxil 500mg",
                batchNumber = "AMX-EXP-01",
                expiryDateEpochMs = System.currentTimeMillis() - 86400000L, // Expired
                expiryDateFormatted = "2026-08-01",
                purchasePrice = 8.50,
                mrp = 14.00,
                salePrice = 13.00,
                currentStock = 25,
                supplierName = "Apex Pharma Supply"
            )
        )

        // Process Supplier Return Claim for expired batch
        repository.processReturn(
            returnType = "SUPPLIER_RETURN_EXPIRY",
            partyName = "Apex Pharma Supply",
            items = listOf(
                ReturnItemInput(
                    batchId = batchId,
                    medicineId = medId,
                    medicineName = "Amoxil 500mg",
                    batchNumber = "AMX-EXP-01",
                    expiryFormatted = "2026-08-01",
                    unitRate = 8.50,
                    quantity = 25,
                    restockAction = "RETURN_TO_SUPPLIER"
                )
            ),
            reason = "EXPIRED_LOT",
            notes = "Debit note requested"
        )

        // Stock in pharmacy should now be 0 after returning to supplier
        val updatedBatch = dao.getAllBatches().first().first { it.id == batchId }
        assertEquals(0, updatedBatch.currentStock)
    }

    @Test
    fun `test AI generic substitute and debt recovery advisor`() = runBlocking {
        val substituteAdvice = aiService.findGenericSubstitutes("Augmentin 625mg")
        assertTrue(substituteAdvice.isNotBlank())

        val recoveryNotice = aiService.generateRecoveryReminder(
            customerName = "Metro Care Clinic",
            amount = 450.0,
            daysOverdue = 45,
            invoiceNo = "INV-7801",
            tone = "Firm"
        )
        assertTrue(recoveryNotice.isNotBlank())
    }
}
