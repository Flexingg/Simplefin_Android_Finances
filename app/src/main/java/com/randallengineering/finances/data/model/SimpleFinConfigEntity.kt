package com.randallengineering.finances.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class SimpleFinConfigEntity(
    @DocumentId
    var id: String = "simplefin",
    var accessUrlConfigured: Boolean = false,
    var lastSyncTimestamp: Long = 0L,
    var lastError: String? = null,
    var errorList: List<String> = emptyList()
)
