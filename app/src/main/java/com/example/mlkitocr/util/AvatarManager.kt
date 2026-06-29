package com.example.mlkitocr.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class AvatarManager(context: Context) {

    private val avatarFile: File

    init {
        avatarFile = File(File(context.filesDir, "avatar"), "user_avatar.jpg")
        avatarFile.parentFile?.mkdirs()
    }

    fun save(bitmap: Bitmap) {
        FileOutputStream(avatarFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
    }

    fun load(): Bitmap? {
        return if (avatarFile.exists()) {
            BitmapFactory.decodeFile(avatarFile.absolutePath)
        } else null
    }

    fun exists(): Boolean = avatarFile.exists()

    fun getCameraUri(context: Context): Uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            avatarFile
        )

    fun delete() {
        avatarFile.delete()
    }
}
