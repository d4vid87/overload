package dev.dwm.liftlog

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import dev.dwm.liftlog.data.db.createDatabase
import dev.dwm.liftlog.ui.App
import dev.dwm.liftlog.ui.CapturedPhoto
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MainActivity : ComponentActivity() {
    private var photoContinuation: ((Boolean) -> Unit)? = null
    private var photoFile: java.io.File? = null

    // full-res capture: TakePicturePreview returned a tiny thumbnail, useless for AI food estimates
    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            photoContinuation?.invoke(ok)
            photoContinuation = null
        }

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    private var importContinuation: ((android.net.Uri?) -> Unit)? = null

    private val pickImport =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            importContinuation?.invoke(uri)
            importContinuation = null
        }

    private var speechContinuation: ((String?) -> Unit)? = null

    private val recognizeSpeech =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val text = result.data
                ?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            speechContinuation?.invoke(text)
            speechContinuation = null
        }

    // downscaled JPEG bytes (≤1536px longest side): full-res capture is useless-large for AI + display
    private fun scaledJpeg(file: java.io.File): ByteArray? = runCatching {
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(file.path, bounds)
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (maxSide / (sample * 2) >= 1536) sample *= 2
        val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = android.graphics.BitmapFactory.decodeFile(file.path, opts) ?: return@runCatching null
        val longest = maxOf(bitmap.width, bitmap.height)
        val scaled = if (longest > 1536) {
            val f = 1536f / longest
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * f).toInt(), (bitmap.height * f).toInt(), true)
        } else bitmap
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        out.toByteArray()
    }.getOrNull()

    override fun onDestroy() {
        if (dev.dwm.liftlog.ui.appActivity === this) dev.dwm.liftlog.ui.appActivity = null
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        dev.dwm.liftlog.ui.appContext = applicationContext
        dev.dwm.liftlog.ui.appActivity = this
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            requestNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        val db = createDatabase(applicationContext)
        val scanner = GmsBarcodeScanning.getClient(this)
        setContent {
            App(
                db,
                scanBarcode = {
                    suspendCancellableCoroutine { cont ->
                        scanner.startScan()
                            .addOnSuccessListener { cont.resume(it.rawValue) }
                            .addOnFailureListener { cont.resume(null) }
                            .addOnCanceledListener { cont.resume(null) }
                    }
                },
                saveExport = { content ->
                    val file = java.io.File(getExternalFilesDir(null), "liftlog-export-${System.currentTimeMillis()}.json")
                    file.writeText(content)
                    file.absolutePath
                },
                loadImport = {
                    val uri = suspendCancellableCoroutine<android.net.Uri?> { cont ->
                        importContinuation = { cont.resume(it) }
                        // */* not application/json: exports land in app-private storage and some
                        // pickers won't surface .json files under a narrow MIME filter
                        runCatching { pickImport.launch(arrayOf("*/*")) }.onFailure {
                            importContinuation = null
                            cont.resume(null)
                        }
                    }
                    uri?.let {
                        contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
                    }
                },
                takePhoto = {
                    val file = java.io.File(cacheDir, "meal-photo.jpg")
                    photoFile = file
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        this, "$packageName.fileprovider", file,
                    )
                    val ok = suspendCancellableCoroutine<Boolean> { cont ->
                        photoContinuation = { cont.resume(it) }
                        runCatching { takePicture.launch(uri) }.onFailure {
                            photoContinuation = null
                            cont.resume(false)
                        }
                    }
                    val bytes = if (ok && file.exists()) scaledJpeg(file) else null
                    bytes?.let {
                        val b64 = Base64.encodeToString(it, Base64.NO_WRAP)
                        // persist a copy so the diary can show the photo the user took (ponytail: no cleanup — personal app)
                        val uriStr = runCatching {
                            val dir = java.io.File(filesDir, "food-photos").apply { mkdirs() }
                            val dest = java.io.File(dir, "${java.util.UUID.randomUUID()}.jpg")
                            dest.writeBytes(it)
                            "file://${dest.absolutePath}"
                        }.getOrNull()
                        CapturedPhoto(b64, uriStr)
                    }
                },
                voiceInput = {
                    suspendCancellableCoroutine { cont ->
                        speechContinuation = { cont.resume(it) }
                        runCatching {
                            recognizeSpeech.launch(
                                android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(
                                        android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                        android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                                    )
                                    putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Say e.g. \"225 for 8\"")
                                }
                            )
                        }.onFailure {
                            speechContinuation = null
                            cont.resume(null)
                        }
                    }
                },
            )
        }
    }
}
