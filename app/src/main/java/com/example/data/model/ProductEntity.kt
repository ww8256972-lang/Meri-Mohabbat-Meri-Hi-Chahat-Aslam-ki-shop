package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["name"]),
        Index(value = ["barcode"]),
        Index(value = ["category"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val barcode: String = "",
    val category: String = "Bidi",
    val unit: String = "Bundle", // Bundle, Katta, Box, Pkt, Piece, etc.
    val purchaseCost: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val currentStock: Double = 0.0,
    val minStockAlert: Double = 0.0,
    val notes: String = "",
    val isArchived: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = minStockAlert > 0.0 && currentStock <= minStockAlert

    val grossMarginPerUnit: Double
        get() = sellingPrice - purchaseCost

    val grossMarginPercentage: Double
        get() = if (sellingPrice > 0) ((sellingPrice - purchaseCost) / sellingPrice) * 100.0 else 0.0

    val inventoryValueAtCost: Double
        get() = currentStock * purchaseCost

    val inventoryValueAtRetail: Double
        get() = currentStock * sellingPrice
}
