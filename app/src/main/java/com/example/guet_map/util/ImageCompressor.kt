package com.example.guet_map.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

object ImageCompressor {

    private const val MAX_DIMENSION = 1280
    private const val JPEG_QUALITY = 82

    fun compressToJpegBytes(context: Context, uri: Uri): ByteArray {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return ByteArray(0)

        val scaled = scaleDownIfNeeded(bitmap, MAX_DIMENSION)
        if (scaled !== bitmap) bitmap.recycle()

        return ByteArrayOutputStream().use { stream ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            scaled.recycle()
            stream.toByteArray()
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sample = 1
        while (width / sample > maxDim * 2 || height / sample > maxDim * 2) {
            sample *= 2
        }
        return sample.coerceAtLeast(1)
    }

    private fun scaleDownIfNeeded(source: Bitmap, maxDim: Int): Bitmap {
        val w = source.width
        val h = source.height
        val maxSide = maxOf(w, h)
        if (maxSide <= maxDim) return source
        val ratio = maxDim.toFloat() / maxSide
        val nw = (w * ratio).toInt().coerceAtLeast(1)
        val nh = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, nw, nh, true)
    }
}
