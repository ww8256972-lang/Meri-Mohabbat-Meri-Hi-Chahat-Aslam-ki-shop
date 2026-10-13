package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.api.WhatsAppManager
import com.example.data.db.AslamDatabase
import com.example.data.model.CustomerEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.MovementType
import com.example.data.model.PaymentEntity
import com.example.data.model.PaymentMode
import com.example.data.model.ProductEntity
import com.example.data.model.PurchaseEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.SaleEntity
import com.example.data.model.ShopProfile
import com.example.data.model.StockMovementEntity
import com.example.data.model.SupplierEntity
import com.example.data.repository.ShopRepository
import com.example.data.worker.ReminderScheduler
import com.example.domain.FinancialReport
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AiChatMessage(
    val id: Long = System.currentTimeMillis(),
    val sender: String, // "user" or "aslam_ai"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ShopViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AslamDatabase.getInstance(application)
    private val repository = ShopRepository(db)

    // UI Feedback messages
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Shop Profile
    private val _shopProfile = MutableStateFlow(
        ShopProfile(
            shopName = "ASLAM RECORD",
            ownerName = "Mohd Aslam",
            phone = "+91 98765 43210",
            address = "Main Market, Commercial Center, Shop #14",
            gstOrTaxId = "GSTIN: 07AAAAA0000A1Z5",
            currencySymbol = "₹"
        )
    )
    val shopProfile: StateFlow<ShopProfile> = _shopProfile.asStateFlow()

    fun updateShopProfile(profile: ShopProfile) {
        _shopProfile.value = profile
        viewModelScope.launch {
            _userMessage.emit("Shop settings updated successfully")
        }
    }

    // --- Search Queries ---
    val productSearchQuery = MutableStateFlow("")
    val customerSearchQuery = MutableStateFlow("")

    // --- Products & Inventory ---
    val products: StateFlow<List<ProductEntity>> = productSearchQuery.flatMapLatest { query ->
        if (query.isBlank()) repository.allActiveProducts else repository.searchProducts(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProductsIncludingArchived: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<ProductEntity>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockMovements: StateFlow<List<StockMovementEntity>> = repository.allStockMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Customers & Suppliers ---
    val customers: StateFlow<List<CustomerEntity>> = customerSearchQuery.flatMapLatest { query ->
        if (query.isBlank()) repository.allCustomers else repository.searchCustomers(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customersWithDue: StateFlow<List<CustomerEntity>> = repository.customersWithDue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<SupplierEntity>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Sales & Transactions ---
    val sales: StateFlow<List<SaleEntity>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val salesWithDue: StateFlow<List<SaleEntity>> = repository.salesWithDue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchases: StateFlow<List<PurchaseEntity>> = repository.allPurchases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<PaymentEntity>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingReminders: StateFlow<List<ReminderEntity>> = repository.pendingReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val financialReport: StateFlow<FinancialReport> = repository.financialReport
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialReport())

    // --- AI Chat ---
    private val _aiMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                sender = "aslam_ai",
                message = "नमस्ते असलम भाई! मैं ASLAM AI हूँ — आपकी दुकान का मुंशी और बिज़नेस सहायक। मुझसे पूछें: आज का मुनाफा, ग्राहकों का उधार, बीडी का स्टॉक या कोई भी हिसाब!"
            )
        )
    )
    val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
            ReminderScheduler.scheduleDailyPaymentCheck(application)
        }
    }

    fun saveProduct(product: ProductEntity, onSaved: ((ProductEntity) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.saveProduct(product)
            _userMessage.emit("Product '${product.name}' saved")
            onSaved?.invoke(product.copy(id = id))
        }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch {
            repository.deleteProduct(id)
            _userMessage.emit("Product deleted")
        }
    }

    fun cleanDemoData() {
        viewModelScope.launch {
            repository.cleanDemoData()
            _userMessage.emit("All demo and sample records cleared")
        }
    }

    fun getBackupSummary(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val text = repository.generateBackupSummary()
            onResult(text)
        }
    }

    fun adjustStock(productId: Long, delta: Double, type: MovementType, note: String) {
        viewModelScope.launch {
            val success = repository.adjustStock(productId, delta, type, note)
            if (success) {
                _userMessage.emit("Stock updated successfully")
            } else {
                _userMessage.emit("Failed to update stock")
            }
        }
    }

    fun archiveProduct(id: Long, isArchived: Boolean) {
        viewModelScope.launch {
            repository.archiveProduct(id, isArchived)
            _userMessage.emit(if (isArchived) "Product archived" else "Product restored")
        }
    }

    fun saveCustomer(customer: CustomerEntity, onSaved: ((CustomerEntity) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.saveCustomer(customer)
            _userMessage.emit("Customer '${customer.name}' saved")
            onSaved?.invoke(customer.copy(id = id))
        }
    }

    fun saveSupplier(supplier: SupplierEntity) {
        viewModelScope.launch {
            repository.saveSupplier(supplier)
            _userMessage.emit("Supplier '${supplier.name}' saved")
        }
    }

    fun recordCustomerPayment(
        customer: CustomerEntity,
        amount: Double,
        mode: PaymentMode,
        ref: String,
        note: String,
        onSuccess: ((PaymentEntity) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = repository.recordCustomerPayment(customer, amount, mode, ref, note)
            if (result.isSuccess) {
                val payment = result.getOrThrow()
                _userMessage.emit("Payment of ₹${String.format("%.2f", amount)} recorded for ${customer.name}")
                onSuccess?.invoke(payment)
            } else {
                _userMessage.emit("Error recording payment: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    suspend fun getSaleItems(saleId: Long): List<SaleItemEntity> {
        return repository.getSaleItems(saleId)
    }

    fun processSale(
        customer: CustomerEntity?,
        items: List<Pair<ProductEntity, Double>>,
        discount: Double,
        paidAmount: Double,
        paymentMode: PaymentMode,
        dueDate: Long?,
        notes: String,
        onSuccess: (SaleEntity) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.processSale(customer, items, discount, paidAmount, paymentMode, dueDate, notes)
            if (result.isSuccess) {
                val sale = result.getOrThrow()
                _userMessage.emit("Sale invoice ${sale.invoiceNumber} created successfully")
                onSuccess(sale)
            } else {
                _userMessage.emit("Failed to create sale: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun processPurchase(
        supplier: SupplierEntity?,
        items: List<Triple<ProductEntity, Double, Double>>,
        paidAmount: Double,
        paymentMode: PaymentMode,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.processPurchase(supplier, items, paidAmount, paymentMode, notes)
            if (result.isSuccess) {
                _userMessage.emit("Purchase recorded and stock added")
                onSuccess()
            } else {
                _userMessage.emit("Failed to record purchase: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun addExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.addExpense(expense)
            _userMessage.emit("Expense of ₹${String.format("%.2f", expense.amount)} added")
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteExpense(id)
            _userMessage.emit("Expense deleted")
        }
    }

    fun addReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.addReminder(reminder)
            _userMessage.emit("Reminder scheduled for ${reminder.customerName}")
        }
    }

    fun toggleReminderCompleted(id: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.markReminderCompleted(id, completed)
            _userMessage.emit(if (completed) "Reminder completed" else "Reminder re-opened")
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            repository.deleteReminder(id)
            _userMessage.emit("Reminder removed")
        }
    }

    fun cancelSale(saleId: Long) {
        viewModelScope.launch {
            val result = repository.cancelSale(saleId)
            if (result.isSuccess) {
                _userMessage.emit("Sale cancelled and stock restored to inventory")
            } else {
                _userMessage.emit("Failed to cancel sale: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun recordSupplierPayment(supplier: SupplierEntity, amount: Double, note: String) {
        viewModelScope.launch {
            val result = repository.recordSupplierPayment(supplier, amount, note)
            if (result.isSuccess) {
                _userMessage.emit("Payment of ₹${String.format("%.2f", amount)} recorded to ${supplier.name}")
            } else {
                _userMessage.emit("Failed to record payment: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun deleteCustomer(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomer(id)
            _userMessage.emit("Customer removed")
        }
    }

    fun deleteSupplier(id: Long) {
        viewModelScope.launch {
            repository.deleteSupplier(id)
            _userMessage.emit("Supplier removed")
        }
    }

    val preferEnglishAi = MutableStateFlow(false)

    fun sendAiPrompt(prompt: String) {
        if (prompt.isBlank() || _isAiLoading.value) return

        val userMsg = AiChatMessage(sender = "user", message = prompt.trim())
        _aiMessages.value = _aiMessages.value + userMsg
        _isAiLoading.value = true

        viewModelScope.launch {
            val report = financialReport.value
            val shop = shopProfile.value
            val currentProducts = products.value
            val currentCustomersDue = customersWithDue.value
            val currentSuppliers = suppliers.value

            val responseText = GeminiClient.consultAdvisor(
                userPrompt = prompt,
                report = report,
                shopName = shop.shopName,
                preferEnglish = preferEnglishAi.value,
                products = currentProducts,
                customersWithDue = currentCustomersDue,
                suppliers = currentSuppliers
            )
            val aiMsg = AiChatMessage(sender = "aslam_ai", message = responseText)
            _aiMessages.value = _aiMessages.value + aiMsg
            _isAiLoading.value = false
        }
    }

    fun clearAiChat() {
        val welcome = if (preferEnglishAi.value) {
            "Chat history cleared. How may I advise your shop business today?"
        } else {
            "बातचीत का इतिहास साफ़ कर दिया गया है। आज आपकी दुकान के हिसाब-किताब में मैं क्या मदद करूँ?"
        }
        _aiMessages.value = listOf(
            AiChatMessage(
                sender = "aslam_ai",
                message = welcome
            )
        )
    }
}
