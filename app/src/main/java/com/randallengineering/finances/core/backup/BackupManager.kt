package com.randallengineering.finances.core.backup

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

/**
 * Local backup/export helpers. All export is of the user's real data.
 * A single cache file is written for sharing; MediaStore is used for a
 * durable "Save to Downloads" copy on API 29+.
 */
object BackupManager {

    private fun shareableFile(context: Context, filename: String, content: String): File {
        val file = File(context.cacheDir, filename)
        file.writeText(content)
        return file
    }

    /** Create a share intent (Files/Save to Drive/email/…) via FileProvider. */
    fun shareCsv(context: Context, csv: String, filename: String) {
        val file = shareableFile(context, filename, csv)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, filename)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(share, "Export $filename")
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(chooser)
    }

    /**
     * Persist a durable copy to the Downloads folder via MediaStore.
     * Returns the resulting Uri, or null if the platform is too old.
     */
    fun saveCsvToDownloads(context: Context, csv: String, filename: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Pre-29 public Downloads writes require WRITE_EXTERNAL_STORAGE; rely
            // on the share intent instead for older devices.
            return null
        }
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/RandallFinances")
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) } ?: run {
            resolver.delete(uri, null, null)
            return null
        }
        return uri
    }

    /** Write the CSV to a cache file and return it (used by Drive backup). */
    fun writeCsvToCache(context: Context, csv: String, filename: String): File =
        shareableFile(context, filename, csv)
}
