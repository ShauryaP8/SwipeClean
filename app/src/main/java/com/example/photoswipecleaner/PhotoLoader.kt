package com.example.photoswipecleaner

import android.content.Context
import android.os.Environment
import java.io.File

fun getAllImages(context: Context): List<File> {
    val imageFiles = mutableListOf<File>()
    val storageDir = Environment.getExternalStorageDirectory()

    fun scanDir(dir: File) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                scanDir(file)
            } else if (file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp")) {
                imageFiles.add(file)
            }
        }
    }

    scanDir(storageDir)
    return imageFiles
}