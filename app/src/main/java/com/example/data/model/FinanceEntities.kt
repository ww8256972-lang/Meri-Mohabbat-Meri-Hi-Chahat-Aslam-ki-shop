package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["saleId"]),
        Index(value = ["paymentDate"])
    ]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val receiptNumber: String,
    val customerId: Long,
    val customerName: String,
    val saleId: Long? = null,
    val amount: Double,
    val paymentMode: PaymentMode,
    val transactionRef: String = "",
    val note: String = "",
    val paymentDate: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["category"]),
        Index(value = ["expenseDate"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String, // Rent, Electricity, Salaries, Transport, Tea/Snacks, Maintenance, Other
    val amount: Double,
    val note: String = "",
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val expenseDate: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["dueDate"]),
        Index(value = ["isCompleted"])
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val customerName: String,
    val customerPhone: String,
    val amountDue: Double,
    val dueDate: Long,
    val note: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class ShopProfile(
    val shopName: String = "ASLAM RECORD",
    val ownerName: String = "Mohd Aslam",
    val phone: String = "+91 98765 43210",
    val address: String = "Main Bazar, Commercial Market, Shop #14",
    val gstOrTaxId: String = "GSTIN: 07AAAAA0000A1Z5",
    val currencySymbol: String = "₹",
    val whatsappCloudPhoneId: String = "",
    val whatsappCloudAccessToken: String = "",
    val isWhatsappConfigured: Boolean = false
)
