package com.example.util

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import com.example.data.model.Customer
import com.example.data.model.RecoveryPayment
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WhatsAppHelper {

    /**
     * Cleans up phone numbers: removes spaces, hyphens, parentheses.
     * Ensures only numerical characters remain.
     */
    fun cleanPhoneNumber(rawPhone: String): String {
        val digitsOnly = rawPhone.replace(Regex("[^0-9]"), "")
        return digitsOnly
    }

    /**
     * Checks if the device currently has an active internet connection.
     */
    fun isDataOrWifiOnline(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = connectivityManager?.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        } catch (e: Exception) {
            true // Fallback to optimistic true
        }
    }

    /**
     * Formats a professional, digital receiving receipt message for WhatsApp.
     */
    fun buildReceivingReceiptMessage(
        customerName: String,
        receiptNo: String,
        amountPaid: Double,
        previousBalance: Double,
        remainingBalance: Double,
        paymentMode: String,
        referenceNo: String,
        timestamp: Long = System.currentTimeMillis(),
        pharmacyName: String = "Umair Pharmacy"
    ): String {
        val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(timestamp))
        val amountStr = String.format(Locale.US, "%.2f", amountPaid)
        val prevStr = String.format(Locale.US, "%.2f", previousBalance)
        val remStr = String.format(Locale.US, "%.2f", maxOf(0.0, remainingBalance))

        val statusLine = if (remainingBalance <= 0.05) {
            "✅ *Status: Account FULLY SETTLED. Thank you!*"
        } else {
            "⚠️ *Remaining Due Balance: $$remStr*"
        }

        return """
🏥 *$pharmacyName*
🧾 *OFFICIAL PAYMENT RECEIPT*
━━━━━━━━━━━━━━━━━━━━━━━━━
Dear *$customerName*,

We have received and recorded your payment towards your pharmacy account ledger.

• *Receipt No:* #$receiptNo
• *Date & Time:* $formattedDate
• *Amount Received:* *$$amountStr*
• *Payment Mode:* ${paymentMode.replace("_", " ")}
• *Reference ID:* ${referenceNo.ifBlank { "N/A" }}

📊 *LEDGER BALANCE SUMMARY:*
• Previous Due: $$prevStr
• Payment Credited: -$$amountStr
$statusLine

Thank you for your business and prompt settlement!
━━━━━━━━━━━━━━━━━━━━━━━━━
_This is an automated system confirmation from $pharmacyName._
        """.trimIndent()
    }

    /**
     * Formats an itemized statement / ledger summary for WhatsApp.
     */
    fun buildLedgerStatementMessage(
        customer: Customer,
        totalInvoiced: Double,
        totalRecovered: Double,
        recentTransactions: List<String>,
        pharmacyName: String = "Umair Pharmacy"
    ): String {
        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
        val balStr = String.format(Locale.US, "%.2f", customer.outstandingBalance)
        val invStr = String.format(Locale.US, "%.2f", totalInvoiced)
        val recStr = String.format(Locale.US, "%.2f", totalRecovered)

        val txSummary = if (recentTransactions.isEmpty()) {
            "• No recent transactions on record."
        } else {
            recentTransactions.take(8).joinToString("\n")
        }

        return """
🏥 *$pharmacyName*
📋 *CUSTOMER ACCOUNT LEDGER STATEMENT*
━━━━━━━━━━━━━━━━━━━━━━━━━
*Customer:* ${customer.name}
*Phone:* ${customer.phone}
*Statement Date:* $dateStr

📊 *FINANCIAL SUMMARY:*
• Total Invoiced (Debits): $$invStr
• Total Received (Credits): $$recStr
• *Current Outstanding Balance: $$balStr*
• Overdue Status: ${if (customer.outstandingBalance > 0) "${customer.daysOverdue} days overdue" else "Settled / Nil"}

📑 *RECENT LEDGER ENTRIES:*
$txSummary

━━━━━━━━━━━━━━━━━━━━━━━━━
_For queries or payment confirmations, reply directly to this message._
        """.trimIndent()
    }

    /**
     * Dispatches WhatsApp message using Android Intent.
     * Uses WhatsApp package if installed, with seamless fallback to web URL.
     */
    fun sendWhatsAppMessage(
        context: Context,
        rawPhone: String,
        messageText: String
    ): Boolean {
        val cleanPhone = cleanPhoneNumber(rawPhone)
        val encodedMessage = try {
            URLEncoder.encode(messageText, "UTF-8")
        } catch (e: Exception) {
            Uri.encode(messageText)
        }

        val urlString = if (cleanPhone.isNotBlank()) {
            "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"
        } else {
            "https://api.whatsapp.com/send?text=$encodedMessage"
        }

        val uri = Uri.parse(urlString)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            // First try targeting WhatsApp directly
            intent.setPackage("com.whatsapp")
            context.startActivity(intent)
            Toast.makeText(context, "Opening WhatsApp for receipt delivery...", Toast.LENGTH_SHORT).show()
            true
        } catch (e1: Exception) {
            try {
                // Try WhatsApp Business package
                intent.setPackage("com.whatsapp.w4b")
                context.startActivity(intent)
                Toast.makeText(context, "Opening WhatsApp Business...", Toast.LENGTH_SHORT).show()
                true
            } catch (e2: Exception) {
                try {
                    // Fallback to general intent or browser
                    intent.setPackage(null)
                    context.startActivity(intent)
                    Toast.makeText(context, "Opening WhatsApp via web...", Toast.LENGTH_SHORT).show()
                    true
                } catch (e3: Exception) {
                    Toast.makeText(context, "WhatsApp is not installed on this device.", Toast.LENGTH_LONG).show()
                    false
                }
            }
        }
    }
}
