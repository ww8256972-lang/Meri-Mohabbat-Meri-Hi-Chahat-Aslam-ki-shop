package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.db.AslamDatabase
import com.example.data.model.CustomerEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.MovementType
import com.example.data.model.PaymentEntity
import com.example.data.model.PaymentMode
import com.example.data.model.PaymentStatus
import com.example.data.model.ProductEntity
import com.example.data.model.PurchaseEntity
import com.example.data.model.PurchaseItemEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.SaleEntity
import com.example.data.model.SaleItemEntity
import com.example.data.model.ShopProfile
import com.example.data.model.StockMovementEntity
import com.example.data.model.SupplierEntity
import com.example.domain.FinancialReport
import com.example.domain.TopProductSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ShopRepository(private val db: AslamDatabase) {

    // --- Product & Inventory ---
    val allActiveProducts: Flow<List<ProductEntity>> = db.productDao().getAllActiveProducts()
    val allProducts: Flow<List<ProductEntity>> = db.productDao().getAllProducts()
    val lowStockProducts: Flow<List<ProductEntity>> = db.productDao().getLowStockProducts()
    val allStockMovements: Flow<List<StockMovementEntity>> = db.stockMovementDao().getAllMovements()

    fun searchProducts(query: String): Flow<List<ProductEntity>> = db.productDao().searchProducts(query)

    suspend fun saveProduct(product: ProductEntity): Long = withContext(Dispatchers.IO) {
        if (product.id == 0L) {
            val id = db.productDao().insertProduct(product)
            if (product.currentStock > 0) {
                db.stockMovementDao().insertMovement(
                    StockMovementEntity(
                        productId = id,
                        productName = product.name,
                        type = MovementType.ADJUSTMENT_ADD,
                        quantityChange = product.currentStock,
                        previousStock = 0.0,
                        newStock = product.currentStock,
                        note = "Initial Opening Stock"
                    )
                )
            }
            id
        } else {
            db.productDao().updateProduct(product)
            product.id
        }
    }

    suspend fun adjustStock(
        productId: Long,
        quantityDelta: Double,
        type: MovementType,
        note: String
    ): Boolean = withContext(Dispatchers.IO) {
        db.withTransaction {
            val product = db.productDao().getProductById(productId) ?: return@withTransaction false
            val newStock = (product.currentStock + quantityDelta).coerceAtLeast(0.0)
            db.productDao().updateStock(productId, newStock)
            db.stockMovementDao().insertMovement(
                StockMovementEntity(
                    productId = productId,
                    productName = product.name,
                    type = type,
                    quantityChange = quantityDelta,
                    previousStock = product.currentStock,
                    newStock = newStock,
                    note = note
                )
            )
            true
        }
    }

    suspend fun archiveProduct(id: Long, isArchived: Boolean) = withContext(Dispatchers.IO) {
        db.productDao().setArchived(id, isArchived)
    }

    suspend fun deleteProduct(id: Long) = withContext(Dispatchers.IO) {
        db.productDao().deleteProduct(id)
    }

    // --- Customers & Suppliers (Khata / Parties) ---
    val allCustomers: Flow<List<CustomerEntity>> = db.customerDao().getAllCustomers()
    val customersWithDue: Flow<List<CustomerEntity>> = db.customerDao().getCustomersWithDue()
    val allSuppliers: Flow<List<SupplierEntity>> = db.supplierDao().getAllSuppliers()

    fun searchCustomers(query: String): Flow<List<CustomerEntity>> = db.customerDao().searchCustomers(query)

    suspend fun saveCustomer(customer: CustomerEntity): Long = withContext(Dispatchers.IO) {
        if (customer.id == 0L) {
            db.customerDao().insertCustomer(customer)
        } else {
            db.customerDao().updateCustomer(customer)
            customer.id
        }
    }

    suspend fun saveSupplier(supplier: SupplierEntity): Long = withContext(Dispatchers.IO) {
        if (supplier.id == 0L) {
            db.supplierDao().insertSupplier(supplier)
        } else {
            db.supplierDao().updateSupplier(supplier)
            supplier.id
        }
    }

    // --- Sales & Transactions ---
    val allSales: Flow<List<SaleEntity>> = db.saleDao().getAllSales()
    val salesWithDue: Flow<List<SaleEntity>> = db.saleDao().getSalesWithDue()
    val allSaleItems: Flow<List<SaleItemEntity>> = db.saleDao().getAllSaleItems()

    suspend fun processSale(
        customer: CustomerEntity?,
        items: List<Pair<ProductEntity, Double>>, // Product and quantity sold
        discount: Double,
        paidAmount: Double,
        paymentMode: PaymentMode,
        dueDate: Long?,
        notes: String
    ): Result<SaleEntity> = withContext(Dispatchers.IO) {
        try {
            if (items.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Sale must have at least one item"))

            db.withTransaction {
                val timestamp = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
                val invoiceNumber = "INV-${dateFormat.format(Date(timestamp))}"

                val subtotal = items.sumOf { (product, qty) -> product.sellingPrice * qty }
                val totalAmount = (subtotal - discount).coerceAtLeast(0.0)
                val actualPaid = paidAmount.coerceAtMost(totalAmount).coerceAtLeast(0.0)
                val dueAmount = (totalAmount - actualPaid).coerceAtLeast(0.0)

                val paymentStatus = when {
                    dueAmount <= 0.0 -> PaymentStatus.PAID
                    actualPaid > 0.0 -> PaymentStatus.PARTIAL
                    else -> PaymentStatus.UNPAID
                }

                val sale = SaleEntity(
                    invoiceNumber = invoiceNumber,
                    customerId = customer?.id,
                    customerName = customer?.name ?: "Walk-in Customer",
                    customerPhone = customer?.phone ?: "",
                    subtotal = subtotal,
                    discount = discount,
                    totalAmount = totalAmount,
                    paidAmount = actualPaid,
                    dueAmount = dueAmount,
                    paymentStatus = paymentStatus,
                    paymentMode = paymentMode,
                    dueDate = dueDate,
                    notes = notes,
                    createdAt = timestamp
                )
                val saleId = db.saleDao().insertSale(sale)

                // Save line items and deduct inventory
                val saleItems = items.map { (product, qty) ->
                    val lineTotal = product.sellingPrice * qty
                    val item = SaleItemEntity(
                        saleId = saleId,
                        productId = product.id,
                        productName = product.name,
                        unit = product.unit,
                        quantity = qty,
                        unitCostPrice = product.purchaseCost,
                        unitSellingPrice = product.sellingPrice,
                        lineTotal = lineTotal
                    )

                    // Stock deduction
                    val newStock = (product.currentStock - qty).coerceAtLeast(0.0)
                    db.productDao().updateStock(product.id, newStock)

                    // Audit trail
                    db.stockMovementDao().insertMovement(
                        StockMovementEntity(
                            productId = product.id,
                            productName = product.name,
                            type = MovementType.SALE_OUT,
                            quantityChange = -qty,
                            previousStock = product.currentStock,
                            newStock = newStock,
                            referenceInvoice = invoiceNumber,
                            note = "Sale to ${sale.customerName}"
                        )
                    )
                    item
                }
                db.saleDao().insertSaleItems(saleItems)

                // If customer owes money, add to customer's Khata balance
                if (customer != null && dueAmount > 0.0) {
                    val updatedBal = customer.currentDueBalance + dueAmount
                    db.customerDao().updateBalance(customer.id, updatedBal)

                    if (dueDate != null) {
                        db.reminderDao().insertReminder(
                            ReminderEntity(
                                customerId = customer.id,
                                customerName = customer.name,
                                customerPhone = customer.phone,
                                amountDue = dueAmount,
                                dueDate = dueDate,
                                note = "Invoice $invoiceNumber payment due"
                            )
                        )
                    }
                }

                // If money was paid upfront, create PaymentEntity receipt record
                if (actualPaid > 0.0) {
                    val recFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
                    val receiptNumber = "REC-${recFormat.format(Date(timestamp))}"
                    db.paymentDao().insertPayment(
                        PaymentEntity(
                            receiptNumber = receiptNumber,
                            customerId = customer?.id ?: 0L,
                            customerName = customer?.name ?: "Walk-in Customer",
                            saleId = saleId,
                            amount = actualPaid,
                            paymentMode = paymentMode,
                            transactionRef = invoiceNumber,
                            note = "Payment received for $invoiceNumber",
                            paymentDate = timestamp
                        )
                    )
                }

                Result.success(sale.copy(id = saleId))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Purchases & Inventory Inward ---
    val allPurchases: Flow<List<PurchaseEntity>> = db.purchaseDao().getAllPurchases()

    suspend fun processPurchase(
        supplier: SupplierEntity?,
        items: List<Triple<ProductEntity, Double, Double>>, // Product, quantity, unitCost
        paidAmount: Double,
        paymentMode: PaymentMode,
        notes: String
    ): Result<PurchaseEntity> = withContext(Dispatchers.IO) {
        try {
            if (items.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Purchase must have at least one item"))

            db.withTransaction {
                val timestamp = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
                val invoiceNumber = "PUR-${dateFormat.format(Date(timestamp))}"

                val totalAmount = items.sumOf { (_, qty, cost) -> qty * cost }
                val actualPaid = paidAmount.coerceAtMost(totalAmount).coerceAtLeast(0.0)
                val dueAmount = (totalAmount - actualPaid).coerceAtLeast(0.0)

                val paymentStatus = when {
                    dueAmount <= 0.0 -> PaymentStatus.PAID
                    actualPaid > 0.0 -> PaymentStatus.PARTIAL
                    else -> PaymentStatus.UNPAID
                }

                val purchase = PurchaseEntity(
                    invoiceNumber = invoiceNumber,
                    supplierId = supplier?.id,
                    supplierName = supplier?.name ?: "Direct Cash Supplier",
                    supplierPhone = supplier?.phone ?: "",
                    totalAmount = totalAmount,
                    paidAmount = actualPaid,
                    dueAmount = dueAmount,
                    paymentStatus = paymentStatus,
                    paymentMode = paymentMode,
                    purchaseDate = timestamp,
                    notes = notes,
                    createdAt = timestamp
                )
                val purchaseId = db.purchaseDao().insertPurchase(purchase)

                val purchaseItems = items.map { (product, qty, cost) ->
                    val lineTotal = qty * cost
                    val item = PurchaseItemEntity(
                        purchaseId = purchaseId,
                        productId = product.id,
                        productName = product.name,
                        unit = product.unit,
                        quantity = qty,
                        unitCost = cost,
                        lineTotal = lineTotal
                    )

                    // Increment product stock and update purchaseCost if needed
                    val newStock = product.currentStock + qty
                    db.productDao().updateStock(product.id, newStock)
                    if (cost > 0 && cost != product.purchaseCost) {
                        db.productDao().updateProduct(product.copy(purchaseCost = cost, currentStock = newStock))
                    }

                    db.stockMovementDao().insertMovement(
                        StockMovementEntity(
                            productId = product.id,
                            productName = product.name,
                            type = MovementType.PURCHASE_IN,
                            quantityChange = qty,
                            previousStock = product.currentStock,
                            newStock = newStock,
                            referenceInvoice = invoiceNumber,
                            note = "Purchase from ${purchase.supplierName}"
                        )
                    )
                    item
                }
                db.purchaseDao().insertPurchaseItems(purchaseItems)

                // If unpaid balance to supplier, update supplier balance
                if (supplier != null && dueAmount > 0.0) {
                    val updatedBal = supplier.currentPayableBalance + dueAmount
                    db.supplierDao().updateBalance(supplier.id, updatedBal)
                }

                Result.success(purchase.copy(id = purchaseId))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Payments & Khata Settlements ---
    val allPayments: Flow<List<PaymentEntity>> = db.paymentDao().getAllPayments()

    suspend fun recordCustomerPayment(
        customer: CustomerEntity,
        amount: Double,
        paymentMode: PaymentMode,
        transactionRef: String,
        note: String
    ): Result<PaymentEntity> = withContext(Dispatchers.IO) {
        try {
            if (amount <= 0) return@withContext Result.failure(IllegalArgumentException("Amount must be greater than zero"))

            db.withTransaction {
                val timestamp = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
                val receiptNumber = "REC-${dateFormat.format(Date(timestamp))}"

                val payment = PaymentEntity(
                    receiptNumber = receiptNumber,
                    customerId = customer.id,
                    customerName = customer.name,
                    amount = amount,
                    paymentMode = paymentMode,
                    transactionRef = transactionRef,
                    note = note,
                    paymentDate = timestamp
                )
                val paymentId = db.paymentDao().insertPayment(payment)

                // Reduce customer's outstanding balance
                val newBalance = (customer.currentDueBalance - amount).coerceAtLeast(0.0)
                db.customerDao().updateBalance(customer.id, newBalance)

                Result.success(payment.copy(id = paymentId))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Expenses ---
    val allExpenses: Flow<List<ExpenseEntity>> = db.expenseDao().getAllExpenses()

    suspend fun addExpense(expense: ExpenseEntity): Long = withContext(Dispatchers.IO) {
        db.expenseDao().insertExpense(expense)
    }

    suspend fun deleteExpense(id: Long) = withContext(Dispatchers.IO) {
        db.expenseDao().deleteExpense(id)
    }

    // --- Reminders ---
    val allReminders: Flow<List<ReminderEntity>> = db.reminderDao().getAllReminders()
    val pendingReminders: Flow<List<ReminderEntity>> = db.reminderDao().getPendingReminders()

    suspend fun addReminder(reminder: ReminderEntity): Long = withContext(Dispatchers.IO) {
        db.reminderDao().insertReminder(reminder)
    }

    suspend fun markReminderCompleted(id: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        db.reminderDao().markCompleted(id, completed)
    }

    suspend fun deleteReminder(id: Long) = withContext(Dispatchers.IO) {
        db.reminderDao().deleteReminder(id)
    }

    // --- Financial Reports Calculation Flow ---
    val financialReport: Flow<FinancialReport> = combine(
        combine(
            db.saleDao().getAllSales(),
            db.saleDao().getAllSaleItems(),
            db.expenseDao().getAllExpenses()
        ) { sales, saleItems, expenses -> Triple(sales, saleItems, expenses) },
        combine(
            db.customerDao().getAllCustomers(),
            db.supplierDao().getAllSuppliers(),
            db.productDao().getAllProducts()
        ) { customers, suppliers, products -> Triple(customers, suppliers, products) }
    ) { (sales, saleItems, expenses), (customers, suppliers, products) ->
        val totalRevenue = sales.sumOf { it.totalAmount }
        val costOfGoodsSold = saleItems.sumOf { it.costOfGoodsSold }
        val grossProfit = totalRevenue - costOfGoodsSold
        val grossMarginPercent = if (totalRevenue > 0) (grossProfit / totalRevenue) * 100.0 else 0.0

        val totalExpenses = expenses.sumOf { it.amount }
        val netProfit = grossProfit - totalExpenses

        val totalCashCollected = sales.sumOf { it.paidAmount }
        val totalPendingReceivables = customers.sumOf { it.currentDueBalance }
        val totalSupplierPayables = suppliers.sumOf { it.currentPayableBalance }

        val inventoryValueAtCost = products.filter { !it.isArchived }.sumOf { it.inventoryValueAtCost }
        val inventoryValueAtRetail = products.filter { !it.isArchived }.sumOf { it.inventoryValueAtRetail }
        val lowStockCount = products.count { !it.isArchived && it.isLowStock }

        // Top products by revenue
        val topProducts = saleItems
            .groupBy { it.productName }
            .map { (name, items) ->
                TopProductSummary(
                    productName = name,
                    totalQuantitySold = items.sumOf { it.quantity },
                    totalRevenue = items.sumOf { it.lineTotal },
                    totalProfit = items.sumOf { it.grossProfit }
                )
            }
            .sortedByDescending { it.totalRevenue }
            .take(5)

        val expenseBreakdown = expenses
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        FinancialReport(
            totalRevenue = totalRevenue,
            costOfGoodsSold = costOfGoodsSold,
            grossProfit = grossProfit,
            grossProfitMarginPercent = grossMarginPercent,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            totalCashCollected = totalCashCollected,
            totalPendingReceivables = totalPendingReceivables,
            totalSupplierPayables = totalSupplierPayables,
            inventoryValueAtCost = inventoryValueAtCost,
            inventoryValueAtRetail = inventoryValueAtRetail,
            totalSalesCount = sales.size,
            lowStockCount = lowStockCount,
            topSellingProducts = topProducts,
            expenseBreakdown = expenseBreakdown
        )
    }

    suspend fun cancelSale(saleId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            db.withTransaction {
                val sale = db.saleDao().getSaleById(saleId) ?: return@withTransaction Result.failure(IllegalArgumentException("Sale not found"))
                val saleItems = db.saleDao().getSaleItemsDirect(saleId)

                // Return stock to inventory
                for (item in saleItems) {
                    val product = db.productDao().getProductById(item.productId)
                    if (product != null) {
                        val restoredStock = product.currentStock + item.quantity
                        db.productDao().updateStock(product.id, restoredStock)
                        db.stockMovementDao().insertMovement(
                            StockMovementEntity(
                                productId = product.id,
                                productName = product.name,
                                type = MovementType.CUSTOMER_RETURN,
                                quantityChange = item.quantity,
                                previousStock = product.currentStock,
                                newStock = restoredStock,
                                referenceInvoice = sale.invoiceNumber,
                                note = "Cancelled Sale Return: ${sale.invoiceNumber}"
                            )
                        )
                    }
                }

                // Reverse customer due balance if pending
                if (sale.customerId != null && sale.dueAmount > 0.0) {
                    val customer = db.customerDao().getCustomerById(sale.customerId)
                    if (customer != null) {
                        val newBal = (customer.currentDueBalance - sale.dueAmount).coerceAtLeast(0.0)
                        db.customerDao().updateBalance(customer.id, newBal)
                    }
                }

                // Delete sale items and sale
                db.saleDao().deleteSaleItemsForSale(saleId)
                db.saleDao().deleteSale(saleId)
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSaleItems(saleId: Long): List<SaleItemEntity> = withContext(Dispatchers.IO) {
        db.saleDao().getSaleItemsDirect(saleId)
    }

    suspend fun recordSupplierPayment(
        supplier: SupplierEntity,
        amount: Double,
        note: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (amount <= 0.0) return@withContext Result.failure(IllegalArgumentException("Amount must be greater than zero"))
            db.withTransaction {
                val newBal = (supplier.currentPayableBalance - amount).coerceAtLeast(0.0)
                db.supplierDao().updateBalance(supplier.id, newBal)
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(id: Long) = withContext(Dispatchers.IO) {
        db.customerDao().deleteCustomer(id)
    }

    suspend fun deleteSupplier(id: Long) = withContext(Dispatchers.IO) {
        db.supplierDao().deleteSupplier(id)
    }

    // --- Seed & Demo Data Cleanup Helper ---
    suspend fun cleanDemoData() = withContext(Dispatchers.IO) {
        // 1. Remove fictional sample products
        val sampleProductNames = listOf(
            "Basmati Premium Rice",
            "Basmati Rice 5kg",
            "Bath Soap",
            "Dairy Fresh Milk",
            "Milk 1L",
            "Premium Tea Dust",
            "Refined Sugar",
            "Sunflower Cooking Oil",
            "Washing Powder",
            "Whole Wheat Atta"
        )
        db.productDao().deleteProductsByNames(sampleProductNames)

        // 2. Remove fictional sample customer names from demo dataset
        val sampleCustomerNames = listOf(
            "Rashid Ahmed",
            "Tariq Mahmood",
            "Mohd Imran",
            "Saleem Khan",
            "Demo Customer — Not a Real Customer"
        )
        db.customerDao().deleteCustomersByNames(sampleCustomerNames)

        // 3. Remove fictional preloaded suppliers
        val sampleSuppliers = listOf("National Wholesale Depo", "Golden Agro Mills")
        db.supplierDao().deleteSuppliersByNames(sampleSuppliers)

        // 4. Remove fictional preloaded expenses
        val sampleExpenseNotes = listOf(
            "Monthly shop rent",
            "Power bill for cooling and lighting",
            "Shop helper assistant",
            "Customer & staff refreshments"
        )
        db.expenseDao().deleteExpensesByNotes(sampleExpenseNotes)
    }

    suspend fun seedInitialDataIfNeeded() = withContext(Dispatchers.IO) {
        cleanDemoData()
        // Never auto-insert fake products or fake customers.
        // App starts 100% clean for user's bidi business records.
    }

    suspend fun generateBackupSummary(): String = withContext(Dispatchers.IO) {
        val productsList = db.productDao().getAllActiveProducts().first()
        val salesList = db.saleDao().getAllSales().first()
        val purchasesList = db.purchaseDao().getAllPurchases().first()
        val customersList = db.customerDao().getAllCustomers().first()
        val expensesList = db.expenseDao().getAllExpenses().first()
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val sb = StringBuilder()
        sb.appendLine("========================================")
        sb.appendLine("ASLAM RECORD — BUSINESS BACKUP & SUMMARY")
        sb.appendLine("Date: ${sdf.format(Date())}")
        sb.appendLine("========================================\n")

        sb.appendLine("--- PRODUCTS (${productsList.size}) ---")
        if (productsList.isEmpty()) {
            sb.appendLine("No products added.")
        } else {
            productsList.forEachIndexed { i, p ->
                sb.appendLine("${i + 1}. ${p.name} | Unit: ${p.unit} | Stock: ${p.currentStock} | Buy: ₹${p.purchaseCost} | Sell: ₹${p.sellingPrice} | Notes: ${p.notes}")
            }
        }

        sb.appendLine("\n--- CUSTOMERS (${customersList.size}) ---")
        if (customersList.isEmpty()) {
            sb.appendLine("No customers recorded.")
        } else {
            customersList.forEachIndexed { i, c ->
                sb.appendLine("${i + 1}. ${c.name} | Phone: ${c.phone} | Due Balance: ₹${c.currentDueBalance}")
            }
        }

        sb.appendLine("\n--- RECENT SALES (${salesList.size}) ---")
        salesList.take(20).forEachIndexed { i, s ->
            sb.appendLine("${i + 1}. ${s.invoiceNumber} | Customer: ${s.customerName} | Total: ₹${s.totalAmount} | Paid: ₹${s.paidAmount} | Due: ₹${s.dueAmount}")
        }

        sb.appendLine("\n--- RECENT PURCHASES (${purchasesList.size}) ---")
        purchasesList.take(20).forEachIndexed { i, p ->
            sb.appendLine("${i + 1}. ${p.invoiceNumber} | Supplier: ${p.supplierName} | Total: ₹${p.totalAmount} | Paid: ₹${p.paidAmount} | Due: ₹${p.dueAmount}")
        }

        sb.appendLine("\n--- EXPENSES (${expensesList.size}) ---")
        val totalExp = expensesList.sumOf { it.amount }
        sb.appendLine("Total Expenses Recorded: ₹${String.format(Locale.getDefault(), "%.2f", totalExp)}")

        sb.appendLine("\n========================================")
        sb.appendLine("End of ASLAM RECORD Backup")
        sb.appendLine("========================================")
        sb.toString()
    }
}
