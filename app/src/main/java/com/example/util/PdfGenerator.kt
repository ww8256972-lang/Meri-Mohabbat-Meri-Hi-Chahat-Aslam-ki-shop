package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.CustomerEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SaleEntity
import com.example.data.model.SaleItemEntity
import com.example.data.model.ShopProfile
import com.example.domain.FinancialReport
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    private fun getPdfDirectory(context: Context): File {
        val dir = File(context.cacheDir, "generated_pdfs")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun sharePdf(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share $title"))
    }

    /**
     * 1. Generate Sale Receipt / Cash Memo PDF
     */
    fun generateSaleReceiptPdf(
        context: Context,
        shop: ShopProfile,
        sale: SaleEntity,
        items: List<SaleItemEntity>
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())

        // Background
        canvas.drawColor(Color.WHITE)

        // Header Background Banner
        paint.color = Color.rgb(15, 81, 50) // Emerald Dark
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 95f, paint)

        // Shop Title
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(shop.shopName, 36f, 38f, paint)

        // Shop Subtitle / Owner
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("${shop.ownerName} • Phone: ${shop.phone}", 36f, 56f, paint)
        canvas.drawText("${shop.address} • ${shop.gstOrTaxId}", 36f, 72f, paint)

        // Document Heading
        var y = 125f
        paint.color = Color.rgb(33, 37, 41)
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TAX INVOICE / SALE RECEIPT", 36f, y, paint)

        paint.textSize = 10f
        paint.color = Color.rgb(108, 117, 125)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Invoice No: ${sale.invoiceNumber}", 36f, y + 18f, paint)
        canvas.drawText("Date & Time: ${sdf.format(Date(sale.createdAt))}", 36f, y + 32f, paint)

        // Customer Info Card
        canvas.drawText("Customer: ${sale.customerName}", 320f, y + 18f, paint)
        if (sale.customerPhone.isNotBlank()) {
            canvas.drawText("Phone: ${sale.customerPhone}", 320f, y + 32f, paint)
        }
        canvas.drawText("Payment Mode: ${sale.paymentMode.name}", 320f, y + 46f, paint)

        y += 65f

        // Table Header
        paint.color = Color.rgb(241, 243, 245)
        canvas.drawRect(36f, y, (PAGE_WIDTH - 36).toFloat(), y + 24f, paint)

        paint.color = Color.rgb(33, 37, 41)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("#", 46f, y + 16f, paint)
        canvas.drawText("Item / Product", 80f, y + 16f, paint)
        canvas.drawText("Qty", 320f, y + 16f, paint)
        canvas.drawText("Rate", 390f, y + 16f, paint)
        canvas.drawText("Amount", 480f, y + 16f, paint)

        y += 28f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        // Table Rows
        if (items.isEmpty()) {
            canvas.drawText("1", 46f, y + 14f, paint)
            canvas.drawText("Bidi & Merchandise Sale", 80f, y + 14f, paint)
            canvas.drawText("1 Lot", 320f, y + 14f, paint)
            canvas.drawText("Rs. ${String.format(Locale.US, "%.2f", sale.totalAmount)}", 390f, y + 14f, paint)
            canvas.drawText("Rs. ${String.format(Locale.US, "%.2f", sale.totalAmount)}", 480f, y + 14f, paint)
            y += 24f
        } else {
            items.forEachIndexed { index, item ->
                if (index % 2 == 1) {
                    val rowPaint = Paint().apply { color = Color.rgb(250, 250, 250) }
                    canvas.drawRect(36f, y, (PAGE_WIDTH - 36).toFloat(), y + 22f, rowPaint)
                }
                canvas.drawText("${index + 1}", 46f, y + 15f, paint)
                val displayName = if (item.productName.length > 32) item.productName.take(30) + ".." else item.productName
                canvas.drawText(displayName, 80f, y + 15f, paint)
                canvas.drawText("${String.format(Locale.US, "%.1f", item.quantity)} ${item.unit}", 320f, y + 15f, paint)
                canvas.drawText("Rs. ${String.format(Locale.US, "%.2f", item.unitSellingPrice)}", 390f, y + 15f, paint)
                canvas.drawText("Rs. ${String.format(Locale.US, "%.2f", item.lineTotal)}", 480f, y + 15f, paint)
                y += 22f
            }
        }

        // Divider
        y += 10f
        paint.color = Color.rgb(222, 226, 230)
        canvas.drawLine(36f, y, (PAGE_WIDTH - 36).toFloat(), y, paint)
        y += 20f

        // Summary Totals
        paint.color = Color.rgb(33, 37, 41)
        paint.textSize = 10f
        canvas.drawText("Subtotal:", 350f, y, paint)
        canvas.drawText("Rs. ${String.format(Locale.US, "%.2f", sale.subtotal)}", 480f, y, paint)
        y += 16f

        if (sale.discount > 0) {
            canvas.drawText("Discount:", 350f, y, paint)
            canvas.drawText("- Rs. ${String.format(Locale.US, "%.2f", sale.discount)}", 480f, y, paint)
            y += 16f
        }

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        paint.color = Color.rgb(15, 81, 50)
        canvas.drawText("Bill Total:", 350f, y, paint)
        canvas.drawText("Rs. ${String.format(Locale.US, "%.2f", sale.totalAmount)}", 480f, y, paint)
        y += 18f

        paint.textSize = 10f
        paint.color = Color.rgb(40, 167, 69)
        canvas.drawText("Amount Paid (${sale.paymentMode.name}):", 350f, y, paint)
        canvas.drawText("Rs. ${String.format(Locale.US, "%.2f", sale.paidAmount)}", 480f, y, paint)
        y += 16f

        if (sale.dueAmount > 0) {
            paint.color = Color.rgb(220, 53, 69)
            canvas.drawText("Balance Due (Udhar):", 350f, y, paint)
            canvas.drawText("Rs. ${String.format(Locale.US, "%.2f", sale.dueAmount)}", 480f, y, paint)
            y += 16f
        }

        // Notes
        if (sale.notes.isNotBlank()) {
            y += 14f
            paint.color = Color.rgb(108, 117, 125)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("Notes: ${sale.notes}", 36f, y, paint)
        }

        // Footer
        paint.color = Color.rgb(173, 181, 189)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        canvas.drawLine(36f, 780f, (PAGE_WIDTH - 36).toFloat(), 780f, paint)
        canvas.drawText("Thank you for your business! • ASLAM RECORD Smart Bidi Business Ledger", 36f, 800f, paint)

        document.finishPage(page)

        val file = File(getPdfDirectory(context), "Sale_${sale.invoiceNumber}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    /**
     * 2. Generate Payment Receipt PDF (Khata / Udhar settlement)
     */
    fun generatePaymentReceiptPdf(
        context: Context,
        shop: ShopProfile,
        customer: CustomerEntity,
        payment: PaymentEntity
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())

        canvas.drawColor(Color.WHITE)

        // Header Background Banner
        paint.color = Color.rgb(15, 81, 50)
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 95f, paint)

        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(shop.shopName, 36f, 38f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("${shop.ownerName} • Phone: ${shop.phone}", 36f, 56f, paint)
        canvas.drawText("${shop.address} • ${shop.gstOrTaxId}", 36f, 72f, paint)

        var y = 135f
        paint.color = Color.rgb(33, 37, 41)
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("OFFICIAL PAYMENT RECEIPT", 36f, y, paint)

        paint.textSize = 10f
        paint.color = Color.rgb(108, 117, 125)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Receipt No: ${payment.receiptNumber}", 36f, y + 20f, paint)
        canvas.drawText("Date & Time: ${sdf.format(Date(payment.paymentDate))}", 36f, y + 36f, paint)

        y += 70f

        // Receipt Card Box
        paint.color = Color.rgb(248, 249, 250)
        canvas.drawRect(36f, y, (PAGE_WIDTH - 36).toFloat(), y + 170f, paint)
        paint.color = Color.rgb(206, 212, 218)
        paint.style = Paint.Style.STROKE
        canvas.drawRect(36f, y, (PAGE_WIDTH - 36).toFloat(), y + 170f, paint)
        paint.style = Paint.Style.FILL

        var cy = y + 30f
        paint.color = Color.rgb(33, 37, 41)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Received From:", 56f, cy, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("${customer.name} (Phone: ${customer.phone})", 180f, cy, paint)

        cy += 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Amount Received:", 56f, cy, paint)
        paint.color = Color.rgb(40, 167, 69)
        paint.textSize = 14f
        canvas.drawText("Rs. ${String.format(Locale.US, "%.2f", payment.amount)}", 180f, cy, paint)

        cy += 26f
        paint.color = Color.rgb(33, 37, 41)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Payment Mode:", 56f, cy, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("${payment.paymentMode.name}${if (payment.transactionRef.isNotBlank()) " (Ref: ${payment.transactionRef})" else ""}", 180f, cy, paint)

        cy += 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Remaining Due Balance:", 56f, cy, paint)
        paint.color = if (customer.currentDueBalance > 0) Color.rgb(220, 53, 69) else Color.rgb(40, 167, 69)
        canvas.drawText("Rs. ${String.format(Locale.US, "%.2f", customer.currentDueBalance)}", 180f, cy, paint)

        if (payment.note.isNotBlank()) {
            cy += 24f
            paint.color = Color.rgb(108, 117, 125)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("Note: ${payment.note}", 56f, cy, paint)
        }

        // Acknowledgment Stamp Badge
        y += 210f
        paint.color = Color.rgb(230, 244, 234)
        canvas.drawRect(36f, y, 220f, y + 40f, paint)
        paint.color = Color.rgb(15, 81, 50)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("✓ PAYMENT ACKNOWLEDGED", 46f, y + 25f, paint)

        // Footer
        paint.color = Color.rgb(173, 181, 189)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        canvas.drawLine(36f, 780f, (PAGE_WIDTH - 36).toFloat(), 780f, paint)
        canvas.drawText("This is a computer-generated payment receipt • ASLAM RECORD", 36f, 800f, paint)

        document.finishPage(page)

        val file = File(getPdfDirectory(context), "Payment_${payment.receiptNumber}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    /**
     * 3. Generate Inventory & Stock Valuation PDF Report
     */
    fun generateInventoryReportPdf(
        context: Context,
        shop: ShopProfile,
        products: List<ProductEntity>
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())

        canvas.drawColor(Color.WHITE)

        // Header
        paint.color = Color.rgb(15, 81, 50)
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 95f, paint)

        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(shop.shopName, 36f, 38f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("${shop.ownerName} • ${shop.phone} • ${shop.address}", 36f, 56f, paint)
        canvas.drawText("INVENTORY & STOCK VALUATION REPORT", 36f, 74f, paint)

        var y = 125f
        paint.color = Color.rgb(33, 37, 41)
        paint.textSize = 10f
        canvas.drawText("Report Generated: ${sdf.format(Date())}", 36f, y, paint)

        // Summary Boxes
        val totalCostVal = products.sumOf { it.inventoryValueAtCost }
        val totalRetailVal = products.sumOf { it.inventoryValueAtRetail }
        val lowStockCount = products.count { it.isLowStock }

        y += 18f
        paint.color = Color.rgb(241, 243, 245)
        canvas.drawRect(36f, y, (PAGE_WIDTH - 36).toFloat(), y + 42f, paint)

        paint.color = Color.rgb(33, 37, 41)
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total Items: ${products.size}", 50f, y + 25f, paint)
        canvas.drawText("Stock Cost Value: Rs. ${String.format(Locale.US, "%.2f", totalCostVal)}", 160f, y + 25f, paint)
        canvas.drawText("Retail Value: Rs. ${String.format(Locale.US, "%.2f", totalRetailVal)}", 350f, y + 25f, paint)
        if (lowStockCount > 0) {
            paint.color = Color.rgb(220, 53, 69)
            canvas.drawText("Low Stock: $lowStockCount", 490f, y + 25f, paint)
        }

        y += 58f

        // Table Header
        paint.color = Color.rgb(230, 235, 240)
        canvas.drawRect(36f, y, (PAGE_WIDTH - 36).toFloat(), y + 22f, paint)

        paint.color = Color.rgb(33, 37, 41)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("#", 42f, y + 15f, paint)
        canvas.drawText("Product Name", 65f, y + 15f, paint)
        canvas.drawText("Category", 210f, y + 15f, paint)
        canvas.drawText("Stock", 280f, y + 15f, paint)
        canvas.drawText("Cost", 350f, y + 15f, paint)
        canvas.drawText("Sell", 410f, y + 15f, paint)
        canvas.drawText("Valuation", 470f, y + 15f, paint)

        y += 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        if (products.isEmpty()) {
            paint.color = Color.rgb(108, 117, 125)
            canvas.drawText("No products in inventory yet.", 42f, y + 15f, paint)
        } else {
            products.forEachIndexed { i, p ->
                if (y > 750f) return@forEachIndexed // Single page safety

                if (i % 2 == 1) {
                    val rowPaint = Paint().apply { color = Color.rgb(250, 250, 250) }
                    canvas.drawRect(36f, y, (PAGE_WIDTH - 36).toFloat(), y + 18f, rowPaint)
                }

                paint.color = Color.rgb(33, 37, 41)
                paint.textSize = 8.5f
                canvas.drawText("${i + 1}", 42f, y + 13f, paint)
                val pName = if (p.name.length > 25) p.name.take(23) + ".." else p.name
                canvas.drawText(pName, 65f, y + 13f, paint)
                canvas.drawText(p.category, 210f, y + 13f, paint)
                canvas.drawText("${String.format(Locale.US, "%.0f", p.currentStock)} ${p.unit}", 280f, y + 13f, paint)
                canvas.drawText("Rs. ${String.format(Locale.US, "%.0f", p.purchaseCost)}", 350f, y + 13f, paint)
                canvas.drawText("Rs. ${String.format(Locale.US, "%.0f", p.sellingPrice)}", 410f, y + 13f, paint)
                canvas.drawText("Rs. ${String.format(Locale.US, "%.0f", p.inventoryValueAtCost)}", 470f, y + 13f, paint)

                y += 18f
            }
        }

        // Footer
        paint.color = Color.rgb(173, 181, 189)
        paint.textSize = 9f
        canvas.drawLine(36f, 780f, (PAGE_WIDTH - 36).toFloat(), 780f, paint)
        canvas.drawText("ASLAM RECORD • Official Stock & Valuation Ledger", 36f, 800f, paint)

        document.finishPage(page)

        val file = File(getPdfDirectory(context), "Inventory_Report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    /**
     * 4. Generate Business Profit & Loss PDF Report
     */
    fun generateFinancialReportPdf(
        context: Context,
        shop: ShopProfile,
        report: FinancialReport,
        period: String
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())

        canvas.drawColor(Color.WHITE)

        // Header
        paint.color = Color.rgb(15, 81, 50)
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 95f, paint)

        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(shop.shopName, 36f, 38f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("${shop.ownerName} • Phone: ${shop.phone}", 36f, 56f, paint)
        canvas.drawText("FINANCIAL PROFIT & LOSS STATEMENT ($period)", 36f, 74f, paint)

        var y = 135f
        paint.color = Color.rgb(33, 37, 41)
        paint.textSize = 10f
        canvas.drawText("Generated On: ${sdf.format(Date())} • Period: $period", 36f, y, paint)

        y += 25f

        fun drawMetricRow(title: String, valueStr: String, isBold: Boolean = false, textColor: Int = Color.rgb(33, 37, 41)) {
            paint.typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = if (isBold) 11f else 10f
            paint.color = textColor
            canvas.drawText(title, 46f, y, paint)
            canvas.drawText(valueStr, 440f, y, paint)
            y += 20f
        }

        // Section 1: Revenue & Gross Profit
        paint.color = Color.rgb(241, 243, 245)
        canvas.drawRect(36f, y - 14f, (PAGE_WIDTH - 36).toFloat(), y + 6f, paint)
        paint.color = Color.rgb(15, 81, 50)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11f
        canvas.drawText("TRADING ACCOUNT / REVENUE", 46f, y, paint)
        y += 24f

        drawMetricRow("Gross Sales Revenue:", "Rs. ${String.format(Locale.US, "%.2f", report.totalRevenue)}", isBold = true)
        drawMetricRow("Cost of Goods Sold (COGS):", "- Rs. ${String.format(Locale.US, "%.2f", report.costOfGoodsSold)}")
        drawMetricRow("Gross Profit:", "Rs. ${String.format(Locale.US, "%.2f", report.grossProfit)} (${String.format(Locale.US, "%.1f", report.grossProfitMarginPercent)}%)", isBold = true, textColor = Color.rgb(15, 81, 50))

        y += 10f

        // Section 2: Operating Expenses
        paint.color = Color.rgb(241, 243, 245)
        canvas.drawRect(36f, y - 14f, (PAGE_WIDTH - 36).toFloat(), y + 6f, paint)
        paint.color = Color.rgb(15, 81, 50)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11f
        canvas.drawText("OPERATING EXPENSES & NET PROFIT", 46f, y, paint)
        y += 24f

        drawMetricRow("Operating Expenses (Rent, Utility, Staff):", "- Rs. ${String.format(Locale.US, "%.2f", report.totalExpenses)}")
        val netColor = if (report.netProfit >= 0) Color.rgb(15, 81, 50) else Color.rgb(220, 53, 69)
        drawMetricRow("Net Business Profit (Loss):", "Rs. ${String.format(Locale.US, "%.2f", report.netProfit)}", isBold = true, textColor = netColor)

        y += 10f

        // Section 3: Cash Flow & Balances
        paint.color = Color.rgb(241, 243, 245)
        canvas.drawRect(36f, y - 14f, (PAGE_WIDTH - 36).toFloat(), y + 6f, paint)
        paint.color = Color.rgb(15, 81, 50)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11f
        canvas.drawText("CASH POSITION & OUTSTANDING DUES", 46f, y, paint)
        y += 24f

        drawMetricRow("Actual Cash Collected:", "Rs. ${String.format(Locale.US, "%.2f", report.totalCashCollected)}")
        drawMetricRow("Customer Udhar (Outstanding Receivables):", "Rs. ${String.format(Locale.US, "%.2f", report.totalPendingReceivables)}", textColor = Color.rgb(220, 53, 69))
        drawMetricRow("Supplier Payables Outstanding:", "Rs. ${String.format(Locale.US, "%.2f", report.totalSupplierPayables)}")
        drawMetricRow("Current Inventory Stock Value (at cost):", "Rs. ${String.format(Locale.US, "%.2f", report.inventoryValueAtCost)}")

        // Footer
        paint.color = Color.rgb(173, 181, 189)
        paint.textSize = 9f
        canvas.drawLine(36f, 780f, (PAGE_WIDTH - 36).toFloat(), 780f, paint)
        canvas.drawText("ASLAM RECORD • True Business Accounting Report", 36f, 800f, paint)

        document.finishPage(page)

        val file = File(getPdfDirectory(context), "Financial_Report_${period}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }
}
