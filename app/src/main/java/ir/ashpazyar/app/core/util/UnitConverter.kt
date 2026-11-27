package ir.ashpazyar.app.core.util

import ir.ashpazyar.app.domain.model.IngredientProfile
import ir.ashpazyar.app.domain.model.IngredientUnit
import kotlin.math.abs

enum class Dimension { MASS, VOLUME, COUNT }

data class BaseQuantity(val amount: Double, val dimension: Dimension) {
    operator fun plus(other: BaseQuantity): BaseQuantity {
        require(dimension == other.dimension) { "ابعاد ناسازگار" }
        return BaseQuantity(amount + other.amount, dimension)
    }
}

object UnitConverter {
    fun toBase(amount: Double, unit: IngredientUnit, profile: IngredientProfile?): BaseQuantity? {
        if (amount < 0.0) return null
        return when (unit) {
            IngredientUnit.GRAM -> BaseQuantity(amount, Dimension.MASS)
            IngredientUnit.KILOGRAM -> BaseQuantity(amount * 1000.0, Dimension.MASS)
            IngredientUnit.MILLILITER -> BaseQuantity(amount, Dimension.VOLUME)
            IngredientUnit.LITER -> BaseQuantity(amount * 1000.0, Dimension.VOLUME)
            IngredientUnit.TEASPOON -> BaseQuantity(amount * 5.0, Dimension.VOLUME)
            IngredientUnit.TABLESPOON -> BaseQuantity(amount * 15.0, Dimension.VOLUME)
            IngredientUnit.PINCH -> BaseQuantity(amount * 0.3, Dimension.VOLUME)
            IngredientUnit.CUP -> {
                val cupWeight = profile?.cupWeightGrams
                if (cupWeight != null) BaseQuantity(amount * cupWeight, Dimension.MASS)
                else BaseQuantity(amount * 240.0, Dimension.VOLUME)
            }
            IngredientUnit.PIECE, IngredientUnit.CLOVE, IngredientUnit.BUNCH, IngredientUnit.SLICE, IngredientUnit.CAN -> {
                val pieceWeight = profile?.pieceWeightGrams
                if (pieceWeight != null) BaseQuantity(amount * pieceWeight, Dimension.MASS)
                else BaseQuantity(amount, Dimension.COUNT)
            }
        }
    }

    fun fromBase(quantity: BaseQuantity, unit: IngredientUnit, profile: IngredientProfile?): Double? {
        return when (unit) {
            IngredientUnit.GRAM -> if (quantity.dimension == Dimension.MASS) quantity.amount else null
            IngredientUnit.KILOGRAM -> if (quantity.dimension == Dimension.MASS) quantity.amount / 1000.0 else null
            IngredientUnit.MILLILITER -> if (quantity.dimension == Dimension.VOLUME) quantity.amount else null
            IngredientUnit.LITER -> if (quantity.dimension == Dimension.VOLUME) quantity.amount / 1000.0 else null
            IngredientUnit.TEASPOON -> if (quantity.dimension == Dimension.VOLUME) quantity.amount / 5.0 else null
            IngredientUnit.TABLESPOON -> if (quantity.dimension == Dimension.VOLUME) quantity.amount / 15.0 else null
            IngredientUnit.PINCH -> if (quantity.dimension == Dimension.VOLUME) quantity.amount / 0.3 else null
            IngredientUnit.CUP -> {
                val cupWeight = profile?.cupWeightGrams
                if (cupWeight != null && quantity.dimension == Dimension.MASS) quantity.amount / cupWeight
                else if (quantity.dimension == Dimension.VOLUME) quantity.amount / 240.0
                else null
            }
            IngredientUnit.PIECE, IngredientUnit.CLOVE, IngredientUnit.BUNCH, IngredientUnit.SLICE, IngredientUnit.CAN -> {
                val pieceWeight = profile?.pieceWeightGrams
                if (pieceWeight != null && quantity.dimension == Dimension.MASS) quantity.amount / pieceWeight
                else if (quantity.dimension == Dimension.COUNT) quantity.amount
                else null
            }
        }
    }

    fun convert(amount: Double, from: IngredientUnit, to: IngredientUnit, profile: IngredientProfile?): Double? {
        val base = toBase(amount, from, profile) ?: return null
        return fromBase(base, to, profile)
    }

    fun sameDimension(a: IngredientUnit, b: IngredientUnit, profile: IngredientProfile?): Boolean {
        val ba = toBase(1.0, a, profile) ?: return false
        val bb = toBase(1.0, b, profile) ?: return false
        return ba.dimension == bb.dimension
    }

    fun pretty(amount: Double, unit: IngredientUnit, profile: IngredientProfile? = null): String {
        val safe = if (abs(amount) < 0.001) 0.01 else amount
        val rounded = when {
            safe >= 100 -> Math.round(safe).toDouble()
            safe >= 10 -> Math.round(safe * 2.0) / 2.0
            else -> Math.round(safe * 10.0) / 10.0
        }
        val unitLabel = when (unit) {
            IngredientUnit.GRAM -> "گرم"
            IngredientUnit.KILOGRAM -> "کیلوگرم"
            IngredientUnit.MILLILITER -> "میلی‌لیتر"
            IngredientUnit.LITER -> "لیتر"
            IngredientUnit.TEASPOON -> "قاشق چای‌خوری"
            IngredientUnit.TABLESPOON -> "قاشق غذاخوری"
            IngredientUnit.CUP -> "پیمانه"
            IngredientUnit.PIECE -> "عدد"
            IngredientUnit.CLOVE -> "حبه"
            IngredientUnit.BUNCH -> "دسته"
            IngredientUnit.SLICE -> "برش"
            IngredientUnit.PINCH -> "نوک‌قاشق"
            IngredientUnit.CAN -> "قوطی"
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

    fun shoppingUnit(quantity: BaseQuantity, profile: IngredientProfile?): IngredientUnit {
        return when (quantity.dimension) {
            Dimension.MASS -> if (quantity.amount >= 1000.0) IngredientUnit.KILOGRAM else IngredientUnit.GRAM
            Dimension.VOLUME -> if (quantity.amount >= 1000.0) IngredientUnit.LITER else IngredientUnit.MILLILITER
            Dimension.COUNT -> when (profile?.defaultUnit) {
                IngredientUnit.PIECE, IngredientUnit.CLOVE, IngredientUnit.BUNCH, IngredientUnit.SLICE, IngredientUnit.CAN -> profile.defaultUnit
                else -> IngredientUnit.PIECE
            }
        }
    }

    fun displayForUnit(quantity: BaseQuantity, unit: IngredientUnit, profile: IngredientProfile?): String {
        val value = fromBase(quantity, unit, profile)
        return if (value == null) bestDisplay(quantity, profile) else pretty(value, unit, profile)
    }
}
