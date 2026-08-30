package com.example.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

object PhotoStore {
    // ponytail: copia sin comprimir; usar BitmapFactory+compress si las fotos pesan mucho
    fun save(context: Context, uri: Uri): String? = try {
        val dir = File(context.filesDir, "kapta_fotos").apply { mkdirs() }
        val dest = File(dir, "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { input.copyTo(it) }
        }
        if (dest.exists() && dest.length() > 0) "file://${dest.absolutePath}" else null
    } catch (_: Exception) {
        null
    }

    fun delete(path: String) {
        if (!path.startsWith("file://")) return
        try { File(path.removePrefix("file://")).delete() } catch (_: Exception) { }
    }
}
