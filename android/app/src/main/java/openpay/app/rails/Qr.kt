package openpay.app.rails

import android.net.Uri

/** Parse a scanned UPI QR payload: upi://pay?pa=...&pn=...&am=...&tn=... */
data class QrPayee(
    val vpa: String,
    val name: String,
    val amountPaise: Long?,
    val note: String?
)

object Qr {

    fun parse(content: String): QrPayee? {
        val trimmed = content.trim()
        if (!trimmed.startsWith("upi://", ignoreCase = true)) return null
        val uri = Uri.parse(trimmed)
        if (uri.authority?.lowercase() != "pay") return null
        val pa = uri.getQueryParameter("pa")?.trim()
        if (pa.isNullOrBlank() || !pa.contains("@")) return null
        val amount = uri.getQueryParameter("am")?.trim()?.toDoubleOrNull()
        return QrPayee(
            vpa = pa,
            name = uri.getQueryParameter("pn")?.trim().orEmpty(),
            amountPaise = amount?.let { Math.round(it * 100) },
            note = uri.getQueryParameter("tn")?.trim()
        )
    }
}
