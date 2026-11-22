package com.rapido.captain40k

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val folderName = "Rapido_40K_Tracker"
    private val fileName = "RAPIDO_CAPTAIN_26DAY_TRACKER.csv"
    private var lastSavedUri: Uri? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(ExcelFileBridge(), "AndroidExcelBridge")

        webView.loadUrl("file:///android_asset/index.html")
    }

    inner class ExcelFileBridge {

        @JavascriptInterface
        fun saveCsvFile(csvContent: String): String {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = contentResolver
                    val relativePath = Environment.DIRECTORY_DOCUMENTS + "/" + folderName

                    val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?"
                    val selectionArgs = arrayOf(fileName, "$relativePath/")
                    resolver.delete(MediaStore.Files.getContentUri("external"), selection, selectionArgs)

                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    }

                    val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { out ->
                            out.write(csvContent.toByteArray(Charsets.UTF_8))
                        }
                        lastSavedUri = uri
                        "✅ Overwritten Documents/$folderName/$fileName"
                    } else {
                        "⚠️ Could not create CSV in Documents"
                    }
                } else {
                    val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    val targetDir = File(docsDir, folderName)
                    if (!targetDir.exists()) targetDir.mkdirs()
                    val file = File(targetDir, fileName)
                    file.writeText(csvContent, Charsets.UTF_8)
                    lastSavedUri = Uri.fromFile(file)
                    "✅ Overwritten Documents/$folderName/$fileName"
                }
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }

        @JavascriptInterface
        fun readCsvFile(): String {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = contentResolver
                    val relativePath = Environment.DIRECTORY_DOCUMENTS + "/" + folderName + "/"
                    val projection = arrayOf(MediaStore.MediaColumns._ID)
                    val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?"
                    val selectionArgs = arrayOf(fileName, relativePath)

                    resolver.query(
                        MediaStore.Files.getContentUri("external"),
                        projection,
                        selection,
                        selectionArgs,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val id = cursor.getLong(0)
                            val uri = Uri.withAppendedPath(MediaStore.Files.getContentUri("external"), id.toString())
                            lastSavedUri = uri
                            return resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.readText() ?: ""
                        }
                    }
                    ""
                } else {
                    val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    val file = File(File(docsDir, folderName), fileName)
                    if (file.exists()) file.readText(Charsets.UTF_8) else ""
                }
            } catch (e: Exception) {
                ""
            }
        }

        @JavascriptInterface
        fun openInExcel() {
            runOnUiThread {
                val uri = lastSavedUri
                if (uri == null) {
                    Toast.makeText(this@MainActivity, "Save a day entry first!", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "text/csv")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(intent, "Open Tracker in Excel / Sheets"))
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Install Microsoft Excel, WPS Office, or Google Sheets to view CSV.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
