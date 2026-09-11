package dev.dwm.liftlog

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dev.dwm.liftlog.data.db.Setting
import dev.dwm.liftlog.data.db.createDatabase
import dev.dwm.liftlog.ui.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private const val DEFAULT_SCALE = 1.5f

fun main() {
    val db = createDatabase()
    val savedScale = runBlocking { db.settingDao().get("uiScale")?.toFloatOrNull() } ?: DEFAULT_SCALE
    application {
        var scale by remember { mutableStateOf(savedScale) }
        val scope = remember { CoroutineScope(Dispatchers.Default) }
        fun setScale(s: Float) {
            scale = s.coerceIn(0.75f, 3f)
            scope.launch { db.settingDao().put(Setting("uiScale", "$scale")) }
        }
        Window(
            onCloseRequest = ::exitApplication,
            title = "Overload",
            state = remember { WindowState(size = DpSize(1280.dp, 900.dp)) },
            onPreviewKeyEvent = { e ->
                if (e.type == KeyEventType.KeyDown && e.isCtrlPressed) {
                    when (e.key) {
                        Key.Equals, Key.Plus -> { setScale(scale + 0.1f); true }
                        Key.Minus -> { setScale(scale - 0.1f); true }
                        Key.Zero -> { setScale(DEFAULT_SCALE); true }
                        else -> false
                    }
                } else false
            },
        ) {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density * scale, base.fontScale)) {
                App(
                    db,
                    saveExport = { content ->
                        val file = java.io.File(
                            System.getProperty("user.home"),
                            "overload-export-${System.currentTimeMillis()}.json",
                        )
                        file.writeText(content)
                        file.absolutePath
                    },
                    loadImport = {
                        // ponytail: Swing chooser — no extra dependency for a rarely used restore
                        val chooser = javax.swing.JFileChooser(System.getProperty("user.home")).apply {
                            dialogTitle = "Choose an Overload backup (.json)"
                        }
                        if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                            chooser.selectedFile?.readText()
                        } else null
                    },
                    uiScale = scale,
                    onUiScale = ::setScale,
                )
            }
        }
    }
}
