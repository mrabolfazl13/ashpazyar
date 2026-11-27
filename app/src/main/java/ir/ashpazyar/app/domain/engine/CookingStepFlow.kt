package ir.ashpazyar.app.domain.engine

/**
 * قواعد خالص حرکت در حالت پخت: تا مرحله جاری تأیید نشود، مرحله بعد باز نمی‌شود.
 */
object CookingStepFlow {
    fun normalizedCompleted(totalSteps: Int, completed: Set<Int>): Set<Int> {
        if (totalSteps <= 0) return emptySet()
        return completed.filter { it in 0 until totalSteps }.toSet()
    }

    fun canAdvance(totalSteps: Int, stepIndex: Int, completed: Set<Int>): Boolean {
        if (totalSteps <= 1 || stepIndex !in 0 until totalSteps - 1) return false
        return stepIndex in normalizedCompleted(totalSteps, completed)
    }

    fun nextIndex(totalSteps: Int, stepIndex: Int, completed: Set<Int>): Int? {
        return if (canAdvance(totalSteps, stepIndex, completed)) stepIndex + 1 else null
    }

    fun progress(totalSteps: Int, completed: Set<Int>): Float {
        if (totalSteps <= 0) return 0f
        return normalizedCompleted(totalSteps, completed).size.toFloat() / totalSteps.toFloat()
    }

    fun allCompleted(totalSteps: Int, completed: Set<Int>): Boolean {
        return totalSteps > 0 && normalizedCompleted(totalSteps, completed).size == totalSteps
    }
}
