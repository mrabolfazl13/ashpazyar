package ir.ashpazyar.app

import ir.ashpazyar.app.core.util.ClockFormat
import ir.ashpazyar.app.core.util.Dimension
import ir.ashpazyar.app.core.util.JalaliCalendar
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.core.util.UnitConverter
import ir.ashpazyar.app.domain.model.IngredientUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class UnitConverterTest {
    private val rice = Fixtures.ingredient("rice", cupWeight = 180.0, pieceWeight = null)
    private val egg = Fixtures.ingredient("eggs", unit = IngredientUnit.PIECE, pieceWeight = 50.0)

    @Test fun massToBase() {
        assertEquals(1500.0, UnitConverter.toBase(1.5, IngredientUnit.KILOGRAM, null)!!.amount, 0.0001)
    }

    @Test fun volumeToBase() {
        val base = UnitConverter.toBase(2.0, IngredientUnit.CUP, null)!!
        assertEquals(Dimension.VOLUME, base.dimension)
        assertEquals(480.0, base.amount, 0.0001)
    }

    @Test fun cupUsesIngredientWeight() {
        val base = UnitConverter.toBase(1.0, IngredientUnit.CUP, rice.profile)!!
        assertEquals(Dimension.MASS, base.dimension)
        assertEquals(180.0, base.amount, 0.0001)
    }

    @Test fun pieceUsesIngredientWeight() {
        val base = UnitConverter.toBase(2.0, IngredientUnit.PIECE, egg.profile)!!
        assertEquals(Dimension.MASS, base.dimension)
        assertEquals(100.0, base.amount, 0.0001)
    }

    @Test fun convertMass() {
        assertEquals(0.75, UnitConverter.convert(750.0, IngredientUnit.GRAM, IngredientUnit.KILOGRAM, null)!!, 0.0001)
    }

    @Test fun convertVolume() {
        assertEquals(3.0, UnitConverter.convert(1.0, IngredientUnit.TABLESPOON, IngredientUnit.TEASPOON, null)!!, 0.0001)
    }

    @Test fun incompatibleDimensionsReturnNull() {
        assertNull(UnitConverter.convert(1.0, IngredientUnit.GRAM, IngredientUnit.MILLILITER, null))
    }

    @Test fun shoppingUnitChoosesKilogramAndLiter() {
        assertEquals(IngredientUnit.KILOGRAM, UnitConverter.shoppingUnit(ir.ashpazyar.app.core.util.BaseQuantity(1500.0, Dimension.MASS), null))
        assertEquals(IngredientUnit.LITER, UnitConverter.shoppingUnit(ir.ashpazyar.app.core.util.BaseQuantity(1200.0, Dimension.VOLUME), null))
    }

    @Test fun countUnitWithoutMetadata() {
        val base = UnitConverter.toBase(3.0, IngredientUnit.PIECE, null)!!
        assertEquals(Dimension.COUNT, base.dimension)
        assertEquals(IngredientUnit.PIECE, UnitConverter.shoppingUnit(base, null))
    }

    @Test fun prettyUsesPersianDigits() {
        val text = UnitConverter.pretty(250.0, IngredientUnit.GRAM, null)
        assertEquals("۲۵۰ گرم", text)
    }

    @Test fun bestDisplayMass() {
        assertEquals("۱ کیلوگرم", UnitConverter.bestDisplay(ir.ashpazyar.app.core.util.BaseQuantity(1000.0, Dimension.MASS)))
    }

    @Test fun clockCountdown() {
        assertEquals("۰۲:۰۵", ClockFormat.countdown(125))
        assertEquals("۰۱:۰۰:۰۱", ClockFormat.countdown(3601))
    }

    @Test fun persianNumbers() {
        assertEquals("۱۲۳٫۵", PersianText.number(123.5))
        assertEquals("۱۲۳", PersianText.number(123.0))
    }

    @Test fun jalaliKnownNowruz() {
        val first = JalaliCalendar.from(LocalDate.of(2025, 3, 21))
        assertEquals(1404, first.year)
        assertEquals(1, first.month)
        assertEquals(1, first.day)
        val second = JalaliCalendar.from(LocalDate.of(2024, 3, 20))
        assertEquals(1403, second.year)
        assertEquals(1, second.month)
        assertEquals(1, second.day)
    }

    @Test fun jalaliMonthNames() {
        val date = JalaliCalendar.from(LocalDate.of(2025, 7, 1))
        assertEquals("تیر", date.monthName)
    }
}
