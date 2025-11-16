package com.example.progetto.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


object PhotoManager {

    private const val TAG = "PhotoManager"
    private const val PHOTOS_DIR = "trip_photos"
    private const val MAX_IMAGE_SIZE = 1920


    fun getPhotosDirectory(context: Context): File {
        val dir = File(context.filesDir, PHOTOS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }


    fun savePhoto(context: Context, uri: Uri): String? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                Log.e(TAG, "❌ Decodifica fallita")
                return null
            }

            val compressedBitmap = compressBitmap(bitmap)

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val fileName = "IMG_$timestamp.jpg"

            val file = File(getPhotosDirectory(context), fileName)
            FileOutputStream(file).use { out ->
                compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            if (bitmap != compressedBitmap) {
                bitmap.recycle()
            }
            compressedBitmap.recycle()

            Log.d(TAG, "✅ Salvato: ${file.absolutePath}")
            return file.absolutePath

        } catch (e: IOException) {
            Log.e(TAG, "❌ Errore: ${e.message}", e)
            return null
        }
    }


    private fun compressBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= MAX_IMAGE_SIZE && height <= MAX_IMAGE_SIZE) {
            return bitmap
        }

        val scale = if (width > height) {
            MAX_IMAGE_SIZE.toFloat() / width
        } else {
            MAX_IMAGE_SIZE.toFloat() / height
        }

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }


    fun deletePhoto(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            val deleted = file.delete()
            if (deleted) {
                Log.d(TAG, "✅ Eliminata: $filePath")
            } else {
                Log.w(TAG, "⚠️ Eliminazione fallita: $filePath")
            }
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore: ${e.message}", e)
            false
        }
    }


    fun getPhotoFile(filePath: String): File {
        return File(filePath)
    }


    fun photoExists(filePath: String): Boolean {
        return File(filePath).exists()
    }
}