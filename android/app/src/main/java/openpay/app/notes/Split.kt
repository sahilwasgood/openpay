package openpay.app.notes

/**
 * Fair bill splitting at paise precision: the first `remainder` shares get
 * one extra paisa, so the parts always add back to the total exactly.
 */
object Split {

    fun shares(totalPaise: Long, people: Int): List<Long> {
        if (people <= 0 || totalPaise <= 0) return emptyList()
        val base = totalPaise / people
        val remainder = (totalPaise % people).toInt()
        return List(people) { i -> base + if (i < remainder) 1L else 0L }
    }
}
