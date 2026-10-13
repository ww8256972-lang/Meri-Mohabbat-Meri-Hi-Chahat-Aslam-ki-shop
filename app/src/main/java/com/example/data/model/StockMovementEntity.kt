package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MovementType {
    PURCHASE_IN,
    SALE_OUT,
    ADJUSTMENT_ADD,
    ADJUSTMENT_DEDUCT,
    DAMAGE_LOSS,
    CUSTOMER_RETURN,
    SUPPLIER_RETURN
}

@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["timestamp"])
    ]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val type: MovementType,
    val quantityChange: Double, // positive or negative
    val previousStock: Double,
    val newStock: Double,
    val referenceInvoice: String = "",
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
