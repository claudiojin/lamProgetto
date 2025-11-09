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

/**
 * 照片管理工具
 *
 * 负责照片的保存、加载、删除
 */
object PhotoManager {

    private const val TAG = "PhotoManager"
    private const val PHOTOS_DIR = "trip_photos"
    private const val MAX_IMAGE_SIZE = 1920  // 最大尺寸（压缩）

    /**
     * 获取照片存储目录
     */
    fun getPhotosDirectory(context: Context): File {
        val dir = File(context.filesDir, PHOTOS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * 保存照片到本地
     *
     * @param context 上下文
     * @param uri 照片URI（从相册选择或相机拍摄）
     * @return 保存后的文件路径
     */
    fun savePhoto(context: Context, uri: Uri): String? {
        try {
            // 1. 读取图片
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                Log.e(TAG, "❌ 无法解码图片")
                return null
            }

            // 2. 压缩图片（如果太大）
            val compressedBitmap = compressBitmap(bitmap)

            // 3. 生成文件名
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val fileName = "IMG_$timestamp.jpg"

            // 4. 保存到文件
            val file = File(getPhotosDirectory(context), fileName)
            FileOutputStream(file).use { out ->
                compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            // 5. 回收Bitmap
            if (bitmap != compressedBitmap) {
                bitmap.recycle()
            }
            compressedBitmap.recycle()

            Log.d(TAG, "✅ 照片已保存: ${file.absolutePath}")
            return file.absolutePath

        } catch (e: IOException) {
            Log.e(TAG, "❌ 保存照片失败: ${e.message}", e)
            return null
        }
    }

    /**
     * 压缩图片（如果尺寸过大）
     */
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

    /**
     * 删除照片文件
     */
    fun deletePhoto(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            val deleted = file.delete()
            if (deleted) {
                Log.d(TAG, "✅ 照片已删除: $filePath")
            } else {
                Log.w(TAG, "⚠️ 照片删除失败: $filePath")
            }
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "❌ 删除照片异常: ${e.message}", e)
            false
        }
    }

    /**
     * 获取照片文件
     */
    fun getPhotoFile(filePath: String): File {
        return File(filePath)
    }

    /**
     * 检查照片文件是否存在
     */
    fun photoExists(filePath: String): Boolean {
        return File(filePath).exists()
    }
}