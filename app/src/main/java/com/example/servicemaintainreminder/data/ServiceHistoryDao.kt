package com.example.servicemaintainreminder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceHistoryDao {
    @Query("SELECT * FROM service_history WHERE itemId = :itemId ORDER BY serviceDate DESC")
    fun getHistoryByItemId(itemId: Long): Flow<List<ServiceHistory>>

    @Query("SELECT * FROM service_history ORDER BY serviceDate DESC")
    fun getAllHistory(): Flow<List<ServiceHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ServiceHistory)

    @Delete
    suspend fun deleteHistory(history: ServiceHistory)
}
