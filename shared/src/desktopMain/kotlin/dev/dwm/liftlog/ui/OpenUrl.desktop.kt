package dev.dwm.liftlog.ui

import java.awt.Desktop
import java.net.URI

actual fun openUrl(url: String) {
    runCatching { Desktop.getDesktop().browse(URI(url)) }
}
