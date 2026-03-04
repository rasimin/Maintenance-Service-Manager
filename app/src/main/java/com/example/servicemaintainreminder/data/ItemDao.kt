package com.example.servicemaintainreminder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY nextServiceDate ASC")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM items")
    suspend fun getAllItemsOnce(): List<Item>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: Long): Item?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item): Long

    @Update
    suspend fun updateItem(item: Item)

    @Delete
    suspend fun deleteItem(item: Item)

    @Query("SELECT * FROM items WHERE name LIKE '%' || :searchQuery || '%'")
    fun searchItems(searchQuery: String): Flow<List<Item>>
}
