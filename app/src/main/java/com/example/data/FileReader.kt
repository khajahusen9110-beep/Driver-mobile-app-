package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** Reads a picked document. Photos are scaled down and re-encoded as JPEG; PDFs are sent as they are. */
object FileReader {
    private const val MAX_BYTES = 10 * 1024 * 1024 // the storage bucket's limit
    private const val MAX_EDGE_PX = 1800

    suspend fun read(context: Context, uri: Uri): PickedFile = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/jpeg"
        if (mime == "application/pdf") {
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw ApiException("Could not read the file.")
            if (bytes.size > MAX_BYTES) throw ApiException("The PDF is larger than 10 MB. Please pick a smaller file.")
            return@withContext PickedFile(bytes, mime, "pdf")
        }
        if (!mime.startsWith("image/")) throw ApiException("Please pick a photo or a PDF.")

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_EDGE_PX * 2) sample *= 2
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw ApiException("Could not read the photo.")

        val scale = MAX_EDGE_PX.toFloat() / maxOf(bitmap.width, bitmap.height)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else bitmap

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        if (scaled !== bitmap) scaled.recycle()
        bitmap.recycle()
        PickedFile(out.toByteArray(), "image/jpeg", "jpg")
    }
}
