package dev.dwm.liftlog.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Nutrient values are per 100g. microsJson: optional map of extra nutrients per 100g. */
@kotlinx.serialization.Serializable
@Entity(indices = [Index("barcode")])
data class Food(
    @PrimaryKey val id: String = newId(),
    val barcode: String? = null,
    val name: String,
    val brand: String = "",
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val microsJson: String? = null,
    val imageUrl: String? = null,
    /** One serving in grams, and how the label words it ("1 tbsp", "1 scoop"). From Open Food Facts. */
    val servingGrams: Double? = null,
    val servingLabel: String? = null,
    val custom: Boolean = false,
    val updatedAt: Long = nowMillis(),
    val deletedAt: Long? = null,
)

@kotlinx.serialization.Serializable
@Entity(indices = [Index("epochDay"), Index("foodId")])
data class FoodLog(
    @PrimaryKey val id: String = newId(),
    val epochDay: Long,
    val foodId: String,
    val grams: Double,
    val meal: String, // Breakfast | Lunch | Dinner | Snack
    val updatedAt: Long = nowMillis(),
    val deletedAt: Long? = null,
)

@kotlinx.serialization.Serializable
@Entity(indices = [Index(value = ["epochDay"], unique = true)])
data class WeightEntry(
    @PrimaryKey val id: String = newId(),
    val epochDay: Long,
    val kg: Double,
    val updatedAt: Long = nowMillis(),
    val deletedAt: Long? = null,
)

@kotlinx.serialization.Serializable
@Entity
data class GroceryItem(
    @PrimaryKey val id: String = newId(),
    val name: String,
    val qty: String = "",
    val checked: Boolean = false,
    val updatedAt: Long = nowMillis(),
    val deletedAt: Long? = null,
)

@kotlinx.serialization.Serializable
@Entity
data class Setting(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long = nowMillis(),
    val deletedAt: Long? = null,
)
