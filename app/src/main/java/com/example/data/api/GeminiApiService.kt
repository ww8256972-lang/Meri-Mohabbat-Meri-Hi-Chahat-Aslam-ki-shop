package com.example.data.api

import com.example.data.model.CustomerEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SupplierEntity
import com.example.domain.FinancialReport
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.Locale
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    @Json(name = "contents") val contents: List<GeminiContent>
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>,
    @Json(name = "role") val role: String = "user"
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): Response<GeminiGenerateResponse>
}

object GeminiClient {
    private val api: GeminiApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeminiApi::class.java)
    }

    private fun getInjectedApiKey(): String {
        return try {
            val field = Class.forName("com.example.BuildConfig").getField("GEMINI_API_KEY")
            (field.get(null) as? String)?.trim().orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    fun sanitizeChatResponse(raw: String): String {
        // Strip dollar-sign math formatting bugs ($...$ or $$...$$) and normalize currency to ₹
        var cleaned = raw
            .replace(Regex("""\$\$(.*?)\$\$""")) { it.groupValues[1] }
            .replace(Regex("""\$(.*?)\$""")) { it.groupValues[1] }
            .replace("$", "₹")
            .replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\[", "[")
            .replace("\\]", "]")
            .replace("**", "*")
        return cleaned.trim()
    }

    suspend fun consultAdvisor(
        userPrompt: String,
        report: FinancialReport,
        shopName: String,
        preferEnglish: Boolean = false,
        products: List<ProductEntity> = emptyList(),
        customersWithDue: List<CustomerEntity> = emptyList(),
        suppliers: List<SupplierEntity> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getInjectedApiKey()

        val productsSummary = if (products.isEmpty()) {
            "No products recorded in inventory yet."
        } else {
            products.joinToString(separator = "\n") { p ->
                "- ${p.name}: Stock = ${p.currentStock} ${p.unit}, Purchase Cost = ₹${p.purchaseCost}, Selling Price = ₹${p.sellingPrice}${if (p.notes.isNotBlank()) " (Notes: ${p.notes})" else ""}${if (p.isLowStock) " [LOW STOCK ALERT]" else ""}"
            }
        }

        val customerDuesSummary = if (customersWithDue.isEmpty()) {
            "No customers with pending dues."
        } else {
            customersWithDue.joinToString(separator = "\n") { c ->
                "- ${c.name} (${c.phone}): Pending Udhar = ₹${c.currentDueBalance}"
            }
        }

        val suppliersSummary = if (suppliers.isEmpty()) {
            "No suppliers recorded."
        } else {
            suppliers.joinToString(separator = "\n") { s ->
                "- ${s.name} (${s.phone}): Outstanding Payable = ₹${s.currentPayableBalance}"
            }
        }

        val contextSummary = """
            Shop Name: $shopName
            Total Sales Revenue: ₹${String.format(Locale.US, "%.2f", report.totalRevenue)}
            Cost of Goods Sold (COGS): ₹${String.format(Locale.US, "%.2f", report.costOfGoodsSold)}
            Gross Profit: ₹${String.format(Locale.US, "%.2f", report.grossProfit)} (${String.format(Locale.US, "%.1f", report.grossProfitMarginPercent)}%)
            Business Operating Expenses: ₹${String.format(Locale.US, "%.2f", report.totalExpenses)}
            Net Profit: ₹${String.format(Locale.US, "%.2f", report.netProfit)}
            Cash Collected: ₹${String.format(Locale.US, "%.2f", report.totalCashCollected)}
            Customer Outstanding Udhar (Receivables): ₹${String.format(Locale.US, "%.2f", report.totalPendingReceivables)}
            Supplier Payables: ₹${String.format(Locale.US, "%.2f", report.totalSupplierPayables)}
            Inventory Stock Value (at cost): ₹${String.format(Locale.US, "%.2f", report.inventoryValueAtCost)}
            Low Stock Products Count: ${report.lowStockCount} items
            Top Selling Items: ${if (report.topSellingProducts.isEmpty()) "None recorded yet" else report.topSellingProducts.joinToString { "${it.productName} (₹${it.totalRevenue})" }}
            Expenses Breakdown: ${if (report.expenseBreakdown.isEmpty()) "None recorded yet" else report.expenseBreakdown.entries.joinToString { "${it.key}: ₹${it.value}" }}
        """.trimIndent()

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val languageDirective = if (preferEnglish) {
                    "Respond in clear, simple, concise English. Use short bullet points. Always use ₹ for currency. Never use dollar signs ($)."
                } else {
                    "CRITICAL: Respond in friendly, natural Hindi (हिन्दी) or easy Hinglish suitable for an Indian shopkeeper. Explain clearly with exact numbers and short bullet points. NEVER wrap text in dollar signs ($) or LaTeX blocks ($...$). All amounts must be in ₹."
                }

                val systemPrompt = """
                    You are ASLAM AI (दुकान का AI मुंशी), the trusted digital business assistant and accountant for $shopName (bidi trading & merchandise business).
                    $languageDirective
                    Give direct, polite, and actionable advice to Mohd Aslam.
                    Never fabricate figures. If a figure is ₹0 or empty, state honestly that no records exist yet.
                    Never approve payments or change ledger data without user action.
                    
                    ACTUAL CURRENT DATABASE LEDGER:
                    $contextSummary
                    
                    CURRENT INVENTORY & PRODUCTS:
                    $productsSummary
                    
                    CUSTOMERS WITH PENDING UDHAR:
                    $customerDuesSummary
                    
                    SUPPLIERS WITH OUTSTANDING BALANCE:
                    $suppliersSummary
                    
                    MERCHANT'S QUESTION:
                    $userPrompt
                """.trimIndent()

                val request = GeminiGenerateRequest(
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
                    )
                )

                val response = api.generateContent(apiKey, request)
                if (response.isSuccessful) {
                    val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!text.isNullOrBlank()) {
                        return@withContext sanitizeChatResponse(text)
                    }
                }
            } catch (_: Exception) {
                // Fallback to local expert analysis
            }
        }

        val localResult = generateLocalExpertAnalysis(
            prompt = userPrompt,
            report = report,
            shopName = shopName,
            preferEnglish = preferEnglish,
            products = products,
            customersWithDue = customersWithDue,
            suppliers = suppliers
        )
        return@withContext sanitizeChatResponse(localResult)
    }

    private fun generateLocalExpertAnalysis(
        prompt: String,
        report: FinancialReport,
        shopName: String,
        preferEnglish: Boolean,
        products: List<ProductEntity>,
        customersWithDue: List<CustomerEntity>,
        suppliers: List<SupplierEntity>
    ): String {
        val lower = prompt.lowercase()

        if (preferEnglish) {
            return when {
                lower.contains("bidi") || lower.contains("product") || lower.contains("item") || lower.contains("rate") || lower.contains("price") -> {
                    if (products.isEmpty()) {
                        "📦 No bidi products found in your inventory yet.\n\nTap '+ Add Product' to add your bidi varieties with purchase and selling rates."
                    } else {
                        val productList = products.joinToString(separator = "\n") { p ->
                            "• ${p.name}: Stock = ${p.currentStock} ${p.unit} | Buy = ₹${p.purchaseCost} | Sell = ₹${p.sellingPrice} (Margin: ₹${String.format(Locale.US, "%.2f", p.grossMarginPerUnit)})"
                        }
                        """
                            📦 Bidi Products & Stock List ($shopName):
                            
                            $productList
                            
                            Total Stock Value (at cost): ₹${String.format(Locale.US, "%.2f", report.inventoryValueAtCost)}
                        """.trimIndent()
                    }
                }

                lower.contains("profit") || lower.contains("margin") -> {
                    val netMargin = if (report.totalRevenue > 0) (report.netProfit / report.totalRevenue) * 100 else 0.0
                    """
                        📊 Profit & Loss Summary for $shopName
                        
                        • Gross Sales: ₹${String.format(Locale.US, "%.2f", report.totalRevenue)}
                        • Cost of Goods Sold (COGS): ₹${String.format(Locale.US, "%.2f", report.costOfGoodsSold)}
                        • Gross Profit: ₹${String.format(Locale.US, "%.2f", report.grossProfit)} (${String.format(Locale.US, "%.1f", report.grossProfitMarginPercent)}%)
                        • Business Expenses: ₹${String.format(Locale.US, "%.2f", report.totalExpenses)}
                        • Net Profit: ₹${String.format(Locale.US, "%.2f", report.netProfit)} (${String.format(Locale.US, "%.1f", netMargin)}% net margin)
                        
                        💡 Recommendation: ${if (report.netProfit > 0) "Your business is generating positive net profit. Continue controlling operational overheads." else if (report.totalRevenue == 0.0) "No sales recorded yet. Record your daily sales to track profits." else "Expenses currently exceed gross profit. Review utility and overhead costs."}
                    """.trimIndent()
                }

                lower.contains("udhar") || lower.contains("due") || lower.contains("receivable") || lower.contains("customer") -> {
                    val customerList = if (customersWithDue.isEmpty()) {
                        "Great news! No customers currently have pending dues."
                    } else {
                        customersWithDue.joinToString(separator = "\n") { c ->
                            "• ${c.name} (${c.phone}): Pending Due = ₹${c.currentDueBalance}"
                        }
                    }
                    """
                        ⚠️ Customer Khata (Udhar) Audit
                        
                        $customerList
                        
                        • Total Customer Pending Dues: ₹${String.format(Locale.US, "%.2f", report.totalPendingReceivables)}
                        • Cash Collected from Sales: ₹${String.format(Locale.US, "%.2f", report.totalCashCollected)}
                        
                        💡 Action Required: 
                        ${if (report.totalPendingReceivables > 0) "Send payment reminders to overdue customers from the Business Records tab." else "All customer balances are clear!"}
                    """.trimIndent()
                }

                lower.contains("supplier") || lower.contains("pay") || lower.contains("vendor") -> {
                    val supplierList = if (suppliers.isEmpty()) {
                        "No suppliers recorded."
                    } else {
                        suppliers.joinToString(separator = "\n") { s ->
                            "• ${s.name} (${s.phone}): Balance Due = ₹${s.currentPayableBalance}"
                        }
                    }
                    """
                        🚚 Supplier Balance Audit
                        
                        $supplierList
                        
                        • Total Supplier Payables: ₹${String.format(Locale.US, "%.2f", report.totalSupplierPayables)}
                    """.trimIndent()
                }

                lower.contains("stock") || lower.contains("inventory") || lower.contains("low") -> {
                    val lowStockItems = products.filter { it.isLowStock }
                    val alertText = if (lowStockItems.isEmpty()) {
                        "All products have adequate stock levels."
                    } else {
                        lowStockItems.joinToString(separator = "\n") { p ->
                            "⚠️ ${p.name}: Only ${p.currentStock} ${p.unit} left (Alert threshold: ${p.minStockAlert})"
                        }
                    }
                    """
                        📦 Inventory Valuation & Stock Alerts
                        
                        • Total Stock Valuation (at cost): ₹${String.format(Locale.US, "%.2f", report.inventoryValueAtCost)}
                        • Low-Stock Count: ${report.lowStockCount} products
                        
                        $alertText
                        
                        💡 Reorder Advice: Contact your bidi suppliers promptly to replenish low stock.
                    """.trimIndent()
                }

                lower.contains("saving") || lower.contains("cost") || lower.contains("reduce") || lower.contains("growth") || lower.contains("boost") -> {
                    """
                        💡 Practical Business Growth Suggestions for $shopName
                        
                        1. Bulk Bidi Purchases: Buy bidi bundles/kattas in bulk to negotiate lower unit prices.
                        2. Fast Khata Recovery: Collect pending customer dues promptly to avoid locked cashflow.
                        3. Fast-Moving Varieties: Keep sufficient stock of popular bidi brands to prevent stockouts.
                        4. Expense Tracking: Record small tea, freight, and utility costs to calculate true profit.
                    """.trimIndent()
                }

                else -> {
                    """
                        🤖 ASLAM AI Business Health Summary
                        
                        • Total Sales: ₹${String.format(Locale.US, "%.2f", report.totalRevenue)}
                        • Net Profit: ₹${String.format(Locale.US, "%.2f", report.netProfit)}
                        • Customer Udhar Dues: ₹${String.format(Locale.US, "%.2f", report.totalPendingReceivables)}
                        • Total Stock Value: ₹${String.format(Locale.US, "%.2f", report.inventoryValueAtCost)}
                        • Low Stock Items: ${report.lowStockCount}
                        
                        Ask me any question in Hindi or English about your bidi stock, sales, profits, or customer dues!
                    """.trimIndent()
                }
            }
        } else {
            // Natural, clean Hindi
            return when {
                lower.contains("bidi") || lower.contains("बीडी") || lower.contains("सामान") || lower.contains("माल") || lower.contains("rate") || lower.contains("भाव") || lower.contains("कीमत") -> {
                    if (products.isEmpty()) {
                        "📦 आपकी दुकान में अभी कोई बीडी या प्रोडक्ट दर्ज नहीं है।\n\n'Products' टैब में जाकर '+ Add Product' बटन दबाएं और अपनी बीडी का नाम, खरीद व बिक्री रेट जोड़ें।"
                    } else {
                        val productList = products.joinToString(separator = "\n") { p ->
                            "• ${p.name}: स्टॉक = ${p.currentStock} ${p.unit} | खरीद = ₹${p.purchaseCost} | बिक्री = ₹${p.sellingPrice} (मुनाफ़ा: ₹${String.format(Locale.US, "%.2f", p.grossMarginPerUnit)}/${p.unit})"
                        }
                        """
                            📦 बीडी माल और स्टॉक की सूची ($shopName):
                            
                            $productList
                            
                            दुकान में रखे माल की कुल लागत: ₹${String.format(Locale.US, "%.2f", report.inventoryValueAtCost)}
                        """.trimIndent()
                    }
                }

                lower.contains("profit") || lower.contains("मुनाफा") || lower.contains("नफा") || lower.contains("margin") || lower.contains("कमाई") -> {
                    val netMargin = if (report.totalRevenue > 0) (report.netProfit / report.totalRevenue) * 100 else 0.0
                    """
                        📊 $shopName का मुनाफ़ा और मार्जिन हिसाब:
                        
                        • कुल बिक्री (Sales): ₹${String.format(Locale.US, "%.2f", report.totalRevenue)}
                        • माल की खरीद लागत (COGS): ₹${String.format(Locale.US, "%.2f", report.costOfGoodsSold)}
                        • सकल मुनाफ़ा (Gross Profit): ₹${String.format(Locale.US, "%.2f", report.grossProfit)} (${String.format(Locale.US, "%.1f", report.grossProfitMarginPercent)}%)
                        • दुकान के खर्चे (Expenses): ₹${String.format(Locale.US, "%.2f", report.totalExpenses)}
                        • शुद्ध मुनाफ़ा (Net Profit): ₹${String.format(Locale.US, "%.2f", report.netProfit)} (${String.format(Locale.US, "%.1f", netMargin)}%)
                        
                        💡 सलाह: ${if (report.netProfit > 0) "आपकी दुकान अच्छे मुनाफ़े में चल रही है।" else if (report.totalRevenue == 0.0) "अभी कोई बिक्री दर्ज नहीं हुई है। रोज़ाना बिक्री दर्ज करने पर यहाँ मुनाफ़ा दिखेगा।" else "खर्चे मुनाफ़े से ज़्यादा हैं। दुकान के खर्चों पर नज़र रखें।"}
                    """.trimIndent()
                }

                lower.contains("udhar") || lower.contains("उधार") || lower.contains("बाकी") || lower.contains("खाता") || lower.contains("due") || lower.contains("ग्राहक") -> {
                    val customerList = if (customersWithDue.isEmpty()) {
                        "बहुत बढ़िया! किसी भी ग्राहक पर कोई बकाया उधार नहीं है।"
                    } else {
                        customersWithDue.joinToString(separator = "\n") { c ->
                            "• ${c.name} (${c.phone}): बकाया उधार = ₹${c.currentDueBalance}"
                        }
                    }
                    """
                        ⚠️ ग्राहकों का उधार (खाता) हिसाब:
                        
                        $customerList
                        
                        • कुल बकाया उधार: ₹${String.format(Locale.US, "%.2f", report.totalPendingReceivables)}
                        • नकद वसूली: ₹${String.format(Locale.US, "%.2f", report.totalCashCollected)}
                        
                        💡 सलाह: ${if (report.totalPendingReceivables > 0) "खाता टैब से ग्राहकों को व्हाट्सएप या कॉल करके बकाया उधार समय पर वसूल करें।" else "दुकान की उधारी पूरी तरह साफ़ है!"}
                    """.trimIndent()
                }

                lower.contains("supplier") || lower.contains("सप्लायर") || lower.contains("देना") || lower.contains("लेनदार") -> {
                    val supplierList = if (suppliers.isEmpty()) {
                        "कोई सप्लायर दर्ज नहीं है।"
                    } else {
                        suppliers.joinToString(separator = "\n") { s ->
                            "• ${s.name} (${s.phone}): बकाया भुगतान = ₹${s.currentPayableBalance}"
                        }
                    }
                    """
                        🚚 सप्लायरों का बकाया हिसाब:
                        
                        $supplierList
                        
                        • कुल सप्लायर देनदारी: ₹${String.format(Locale.US, "%.2f", report.totalSupplierPayables)}
                    """.trimIndent()
                }

                lower.contains("stock") || lower.contains("स्टॉक") || lower.contains("low") || lower.contains("कम") -> {
                    val lowStockItems = products.filter { it.isLowStock }
                    val alertText = if (lowStockItems.isEmpty()) {
                        "सभी बीडी और सामान का स्टॉक ठीक है।"
                    } else {
                        lowStockItems.joinToString(separator = "\n") { p ->
                            "⚠️ ${p.name}: केवल ${p.currentStock} ${p.unit} बचा है (अलर्ट सीमा: ${p.minStockAlert})"
                        }
                    }
                    """
                        📦 दुकान के माल और स्टॉक की स्थिति:
                        
                        • दुकान में रखे माल की कुल कीमत: ₹${String.format(Locale.US, "%.2f", report.inventoryValueAtCost)}
                        • कम स्टॉक वाले आइटम: ${report.lowStockCount}
                        
                        $alertText
                        
                        💡 सलाह: खत्म होने वाले सामान को सप्लायर से तुरंत ऑर्डर करें ताकि ग्राहक वापस न जाएं।
                    """.trimIndent()
                }

                lower.contains("saving") || lower.contains("बचत") || lower.contains("सुझाव") || lower.contains("सलाह") || lower.contains("बिक्री") || lower.contains("धंधा") -> {
                    """
                        💡 $shopName के लिए बिज़नेस और मुनाफ़ा बढ़ाने के सुझाव:
                        
                        1. सप्लायर से थोक में बीडी खरीदें ताकि प्रति बंडल कम रेट मिले।
                        2. पुराना उधार समय पर वसूल करें ताकि नई खरीद के लिए हाथ में नकद रहे।
                        3. सबसे ज़्यादा बिकने वाली बीडी का स्टॉक हमेशा बनाए रखें।
                        4. दुकान के छोटे-मोटे खर्चे (भाड़ा, चाय) रोज़ाना दर्ज करें ताकि असली मुनाफ़ा पता रहे।
                    """.trimIndent()
                }

                else -> {
                    val salesMsg = if (report.totalRevenue > 0) "आज तक ₹${String.format(Locale.US, "%.2f", report.totalRevenue)} की कुल बिक्री दर्ज हुई है।" else "अभी तक कोई बिक्री दर्ज नहीं हुई है।"
                    val udharMsg = if (report.totalPendingReceivables > 0) "ग्राहकों से ₹${String.format(Locale.US, "%.2f", report.totalPendingReceivables)} का उधार वसूलना बाकी है।" else "कोई उधार बाकी नहीं है।"
                    """
                        नमस्ते असलम भाई! मैं ASLAM AI हूँ — आपकी दुकान का मुंशी और बिज़नेस सहायक।
                        
                        $salesMsg
                        
                        $udharMsg
                        
                        दुकान में रखे माल की कुल कीमत ₹${String.format(Locale.US, "%.2f", report.inventoryValueAtCost)} है।
                        
                        आप मुझसे किसी भी बीडी का रेट, स्टॉक, ग्राहकों का उधार, मुनाफा या बिज़नेस की सलाह कभी भी पूछ सकते हैं!
                    """.trimIndent()
                }
            }
        }
    }
}
