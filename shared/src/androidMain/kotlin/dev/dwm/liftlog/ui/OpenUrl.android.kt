package dev.dwm.liftlog.ui

import android.content.Intent
import androidx.core.net.toUri

actual fun openUrl(url: String) {
    val ctx = appActivity ?: appContext ?: return
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        if (appActivity == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }
}
