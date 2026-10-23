package openpay.app.data

/**
 * Bank SMS sender identification. Indian bank SMS arrive from short codes
 * like "AD-HDFCBK" or "VM-SBIBNK". We only trust a confirmation SMS when the
 * sender matches the bank the user picked, killing spoofed senders.
 */
data class Bank(val name: String, val senders: List<String>)

object Banks {
    val all: List<Bank> = listOf(
        Bank("State Bank of India", listOf("SBI", "SBIBNK", "SBIB")),
        Bank("HDFC Bank", listOf("HDFCBK", "HDFC")),
        Bank("ICICI Bank", listOf("ICICIB", "ICICI")),
        Bank("Axis Bank", listOf("AXISBK", "AXIS")),
        Bank("Punjab National Bank", listOf("PNB")),
        Bank("Bank of Baroda", listOf("BOB", "BARODA")),
        Bank("Kotak Mahindra Bank", listOf("KOTAKB", "KOTAK")),
        Bank("Canara Bank", listOf("CANBNK", "CANARA")),
        Bank("Union Bank of India", listOf("UNIONB", "UBI")),
        Bank("Bank of India", listOf("BOI", "BANKIND")),
        Bank("IDFC First Bank", listOf("IDFCFB", "IDFC")),
        Bank("IndusInd Bank", listOf("INDUSB", "INDUSIND")),
        Bank("Yes Bank", listOf("YESBNK", "YES")),
        Bank("Airtel Payments Bank", listOf("AIRPB", "AIRTEL")),
        Bank("Paytm Payments Bank", listOf("PAYTM")),
        Bank("Other bank", emptyList())
    )
}
