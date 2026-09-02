package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [
        Medicine::class,
        Batch::class,
        Customer::class,
        Supplier::class,
        SaleTransaction::class,
        SaleItem::class,
        PurchaseTransaction::class,
        PurchaseItem::class,
        RecoveryPayment::class,
        ReturnTransaction::class,
        ReturnItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pharmaDao(): PharmaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pharma_flow_db"
                )
                    .addCallback(DatabaseSeedCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseSeedCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        seedDatabase(database.pharmaDao())
                    }
                }
            }
        }

        private suspend fun seedDatabase(dao: PharmaDao) {
            // Suppliers
            val s1 = dao.insertSupplier(Supplier(name = "Premier Pharma Wholesale", contactPerson = "Kamran Ali", phone = "+1 (555) 234-5678", email = "orders@premierpharma.com", address = "Industrial Estate, Sector 4", outstandingPayable = 45000.0))
            val s2 = dao.insertSupplier(Supplier(name = "Global Med Logistics", contactPerson = "Elena Vance", phone = "+1 (555) 876-5432", email = "elena@globalmed.com", address = "Logistics Hub, North Bay", outstandingPayable = 18500.0))
            val s3 = dao.insertSupplier(Supplier(name = "Apex Life Sciences", contactPerson = "Rashid Mahmood", phone = "+1 (555) 345-9876", email = "supply@apexlifesciences.com", address = "Pharma Plaza, Suite 201", outstandingPayable = 0.0))

            // Customers
            val c1 = dao.insertCustomer(Customer(name = "Al-Shifa Healthcare Clinic", phone = "+1 (555) 441-2233", address = "Central Ave #42", creditLimit = 100000.0, outstandingBalance = 42500.0, daysOverdue = 45))
            val c2 = dao.insertCustomer(Customer(name = "Dr. Sarah Khan (Cardio Care)", phone = "+1 (555) 782-9900", address = "Doctors Colony Blk 3", creditLimit = 50000.0, outstandingBalance = 15200.0, daysOverdue = 22))
            val c3 = dao.insertCustomer(Customer(name = "City Care Hospital Pharmacy", phone = "+1 (555) 671-3344", address = "East Ring Road #10", creditLimit = 150000.0, outstandingBalance = 68000.0, daysOverdue = 65))
            val c4 = dao.insertCustomer(Customer(name = "Walk-in Retail Customer", phone = "+1 (555) 000-0000", address = "Counter", creditLimit = 0.0, outstandingBalance = 0.0, daysOverdue = 0))
            val c5 = dao.insertCustomer(Customer(name = "Family Health Centre", phone = "+1 (555) 912-4455", address = "Downtown Square 8", creditLimit = 60000.0, outstandingBalance = 8400.0, daysOverdue = 12))

            // Medicines
            val m1 = dao.insertMedicine(Medicine(name = "Augmentin 625mg", genericFormula = "Amoxicillin + Clavulanic Acid", category = "Antibiotic", dosageForm = "Tablet", manufacturer = "GSK", minStockLevel = 25, locationRack = "Rack A-01", description = "Broad spectrum antibiotic for bacterial infections"))
            val m2 = dao.insertMedicine(Medicine(name = "Panadol Extra 500mg", genericFormula = "Paracetamol + Caffeine", category = "Analgesic", dosageForm = "Tablet", manufacturer = "Haleon", minStockLevel = 50, locationRack = "Rack A-02", description = "Fast relief from tough headaches and body aches"))
            val m3 = dao.insertMedicine(Medicine(name = "Glucophage 500mg", genericFormula = "Metformin Hydrochloride", category = "Antidiabetic", dosageForm = "Tablet", manufacturer = "Merck", minStockLevel = 30, locationRack = "Rack B-01", description = "First-line medication for type 2 diabetes management"))
            val m4 = dao.insertMedicine(Medicine(name = "Losec 20mg", genericFormula = "Omeprazole", category = "Gastrointestinal", dosageForm = "Capsule", manufacturer = "AstraZeneca", minStockLevel = 20, locationRack = "Rack B-02", description = "Proton pump inhibitor for acid reflux and GERD"))
            val m5 = dao.insertMedicine(Medicine(name = "Zithromax 500mg", genericFormula = "Azithromycin", category = "Antibiotic", dosageForm = "Tablet", manufacturer = "Pfizer", minStockLevel = 15, locationRack = "Rack A-03", description = "Macrolide antibiotic for respiratory tract infections"))
            val m6 = dao.insertMedicine(Medicine(name = "Brufen 400mg", genericFormula = "Ibuprofen", category = "Anti-inflammatory", dosageForm = "Tablet", manufacturer = "Abbott", minStockLevel = 40, locationRack = "Rack C-01", description = "Nonsteroidal anti-inflammatory drug (NSAID)"))
            val m7 = dao.insertMedicine(Medicine(name = "Ventolin Evohaler 100mcg", genericFormula = "Salbutamol Sulfate", category = "Respiratory", dosageForm = "Inhaler", manufacturer = "GSK", minStockLevel = 15, locationRack = "Rack C-03", description = "Fast-acting bronchodilator for asthma relief"))
            val m8 = dao.insertMedicine(Medicine(name = "Zyrtec 10mg", genericFormula = "Cetirizine HCl", category = "Antihistamine", dosageForm = "Tablet", manufacturer = "UCB Pharma", minStockLevel = 25, locationRack = "Rack D-01", description = "Allergy and hay fever symptom relief"))
            val m9 = dao.insertMedicine(Medicine(name = "Ciprobay 500mg", genericFormula = "Ciprofloxacin", category = "Antibiotic", dosageForm = "Tablet", manufacturer = "Bayer", minStockLevel = 15, locationRack = "Rack A-04", description = "Fluoroquinolone antibiotic"))

            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()

            // Calculate dates
            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, 25) // Expiring soon in 25 days!
            val expSoonEpoch1 = cal.timeInMillis
            val expSoonFmt1 = "${cal.get(Calendar.YEAR)}-${String.format("%02d", cal.get(Calendar.MONTH) + 1)}-${String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))}"

            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, 45) // Expiring in 45 days
            val expSoonEpoch2 = cal.timeInMillis
            val expSoonFmt2 = "${cal.get(Calendar.YEAR)}-${String.format("%02d", cal.get(Calendar.MONTH) + 1)}-${String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))}"

            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, 380) // Long expiry
            val expGoodEpoch1 = cal.timeInMillis
            val expGoodFmt1 = "${cal.get(Calendar.YEAR)}-${String.format("%02d", cal.get(Calendar.MONTH) + 1)}-${String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))}"

            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, 540)
            val expGoodEpoch2 = cal.timeInMillis
            val expGoodFmt2 = "${cal.get(Calendar.YEAR)}-${String.format("%02d", cal.get(Calendar.MONTH) + 1)}-${String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))}"

            // Expired batch (for demonstration of return claims)
            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, -15)
            val expiredEpoch = cal.timeInMillis
            val expiredFmt = "${cal.get(Calendar.YEAR)}-${String.format("%02d", cal.get(Calendar.MONTH) + 1)}-${String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))}"

            // Batches (FEFO indexed)
            dao.insertBatch(Batch(medicineId = m1, medicineName = "Augmentin 625mg", batchNumber = "AUG-88210", expiryDateEpochMs = expSoonEpoch1, expiryDateFormatted = expSoonFmt1, purchasePrice = 14.50, mrp = 22.00, salePrice = 20.00, currentStock = 18, supplierName = "Premier Pharma Wholesale"))
            dao.insertBatch(Batch(medicineId = m1, medicineName = "Augmentin 625mg", batchNumber = "AUG-99304", expiryDateEpochMs = expGoodEpoch1, expiryDateFormatted = expGoodFmt1, purchasePrice = 15.00, mrp = 22.50, salePrice = 21.00, currentStock = 65, supplierName = "Premier Pharma Wholesale"))

            dao.insertBatch(Batch(medicineId = m2, medicineName = "Panadol Extra 500mg", batchNumber = "PAN-44012", expiryDateEpochMs = expGoodEpoch2, expiryDateFormatted = expGoodFmt2, purchasePrice = 2.10, mrp = 3.50, salePrice = 3.20, currentStock = 140, supplierName = "Global Med Logistics"))
            dao.insertBatch(Batch(medicineId = m3, medicineName = "Glucophage 500mg", batchNumber = "GLU-31090", expiryDateEpochMs = expSoonEpoch2, expiryDateFormatted = expSoonFmt2, purchasePrice = 4.80, mrp = 7.50, salePrice = 6.90, currentStock = 42, supplierName = "Apex Life Sciences"))
            dao.insertBatch(Batch(medicineId = m3, medicineName = "Glucophage 500mg", batchNumber = "GLU-77211", expiryDateEpochMs = expGoodEpoch1, expiryDateFormatted = expGoodFmt1, purchasePrice = 5.00, mrp = 7.80, salePrice = 7.20, currentStock = 80, supplierName = "Apex Life Sciences"))

            dao.insertBatch(Batch(medicineId = m4, medicineName = "Losec 20mg", batchNumber = "LOS-12003", expiryDateEpochMs = expGoodEpoch1, expiryDateFormatted = expGoodFmt1, purchasePrice = 12.00, mrp = 18.00, salePrice = 16.50, currentStock = 35, supplierName = "Premier Pharma Wholesale"))
            dao.insertBatch(Batch(medicineId = m5, medicineName = "Zithromax 500mg", batchNumber = "ZTH-90214", expiryDateEpochMs = expSoonEpoch1, expiryDateFormatted = expSoonFmt1, purchasePrice = 18.50, mrp = 28.00, salePrice = 26.00, currentStock = 12, supplierName = "Global Med Logistics"))
            dao.insertBatch(Batch(medicineId = m6, medicineName = "Brufen 400mg", batchNumber = "BRF-66100", expiryDateEpochMs = expGoodEpoch2, expiryDateFormatted = expGoodFmt2, purchasePrice = 3.20, mrp = 5.00, salePrice = 4.50, currentStock = 95, supplierName = "Apex Life Sciences"))
            dao.insertBatch(Batch(medicineId = m7, medicineName = "Ventolin Evohaler 100mcg", batchNumber = "VEN-55410", expiryDateEpochMs = expGoodEpoch1, expiryDateFormatted = expGoodFmt1, purchasePrice = 8.50, mrp = 14.00, salePrice = 12.50, currentStock = 28, supplierName = "Premier Pharma Wholesale"))
            dao.insertBatch(Batch(medicineId = m8, medicineName = "Zyrtec 10mg", batchNumber = "ZYR-78190", expiryDateEpochMs = expiredEpoch, expiryDateFormatted = expiredFmt, purchasePrice = 6.00, mrp = 9.50, salePrice = 8.50, currentStock = 15, supplierName = "Global Med Logistics"))

            // Sample Sale Transactions
            val sId1 = dao.insertSaleTransaction(
                SaleTransaction(
                    invoiceNumber = "INV-2026-1001",
                    customerId = c1,
                    customerName = "Al-Shifa Healthcare Clinic",
                    customerPhone = "+1 (555) 441-2233",
                    totalAmount = 650.00,
                    discountPercent = 5.0,
                    discountAmount = 32.50,
                    taxPercent = 0.0,
                    taxAmount = 0.0,
                    netPayable = 617.50,
                    paidAmount = 200.00,
                    balanceDue = 417.50,
                    paymentMode = "CREDIT",
                    paymentStatus = "PARTIAL",
                    timestamp = now - 86400000L * 3,
                    notes = "Monthly OPD bulk supply"
                )
            )
            dao.insertSaleItems(
                listOf(
                    SaleItem(saleId = sId1, medicineId = m1, medicineName = "Augmentin 625mg", batchId = 1, batchNumber = "AUG-88210", expiryFormatted = expSoonFmt1, unitPrice = 20.0, quantity = 20, totalPrice = 400.0),
                    SaleItem(saleId = sId1, medicineId = m2, medicineName = "Panadol Extra 500mg", batchId = 3, batchNumber = "PAN-44012", expiryFormatted = expGoodFmt2, unitPrice = 3.2, quantity = 50, totalPrice = 160.0),
                    SaleItem(saleId = sId1, medicineId = m6, medicineName = "Brufen 400mg", batchId = 8, batchNumber = "BRF-66100", expiryFormatted = expGoodFmt2, unitPrice = 4.5, quantity = 20, totalPrice = 90.0)
                )
            )

            val sId2 = dao.insertSaleTransaction(
                SaleTransaction(
                    invoiceNumber = "INV-2026-1002",
                    customerId = c4,
                    customerName = "Walk-in Retail Customer",
                    customerPhone = "",
                    totalAmount = 56.50,
                    discountPercent = 0.0,
                    discountAmount = 0.0,
                    taxPercent = 0.0,
                    taxAmount = 0.0,
                    netPayable = 56.50,
                    paidAmount = 56.50,
                    balanceDue = 0.0,
                    paymentMode = "CASH",
                    paymentStatus = "PAID",
                    timestamp = now - 86400000L * 1,
                    notes = "Prescription counter checkout"
                )
            )
            dao.insertSaleItems(
                listOf(
                    SaleItem(saleId = sId2, medicineId = m4, medicineName = "Losec 20mg", batchId = 6, batchNumber = "LOS-12003", expiryFormatted = expGoodFmt1, unitPrice = 16.5, quantity = 2, totalPrice = 33.0),
                    SaleItem(saleId = sId2, medicineId = m7, medicineName = "Ventolin Evohaler 100mcg", batchId = 9, batchNumber = "VEN-55410", expiryFormatted = expGoodFmt1, unitPrice = 12.5, quantity = 1, totalPrice = 12.5),
                    SaleItem(saleId = sId2, medicineId = m2, medicineName = "Panadol Extra 500mg", batchId = 3, batchNumber = "PAN-44012", expiryFormatted = expGoodFmt2, unitPrice = 3.2, quantity = 3, totalPrice = 9.6)
                )
            )

            // Recovery Payment record
            dao.insertRecoveryPayment(
                RecoveryPayment(
                    receiptNumber = "REC-88401",
                    customerId = c1,
                    customerName = "Al-Shifa Healthcare Clinic",
                    customerPhone = "+1 (555) 441-2233",
                    invoiceNumber = "INV-2026-1001",
                    amountPaid = 15000.0,
                    paymentMode = "BANK_TRANSFER",
                    referenceNumber = "FT-99482103",
                    timestamp = now - 86400000L * 2,
                    notes = "Partial recovery against due ledger"
                )
            )

            // Return Transaction
            val retId = dao.insertReturnTransaction(
                ReturnTransaction(
                    returnNumber = "RET-2026-0012",
                    returnType = "SUPPLIER_RETURN_EXPIRY",
                    partyName = "Global Med Logistics",
                    totalRefundAmount = 90.00,
                    status = "PROCESSED",
                    reason = "EXPIRED_STOCK",
                    timestamp = now - 86400000L * 1,
                    notes = "Expired batch credit claim returned to vendor"
                )
            )
            dao.insertReturnItems(
                listOf(
                    ReturnItem(
                        returnId = retId,
                        medicineId = m8,
                        medicineName = "Zyrtec 10mg",
                        batchNumber = "ZYR-78190",
                        expiryFormatted = expiredFmt,
                        unitRate = 6.0,
                        quantity = 15,
                        refundAmount = 90.0,
                        restockAction = "RETURN_TO_SUPPLIER"
                    )
                )
            )
        }
    }
}
