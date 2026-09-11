package dev.dwm.liftlog.data

import dev.dwm.liftlog.data.db.Food
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
private data class OffProduct(
    val code: String? = null,
    @SerialName("product_name") val productName: String? = null,
    val brands: String? = null,
    @SerialName("image_front_small_url") val imageFrontSmallUrl: String? = null,
    // serving_quantity is grams, but OFF returns it as a number for some products and a string for
    // others — JsonPrimitive tolerates both. Typing it String? made those products fail to
    // deserialize, which took the whole search result down with it.
    @SerialName("serving_quantity") val servingQuantity: JsonPrimitive? = null,
    @SerialName("serving_size") val servingSize: String? = null,
    val nutriments: JsonObject? = null,
)

@Serializable
private data class OffSearch(val products: List<OffProduct> = emptyList())

@Serializable
private data class OffProductResponse(val status: Int = 0, val product: OffProduct? = null)

private val MICRO_KEYS = listOf(
    "fiber", "sugars", "salt", "sodium", "calcium", "iron", "potassium",
    "magnesium", "zinc", "vitamin-c", "vitamin-d", "vitamin-a", "vitamin-b12",
    "saturated-fat", "cholesterol",
)

class OpenFoodFacts(engineClient: HttpClient) {
    private val client = engineClient.config {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private fun OffProduct.toFood(): Food? {
        val n = nutriments ?: return null
        fun nutrient(key: String): Double? = n["${key}_100g"]?.jsonPrimitive?.doubleOrNull
        val name = productName?.takeIf { it.isNotBlank() } ?: return null
        val kcal = nutrient("energy-kcal") ?: return null
        val micros = MICRO_KEYS.mapNotNull { k -> nutrient(k)?.let { "\"$k\":$it" } }
        return Food(
            barcode = code,
            name = name,
            brand = brands.orEmpty(),
            kcal = kcal,
            protein = nutrient("proteins") ?: 0.0,
            carbs = nutrient("carbohydrates") ?: 0.0,
            fat = nutrient("fat") ?: 0.0,
            microsJson = if (micros.isEmpty()) null else "{${micros.joinToString(",")}}",
            imageUrl = imageFrontSmallUrl?.takeIf { it.isNotBlank() },
            // ignore nonsense servings (0g, or a "serving" bigger than a kilo)
            servingGrams = servingQuantity?.content?.trim()?.toDoubleOrNull()?.takeIf { it > 0 && it <= 1000 },
            servingLabel = servingSize?.trim()?.takeIf { it.isNotBlank() },
        )
    }

    suspend fun search(query: String): List<Food> = runCatching {
        client.get("https://world.openfoodfacts.org/cgi/search.pl") {
            header("User-Agent", "Overload/0.10 (personal app)")
            parameter("search_terms", query)
            parameter("search_simple", 1)
            parameter("action", "process")
            parameter("json", 1)
            parameter("page_size", 20)
            parameter("fields", "code,product_name,brands,image_front_small_url,serving_quantity,serving_size,nutriments")
        }.body<OffSearch>().products.mapNotNull { it.toFood() }
    }.getOrDefault(emptyList())

    /** Best-effort stock thumbnail for a free-text food name (first search hit with an image). */
    suspend fun imageFor(name: String): String? =
        search(name).firstNotNullOfOrNull { it.imageUrl }

    /** Contribute a product upstream to Open Food Facts (crowdsourced DB, they moderate). */
    suspend fun submitProduct(food: Food, user: String, password: String): Boolean = runCatching {
        val code = food.barcode ?: return false
        client.get("https://world.openfoodfacts.org/cgi/product_jqm2.pl") {
            header("User-Agent", "Overload/0.10 (personal app)")
            parameter("code", code)
            parameter("user_id", user)
            parameter("password", password)
            parameter("product_name", food.name)
            parameter("nutrition_data_per", "100g")
            parameter("nutriment_energy-kcal", food.kcal)
            parameter("nutriment_proteins", food.protein)
            parameter("nutriment_carbohydrates", food.carbs)
            parameter("nutriment_fat", food.fat)
        }
        true
    }.getOrDefault(false)

    suspend fun byBarcode(barcode: String): Food? = runCatching {
        val resp = client.get("https://world.openfoodfacts.org/api/v2/product/$barcode.json") {
            header("User-Agent", "Overload/0.10 (personal app)")
        }.body<OffProductResponse>()
        if (resp.status == 1) resp.product?.toFood() else null
    }.getOrNull()
}
