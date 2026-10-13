package com.example

import com.example.data.model.CustomerEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SaleItemEntity
import com.example.domain.FinancialReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopCalculationTest {

    @Test
    fun testBidiProductMarginCalculations() {
        val product = ProductEntity(
            id = 1,
            name = "502 Special Bidi 25s",
            unit = "Bundle",
            purchaseCost = 380.0,
            sellingPrice = 450.0,
            currentStock = 20.0,
            minStockAlert = 5.0,
            notes = "Direct from bidi manufacturer"
        )

        assertEquals(70.0, product.grossMarginPerUnit, 0.001)
        val expectedPercent = (70.0 / 450.0) * 100.0
        assertEquals(expectedPercent, product.grossMarginPercentage, 0.001)
        assertEquals(7600.0, product.inventoryValueAtCost, 0.001)
        assertEquals(9000.0, product.inventoryValueAtRetail, 0.001)
        assertEquals(false, product.isLowStock)
        assertEquals("Bundle", product.unit)
        assertEquals("Direct from bidi manufacturer", product.notes)
    }

    @Test
    fun testLowStockDetection() {
        val lowStockProduct = ProductEntity(
            id = 2,
            name = "Katta Desi Bidi",
            unit = "Katta",
            purchaseCost = 50.0,
            sellingPrice = 60.0,
            currentStock = 3.0,
            minStockAlert = 5.0
        )
        assertTrue(lowStockProduct.isLowStock)
    }

    @Test
    fun testFinancialProfitCalculations() {
        val revenue = 50000.0
        val cogs = 38000.0
        val expenses = 4500.0

        val grossProfit = revenue - cogs
        val netProfit = grossProfit - expenses
        val margin = (grossProfit / revenue) * 100.0

        val report = FinancialReport(
            totalRevenue = revenue,
            costOfGoodsSold = cogs,
            grossProfit = grossProfit,
            grossProfitMarginPercent = margin,
            totalExpenses = expenses,
            netProfit = netProfit,
            totalCashCollected = 42000.0,
            totalPendingReceivables = 8000.0
        )

        assertEquals(12000.0, report.grossProfit, 0.001)
        assertEquals(7500.0, report.netProfit, 0.001)
        assertEquals(24.0, report.grossProfitMarginPercent, 0.001)
        assertEquals(8000.0, report.totalPendingReceivables, 0.001)
    }

    @Test
    fun testSaleItemCogsCalculation() {
        val item = SaleItemEntity(
            id = 1,
            saleId = 10,
            productId = 1,
            productName = "No. 10 Bidi Bundle",
            unit = "Bundle",
            quantity = 3.0,
            unitCostPrice = 380.0,
            unitSellingPrice = 450.0,
            lineTotal = 1350.0
        )

        assertEquals(1140.0, item.costOfGoodsSold, 0.001)
        assertEquals(210.0, item.grossProfit, 0.001)
    }

    @Test
    fun testCustomerBalanceReduction() {
        val customer = CustomerEntity(
            id = 1,
            name = "Local Buyer",
            phone = "9876543210",
            currentDueBalance = 3200.0
        )

        val paymentAmount = 1200.0
        val newBalance = (customer.currentDueBalance - paymentAmount).coerceAtLeast(0.0)
        assertEquals(2000.0, newBalance, 0.001)
    }
}
