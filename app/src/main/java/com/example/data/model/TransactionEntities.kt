package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PaymentStatus {
    PAID,
    PARTIAL,
    UNPAID
}

enum class PaymentMode {
    CASH,
    UPI_ONLINE,
    BANK_TRANSFER,
    CHEQUE,
    CREDIT_KHATA
}

@Entity(
    tableName = "sales",
    indices = [
        Index(value = ["invoiceNumber"], unique = true),
        Index(value = ["customerId"]),
        Index(value = ["createdAt"]),
        Index(value = ["paymentStatus"])
    ]
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val customerId: Long? = null,
    val customerName: String = "Walk-in Customer",
    val customerPhone: String = "",
    val subtotal: Double,
    val discount: Double = 0.0,
    val totalAmount: Double,
    val paidAmount: Double,
    val dueAmount: Double, // Outstanding balance from this invoice
    val paymentStatus: PaymentStatus,
    val paymentMode: PaymentMode,
    val dueDate: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["saleId"]),
        Index(value = ["productId"])
    ]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val unit: String,
    val quantity: Double,
    val unitCostPrice: Double, // Cost price at time of sale (for exact COGS / Profit calculation)
    val unitSellingPrice: Double,
    val lineTotal: Double
) {
    val costOfGoodsSold: Double
        get() = quantity * unitCostPrice

    val grossProfit: Double
        get() = lineTotal - costOfGoodsSold
}

@Entity(
    tableName = "purchases",
    indices = [
        Index(value = ["invoiceNumber"], unique = true),
        Index(value = ["supplierId"]),
        Index(value = ["purchaseDate"])
    ]
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val supplierId: Long? = null,
    val supplierName: String = "Direct Cash Supplier",
    val supplierPhone: String = "",
    val totalAmount: Double,
    val paidAmount: Double,
    val dueAmount: Double,
    val paymentStatus: PaymentStatus,
    val paymentMode: PaymentMode,
    val purchaseDate: Long = System.currentTimeMillis(),
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "purchase_items",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["purchaseId"]),
        Index(value = ["productId"])
    ]
)
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val purchaseId: Long,
    val productId: Long,
    val productName: String,
    val unit: String,
    val quantity: Double,
    val unitCost: Double,
    val lineTotal: Double
)
