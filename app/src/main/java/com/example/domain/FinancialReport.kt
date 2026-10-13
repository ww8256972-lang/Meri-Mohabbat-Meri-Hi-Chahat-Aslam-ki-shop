package com.example.domain

data class FinancialReport(
    val totalRevenue: Double = 0.0,
    val costOfGoodsSold: Double = 0.0,
    val grossProfit: Double = 0.0,
    val grossProfitMarginPercent: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val totalCashCollected: Double = 0.0,
    val totalPendingReceivables: Double = 0.0, // Money owed by customers
    val totalSupplierPayables: Double = 0.0,   // Money owed to suppliers
    val inventoryValueAtCost: Double = 0.0,
    val inventoryValueAtRetail: Double = 0.0,
    val totalSalesCount: Int = 0,
    val lowStockCount: Int = 0,
    val topSellingProducts: List<TopProductSummary> = emptyList(),
    val expenseBreakdown: Map<String, Double> = emptyMap()
)

data class TopProductSummary(
    val productName: String,
    val totalQuantitySold: Double,
    val totalRevenue: Double,
    val totalProfit: Double
)
