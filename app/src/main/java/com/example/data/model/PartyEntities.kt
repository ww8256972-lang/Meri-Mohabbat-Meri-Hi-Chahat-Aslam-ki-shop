package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["name"]),
        Index(value = ["phone"])
    ]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val address: String = "",
    val creditLimit: Double = 50000.0,
    val currentDueBalance: Double = 0.0, // Amount owed by customer to shop (Khata / Udhar)
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "suppliers",
    indices = [
        Index(value = ["name"]),
        Index(value = ["phone"])
    ]
)
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val company: String = "",
    val currentPayableBalance: Double = 0.0, // Amount shop owes to supplier
    val address: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
