package com.mdmresetremover.app

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

object ApkDownloader {

    /**
     * Downloads an APK from the given URL to the app's external files directory.
     * Returns the File on success, null on failure.
     */
    fun downloadApk(
        context: Context,
        url: String,
        fileName: String
    ): File? {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.cacheDir
        val apkFile = File(downloadDir, fileName)

        // Delete existing file if present
        if (apkFile.exists()) {
            apkFile.delete()
        }

        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputFile: RandomAccessFile? = null

        return try {
            val urlObj = URL(url)
            connection = urlObj.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.instanceFollowRedirects = true
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                null
            } else {
                inputStream = connection.inputStream
                outputFile = RandomAccessFile(apkFile, "rw")

                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputFile.write(buffer, 0, bytesRead)
                }

                outputFile.getChannel().force(true)
                apkFile
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            inputStream?.close()
            outputFile?.close()
            connection?.disconnect()
        }
    }
}