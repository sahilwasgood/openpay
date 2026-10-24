package openpay.app.rails

/**
 * The UPI MDR math (DFS notification, Sept 2026): 0.4 percent on
 * person to merchant payments above Rs 2,000, capped at Rs 300, large
 * merchants only, person to person free. The payer never pays it.
 */
object Mdr {

    const val THRESHOLD_PAISE: Long = 200_000L
    const val CAP_PAISE: Long = 30_000L

    fun merchantFeePaise(amountPaise: Long): Long =
        if (amountPaise <= THRESHOLD_PAISE) 0L
        else minOf(amountPaise * 4L / 1000L, CAP_PAISE)

    /**
     * One purchase cut into sub-threshold pieces is prohibited structuring,
     * not savings. Used to warn in the split flow.
     */
    fun looksLikeStructuring(totalPaise: Long, shares: List<Long>): Boolean =
        totalPaise > THRESHOLD_PAISE && shares.isNotEmpty() && shares.all { it <= THRESHOLD_PAISE }
}
