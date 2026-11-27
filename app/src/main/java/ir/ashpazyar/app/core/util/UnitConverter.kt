package ir.ashpazyar.app.core.util

import ir.ashpazyar.app.domain.model.IngredientProfile
import ir.ashpazyar.app.domain.model.Unit
import kotlin.math.abs

enum class Dimension { MASS, VOLUME, COUNT }

data class BaseQuantity(val amount: Double, val dimension: Dimension) {
    operator fun plus(other: BaseQuantity): BaseQuantity {
        require(dimension == other.dimension) { "ابعاد ناسازگار" }
        return BaseQuantity(amount + other.amount, dimension)
    }
}

object UnitConverter {
    fun toBase(amount: Double, unit: Unit, profile: IngredientProfile?): BaseQuantity? {
        if (amount < 0.0) return null
        return when (unit) {
            Unit.GRAM -> BaseQuantity(amount, Dimension.MASS)
            Unit.KILOGRAM -> BaseQuantity(amount * 1000.0, Dimension.MASS)
            Unit.MILLILITER -> BaseQuantity(amount, Dimension.VOLUME)
            Unit.LITER -> BaseQuantity(amount * 1000.0, Dimension.VOLUME)
            Unit.TEASPOON -> BaseQuantity(amount * 5.0, Dimension.VOLUME)
            Unit.TABLESPOON -> BaseQuantity(amount * 15.0, Dimension.VOLUME)
            Unit.PINCH -> BaseQuantity(amount * 0.3, Dimension.VOLUME)
            Unit.CUP -> {
                val cupWeight = profile?.cupWeightGrams
                if (cupWeight != null) BaseQuantity(amount * cupWeight, Dimension.MASS)
                else BaseQuantity(amount * 240.0, Dimension.VOLUME)
            }
            Unit.PIECE, Unit.CLOVE, Unit.BUNCH, Unit.SLICE, Unit.CAN -> {
                val pieceWeight = profile?.pieceWeightGrams
                if (pieceWeight != null) BaseQuantity(amount * pieceWeight, Dimension.MASS)
                else BaseQuantity(amount, Dimension.COUNT)
            }
        }
    }

    fun fromBase(quantity: BaseQuantity, unit: Unit, profile: IngredientProfile?): Double? {
        return when (unit) {
            Unit.GRAM -> if (quantity.dimension == Dimension.MASS) quantity.amount else null
            Unit.KILOGRAM -> if (quantity.dimension == Dimension.MASS) quantity.amount / 1000.0 else null
            Unit.MILLILITER -> if (quantity.dimension == Dimension.VOLUME) quantity.amount else null
            Unit.LITER -> if (quantity.dimension == Dimension.VOLUME) quantity.amount / 1000.0 else null
            Unit.TEASPOON -> if (quantity.dimension == Dimension.VOLUME) quantity.amount / 5.0 else null
            Unit.TABLESPOON -> if (quantity.dimension == Dimension.VOLUME) quantity.amount / 15.0 else null
            Unit.PINCH -> if (quantity.dimension == Dimension.VOLUME) quantity.amount / 0.3 else null
            Unit.CUP -> {
                val cupWeight = profile?.cupWeightGrams
                if (cupWeight != null && quantity.dimension == Dimension.MASS) quantity.amount / cupWeight
                else if (quantity.dimension == Dimension.VOLUME) quantity.amount / 240.0
                else null
            }
            Unit.PIECE, Unit.CLOVE, Unit.BUNCH, Unit.SLICE, Unit.CAN -> {
                val pieceWeight = profile?.pieceWeightGrams
                if (pieceWeight != null && quantity.dimension == Dimension.MASS) quantity.amount / pieceWeight
                else if (quantity.dimension == Dimension.COUNT) quantity.amount
                else null
            }
        }
    }

    fun convert(amount: Double, from: Unit, to: Unit, profile: IngredientProfile?): Double? {
        val base = toBase(amount, from, profile) ?: return null
        return fromBase(base, to, profile)
    }

    fun sameDimension(a: Unit, b: Unit, profile: IngredientProfile?): Boolean {
        val ba = toBase(1.0, a, profile) ?: return false
        val bb = toBase(1.0, b, profile) ?: return false
        return ba.dimension == bb.dimension
    }

    fun pretty(amount: Double, unit: Unit, profile: IngredientProfile? = null): String {
        val safe = if (abs(amount) < 0.001) 0.01 else amount
        val rounded = when {
            safe >= 100 -> Math.round(safe).toDouble()
            safe >= 10 -> Math.round(safe * 2.0) / 2.0
            else -> Math.round(safe * 10.0) / 10.0
        }
        val unitLabel = when (unit) {
            Unit.GRAM -> "گرم"
            Unit.KILOGRAM -> "کیلوگرم"
            Unit.MILLILITER -> "میلی‌لیتر"
            Unit.LITER -> "لیتر"
            Unit.TEASPOON -> "قاشق چای‌خوری"
            Unit.TABLESPOON -> "قاشق غذاخوری"
            Unit.CUP -> "پیمانه"
            Unit.PIECE -> "عدد"
            Unit.CLOVE -> "حبه"
            Unit.BUNCH -> "دسته"
            Unit.SLICE -> "برش"
            Unit.PINCH -> "نوک‌قاشق"
            Unit.CAN -> "قوطی"
        }
        return PersianText.number(rounded) + " " + unitLabel
    }

    fun bestDisplay(quantity: BaseQuantity, profile: IngredientProfile? = null): String {
        return when (quantity.dimension) {
            Dimension.MASS -> if (quantity.amount >= 1000.0) PersianText.number(quantity.amount / 1000.0) + " کیلوگرم"
            else PersianText.number(quantity.amount) + " گرم"
            Dimension.VOLUME -> if (quantity.amount >= 1000.0) PersianText.number(quantity.amount / 1000.0) + " لیتر"
            else PersianText.number(quantity.amount) + " میلی‌لیتر"
            Dimension.COUNT -> PersianText.number(quantity.amount) + " " + (profile?.defaultUnit?.label?.takeIf {
                it in listOf("عدد", "حبه", "دسته", "برش", "قوطی")
            } ?: "عدد")
        }
    }

    fun shoppingUnit(quantity: BaseQuantity, profile: IngredientProfile?): Unit {
        return when (quantity.dimension) {
            Dimension.MASS -> if (quantity.amount >= 1000.0) Unit.KILOGRAM else Unit.GRAM
            Dimension.VOLUME -> if (quantity.amount >= 1000.0) Unit.LITER else Unit.MILLILITER
            Dimension.COUNT -> when (profile?.defaultUnit) {
                Unit.PIECE, Unit.CLOVE, Unit.BUNCH, Unit.SLICE, Unit.CAN -> profile.defaultUnit
                else -> Unit.PIECE
            }
        }
    }

    fun displayForUnit(quantity: BaseQuantity, unit: Unit, profile: IngredientProfile?): String {
        val value = fromBase(quantity, unit, profile)
        return if (value == null) bestDisplay(quantity, profile) else pretty(value, unit, profile)
    }
}
