package dev.dwm.liftlog.ui

/** Open a link in the system browser. Best-effort: silently does nothing if it fails. */
expect fun openUrl(url: String)

/**
 * Demo video for a movement. A YouTube search beats storing 1276 clip URLs that would rot —
 * the top results for "<exercise> form" are the demos.
 */
fun demoVideoUrl(exerciseName: String): String {
    val q = exerciseName.trim().replace(Regex("[^A-Za-z0-9 ]"), "").replace(" ", "+")
    return "https://www.youtube.com/results?search_query=$q+form"
}
