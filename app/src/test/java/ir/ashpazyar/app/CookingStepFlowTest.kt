package ir.ashpazyar.app

import ir.ashpazyar.app.domain.engine.CookingStepFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CookingStepFlowTest {
    @Test
    fun `next step stays locked until the current step is completed`() {
        assertFalse(CookingStepFlow.canAdvance(totalSteps = 4, stepIndex = 1, completed = setOf(0)))
        assertNull(CookingStepFlow.nextIndex(totalSteps = 4, stepIndex = 1, completed = setOf(0)))

        assertEquals(2, CookingStepFlow.nextIndex(totalSteps = 4, stepIndex = 1, completed = setOf(0, 1)))
    }

    @Test
    fun `completed other steps do not unlock the current step`() {
        assertFalse(CookingStepFlow.canAdvance(totalSteps = 5, stepIndex = 2, completed = setOf(0, 1, 3, 4)))
        assertEquals(3, CookingStepFlow.nextIndex(totalSteps = 5, stepIndex = 2, completed = setOf(0, 1, 2, 3, 4)))
    }

    @Test
    fun `last step has no next step even when completed`() {
        assertFalse(CookingStepFlow.canAdvance(totalSteps = 3, stepIndex = 2, completed = setOf(0, 1, 2)))
        assertNull(CookingStepFlow.nextIndex(totalSteps = 3, stepIndex = 2, completed = setOf(0, 1, 2)))
    }

    @Test
    fun `progress counts only valid completed step indexes`() {
        assertEquals(0.5f, CookingStepFlow.progress(totalSteps = 4, completed = setOf(0, 2, 9, -1)), 0.0001f)
        assertEquals(0f, CookingStepFlow.progress(totalSteps = 0, completed = setOf(0)), 0.0001f)
    }

    @Test
    fun `all completed requires every valid step`() {
        assertFalse(CookingStepFlow.allCompleted(totalSteps = 3, completed = setOf(0, 1)))
        assertTrue(CookingStepFlow.allCompleted(totalSteps = 3, completed = setOf(0, 1, 2)))
        assertFalse(CookingStepFlow.allCompleted(totalSteps = 0, completed = emptySet()))
    }
}
