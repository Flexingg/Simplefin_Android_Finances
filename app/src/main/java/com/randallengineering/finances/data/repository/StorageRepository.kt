package com.randallengineering.finances.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.randallengineering.finances.core.network.Resource
import kotlinx.coroutines.tasks.await

class StorageRepository(
    private val storage: FirebaseStorage
) {
    suspend fun uploadReceipt(
        userId: String,
        transactionId: String,
        fileName: String,
        uri: Uri,
        mimeType: String = "image/jpeg"
    ): Resource<String> {
        return try {
            val safeUserId = userId.ifBlank { "anonymous" }
            val path = "receipts/$safeUserId/$transactionId/$fileName"
            val ref = storage.reference.child(path)
            val metadata = StorageMetadata.Builder()
                .setContentType(mimeType)
                .build()

            ref.putFile(uri, metadata).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Resource.Success(downloadUrl)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error uploading receipt", e)
        }
    }
}
