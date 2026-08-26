package com.smartagent.app.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

object ImageUtils {
    fun readAndCompress(contentResolver: ContentResolver, uri: Uri): String {
        val original = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not read the selected image")
        val bitmap = BitmapFactory.decodeByteArray(original, 0, original.size)
            ?: error("The selected file is not a supported image")

        val resized = if (bitmap.width > MAX_DIMENSION || bitmap.height > MAX_DIMENSION) {
            val scale = MAX_DIMENSION.toFloat() / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).roundToInt(),
                (bitmap.height * scale).roundToInt(),
                true
            )
        } else {
            bitmap
        }

        val output = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 82, output)
        if (resized !== bitmap) resized.recycle()
        bitmap.recycle()
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private const val MAX_DIMENSION = 1600
}
