package com.randallengineering.finances.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GenericRecordDao {

    @Query("SELECT * FROM domain_records WHERE kind = :kind ORDER BY recordId")
    fun observe(kind: String): Flow<List<DomainRecordRow>>

    @Query("SELECT * FROM domain_records WHERE kind = :kind")
    suspend fun getAll(kind: String): List<DomainRecordRow>

    @Query("SELECT * FROM domain_records WHERE kind = :kind AND recordId = :id LIMIT 1")
    suspend fun getById(kind: String, id: String): DomainRecordRow?

    @Query("SELECT COUNT(*) FROM domain_records WHERE kind = :kind")
    suspend fun count(kind: String): Int

    @Upsert
    suspend fun upsertAll(rows: List<DomainRecordRow>)

    @Upsert
    suspend fun upsert(row: DomainRecordRow)

    @Query("DELETE FROM domain_records WHERE kind = :kind AND recordId = :id")
    suspend fun delete(kind: String, id: String)

    @Query("DELETE FROM domain_records WHERE kind = :kind")
    suspend fun clear(kind: String)
}
