package dev.dwm.liftlog.domain

import dev.dwm.liftlog.data.db.Exercise

/**
 * The exercise seed merges two vocabularies (wger capitalized, free-exercise-db lowercase), so
 * "Kettlebell" and "kettlebells" or "body only" and "none (bodyweight exercise)" all appear. Every
 * equipment-aware feature (picker filter, swap, plan generator) goes through these tags instead of
 * matching raw strings.
 */
fun equipmentTags(raw: String): Set<String> {
    val s = raw.lowercase()
    val tags = mutableSetOf<String>()
    if ("barbell" in s || "sz-bar" in s || "e-z curl" in s) tags += "barbell"
    if ("dumbbell" in s) tags += "dumbbell"
    if ("kettlebell" in s) tags += "kettlebell"
    if ("body only" in s || "bodyweight" in s || "pull-up bar" in s || "gym mat" in s) tags += "bodyweight"
    if ("band" in s) tags += "bands"
    if ("cable" in s) tags += "cable"
    if ("machine" in s) tags += "machine"
    if ("bench" in s) tags += "bench"
    if ("trx" in s) tags += "trx"
    if (tags.isEmpty()) tags += "other"
    return tags
}

enum class Kit { ALL, HOME }

/** What a dumbbell/kettlebell home gym can actually do. */
private val HOME_TAGS = setOf("dumbbell", "kettlebell", "bodyweight", "bands", "bench", "trx")

fun fitsKit(exercise: Exercise, kit: Kit): Boolean =
    kit == Kit.ALL || equipmentTags(exercise.equipment).any { it in HOME_TAGS }

private val STOP_WORDS = setOf("with", "the", "and", "one", "two", "your")

private fun words(text: String): Set<String> = text.lowercase()
    .split(',', ' ', '/', '-', '(', ')')
    .map { it.trim() }
    .filter { it.length > 2 && it !in STOP_WORDS }
    .toSet()

/**
 * Alternatives for the same job: same category first, then shared muscles, restricted to the kit.
 * Deliberately dumb string matching — the seed's muscle field is free text.
 */
fun swapCandidates(current: Exercise, all: List<Exercise>, kit: Kit, limit: Int = 5): List<Exercise> {
    // Template-created exercises have blank muscles and category, so name words count too —
    // otherwise "Pull-up" has nothing to match on and swap comes up empty.
    val muscleWords = words(current.muscles)
    val nameWords = words(current.name)
    return all.asSequence()
        .filter { it.id != current.id && it.deletedAt == null && fitsKit(it, kit) }
        .map { ex ->
            var score = muscleWords.count { it in words(ex.muscles) }
            score += 2 * nameWords.count { it in words(ex.name) }
            if (ex.category.equals(current.category, ignoreCase = true) && ex.category.isNotBlank()) score += 3
            ex to score
        }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }
        .map { it.first }
        // the seed has near-duplicate rows for the same movement — one entry each is enough
        .distinctBy { it.name.lowercase() }
        .take(limit)
        .toList()
}
