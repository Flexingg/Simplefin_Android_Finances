package com.randallengineering.finances.core.backup

import android.content.Context
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.randallengineering.finances.core.network.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Real Google Drive backup via the Drive REST API (v3). Uploads the user's CSV
 * backup to a "RandallFinances" folder they can see in Drive, using the
 * `drive.file` scope (app-created files only). Errors surface as a readable
 * message so the user knows whether to authorize Drive or enable it in console.
 */
object DriveBackupManager {

    const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    const val FOLDER_NAME = "RandallFinances"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Get a Drive-scoped access token for the last signed-in Google account.
     * Throws GoogleAuthException when the account isn't authorized for Drive
     * (the caller should then launch a Drive-scoped sign-in to get consent).
     */
    @Throws(GoogleAuthException::class)
    fun getDriveToken(context: Context): String {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: throw GoogleAuthException("Not signed in to Google")
        val email = account.email ?: throw GoogleAuthException("Signed-in account has no email")
        return GoogleAuthUtil.getToken(context, email, "oauth2:$DRIVE_SCOPE")
    }

    /** True when a signed-in account exists (regardless of Drive scope). */
    fun hasAccount(context: Context): Boolean =
        GoogleSignIn.getLastSignedInAccount(context)?.let { it.email != null } == true

    /** Upload the CSV to the user's Drive under a "RandallFinances" folder. */
    suspend fun backupCsv(context: Context, csv: String, filename: String): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val token = getDriveToken(context)
            val folderId = ensureFolder(token)
            val url = uploadFile(token, folderId, filename, csv)
            Resource.Success(url)
        } catch (e: GoogleAuthException) {
            Resource.Error("Drive access not authorized. Tap Backup again and choose your Google account to grant access.")
        } catch (e: Exception) {
            Resource.Error("Drive backup failed: ${e.localizedMessage ?: e.message}")
        }
    }

    private fun authHeader(token: String) = "Bearer $token"
    private val jsonType = "application/json; charset=UTF-8".toMediaType()
    private val csvType = "text/csv".toMediaType()

    private fun ensureFolder(token: String): String {
        // Look for an existing folder with our name.
        val query = java.net.URLEncoder.encode("name='$FOLDER_NAME' and mimeType='application/vnd.google-apps.folder' and trashed=false", "UTF-8")
        val listReq = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files?q=$query&fields=files(id,name)&spaces=drive")
            .header("Authorization", authHeader(token))
            .get()
            .build()
        client.newCall(listReq).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            val arr = JSONObject(body).optJSONArray("files")
            if (arr != null && arr.length() > 0) {
                return arr.getJSONObject(0).getString("id")
            }
        }
        // Create it.
        val meta = JSONObject()
            .put("name", FOLDER_NAME)
            .put("mimeType", "application/vnd.google-apps.folder")
        val createReq = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files?fields=id")
            .header("Authorization", authHeader(token))
            .header("Content-Type", jsonType.toString())
            .post(meta.toString().toRequestBody(jsonType))
            .build()
        client.newCall(createReq).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw Exception("folder create ${resp.code}: $body")
            return JSONObject(body).getString("id")
        }
    }

    private fun uploadFile(token: String, folderId: String, name: String, csv: String): String {
        val boundary = "RF_BOUNDARY_${System.currentTimeMillis()}"
        val meta = JSONObject()
            .put("name", name)
            .put("parents", org.json.JSONArray(listOf(folderId)))
            .put("mimeType", "text/csv")

        val body = StringBuilder()
        body.append("--$boundary\r\n")
        body.append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
        body.append(meta.toString()).append("\r\n")
        body.append("--$boundary\r\n")
        body.append("Content-Type: text/csv\r\n\r\n")
        body.append(csv).append("\r\n")
        body.append("--$boundary--\r\n")

        val url = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=webViewLink,id"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", authHeader(token))
            .header("Content-Type", "multipart/related; boundary=$boundary")
            .post(body.toString().toRequestBody("multipart/related; boundary=$boundary".toMediaType()))
            .build()
        client.newCall(request).execute().use { resp ->
            val out = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw Exception("upload ${resp.code}: $out")
            val json = JSONObject(out)
            return json.optString("webViewLink").ifBlank {
                "https://drive.google.com/drive/my-drive"
            }
        }
    }
}
