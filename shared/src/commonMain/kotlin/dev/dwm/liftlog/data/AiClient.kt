package dev.dwm.liftlog.data

import dev.dwm.liftlog.data.db.Food
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class ParsedFood(
    val name: String,
    val grams: Double,
    val kcal: Double,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
)

@Serializable
private data class ChatChoice(val message: ChatMessage)

@Serializable
private data class ChatMessage(val content: String? = null)

@Serializable
private data class ChatResponse(val choices: List<ChatChoice> = emptyList())

// Out-of-the-box defaults: paste one OpenRouter key and photo/text food logging works.
// Endpoint/model settings remain as optional overrides (e.g. local Ollama).
const val DEFAULT_AI_ENDPOINT = "https://openrouter.ai/api/v1"
const val DEFAULT_AI_MODEL = "google/gemini-2.5-flash"

/**
 * OpenAI-compatible chat client — works with local Ollama (`http://host:11434/v1`),
 * OpenRouter, etc. Vision via image_url data URI (Ollama vision models supported).
 */
class AiClient(
    engineClient: HttpClient,
    private val endpoint: String,
    private val model: String,
    private val apiKey: String? = null,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = engineClient.config {
        install(ContentNegotiation) { json(this@AiClient.json) }
        install(HttpTimeout) { requestTimeoutMillis = 120_000 }
    }

    suspend fun chat(prompt: String, imageB64Jpeg: String? = null): String {
        val body = buildJsonObject {
            put("model", model)
            put("messages", buildJsonArray {
                addJsonObject {
                    put("role", "user")
                    if (imageB64Jpeg == null) {
                        put("content", prompt)
                    } else {
                        put("content", buildJsonArray {
                            addJsonObject { put("type", "text"); put("text", prompt) }
                            addJsonObject {
                                put("type", "image_url")
                                put("image_url", buildJsonObject {
                                    put("url", "data:image/jpeg;base64,$imageB64Jpeg")
                                })
                            }
                        })
                    }
                }
            })
        }
        val resp: ChatResponse = client.post("${endpoint.trimEnd('/')}/chat/completions") {
            apiKey?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()
        return resp.choices.firstOrNull()?.message?.content.orEmpty()
    }

    /** Parse a natural-language meal description (or photo) into foods with estimated nutrition. */
    suspend fun parseFoods(description: String, imageB64Jpeg: String? = null): List<ParsedFood> {
        val prompt = """
            Identify each distinct food ${if (imageB64Jpeg != null) "in this photo" else "in this meal description"} and estimate its nutrition.
            ${if (imageB64Jpeg == null) "Meal: \"$description\"" else ""}
            Estimate portion size in grams using visible cues: a dinner plate is ~27 cm across, use
            cutlery or hands for scale, and typical cooked food densities. Keep kcal and macros
            consistent with the grams you state. When unsure, prefer common serving sizes over guesses.
            Include any visible brand or product name in the food name (e.g. "Optimum Nutrition Gold
            Standard Whey", not "protein shake") so it can be matched to a product photo.
            Reply with ONLY a JSON array, no prose:
            [{"name": "...", "grams": <portion grams>, "kcal": <kcal for that portion>, "protein": <g>, "carbs": <g>, "fat": <g>}]
        """.trimIndent()
        val content = chat(prompt, imageB64Jpeg)
        val start = content.indexOf('[')
        val end = content.lastIndexOf(']')
        if (start < 0 || end <= start) return emptyList()
        return runCatching {
            json.decodeFromString<List<ParsedFood>>(content.substring(start, end + 1))
        }.getOrDefault(emptyList())
    }

    /**
     * A full day of meals hitting the macro targets, in the same JSON shape [parseFoods] returns —
     * so the result renders and logs through the existing parsed-food path.
     */
    suspend fun mealPlan(goal: String, kcal: Double, protein: Double, carbs: Double, fat: Double): List<ParsedFood> {
        val prompt = """
            Plan one day of simple, cheap, home-cookable meals for a "$goal" goal.
            Hit these daily targets as closely as you can: ${kcal.toInt()} kcal,
            ${protein.toInt()}g protein, ${carbs.toInt()}g carbs, ${fat.toInt()}g fat.
            Spread it across breakfast, lunch, dinner and one snack. Name each item plainly
            (e.g. "Chicken breast", "Greek yogurt") and give a realistic portion in grams.
            Reply with ONLY a JSON array, no prose:
            [{"name": "...", "grams": <portion grams>, "kcal": <kcal for that portion>, "protein": <g>, "carbs": <g>, "fat": <g>}]
        """.trimIndent()
        val content = chat(prompt)
        val start = content.indexOf('[')
        val end = content.lastIndexOf(']')
        if (start < 0 || end <= start) return emptyList()
        return runCatching {
            json.decodeFromString<List<ParsedFood>>(content.substring(start, end + 1))
        }.getOrDefault(emptyList())
    }

    suspend fun suggestWorkout(recentSummary: String): String = chat(
        """
        You are a strength coach. Based on my recent training below, suggest today's workout:
        exercises, sets x reps, and weights. Be concise (max 10 lines).

        $recentSummary
        """.trimIndent()
    )
}

/** Build a client from settings, falling back to baked-in defaults. Fails only when a key is required and missing. */
suspend fun aiClient(db: dev.dwm.liftlog.data.db.AppDatabase): Result<AiClient> {
    val endpoint = db.settingDao().get("aiEndpoint")?.takeIf { it.isNotBlank() } ?: DEFAULT_AI_ENDPOINT
    val model = db.settingDao().get("aiModel")?.takeIf { it.isNotBlank() } ?: DEFAULT_AI_MODEL
    val key = db.settingDao().get("aiApiKey")
    if (endpoint == DEFAULT_AI_ENDPOINT && key.isNullOrBlank()) {
        return Result.failure(IllegalStateException("Paste your OpenRouter API key in More → AI (one-time setup)"))
    }
    return Result.success(AiClient(httpClient(), endpoint, model, key))
}

/** Convert an AI-parsed portion to a per-100g Food row plus its logged grams. */
fun ParsedFood.toFood(): Food {
    val factor = if (grams > 0) 100.0 / grams else 1.0
    return Food(
        name = name,
        kcal = kcal * factor,
        protein = protein * factor,
        carbs = carbs * factor,
        fat = fat * factor,
        custom = true,
    )
}
