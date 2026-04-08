package com.example.servicemaintainreminder.data

import kotlinx.coroutines.flow.Flow

class ServiceRepository(private val itemDao: ItemDao, private val serviceHistoryDao: ServiceHistoryDao) {

    val allItems: Flow<List<Item>> = itemDao.getAllItems()
    
    val allHistory: Flow<List<ServiceHistory>> = serviceHistoryDao.getAllHistory()

    suspend fun getItemById(id: Long): Item? = itemDao.getItemById(id)

    suspend fun insertItem(item: Item): Long = itemDao.insertItem(item)

    suspend fun updateItem(item: Item) = itemDao.updateItem(item)

    suspend fun deleteItem(item: Item) = itemDao.deleteItem(item)

    fun searchItems(query: String): Flow<List<Item>> = itemDao.searchItems(query)

    fun getHistoryByItemId(itemId: Long): Flow<List<ServiceHistory>> = 
        serviceHistoryDao.getHistoryByItemId(itemId)

    suspend fun insertHistory(history: ServiceHistory) = serviceHistoryDao.insertHistory(history)

    suspend fun deleteHistory(history: ServiceHistory) = serviceHistoryDao.deleteHistory(history)

    suspend fun updateHistory(history: ServiceHistory) = serviceHistoryDao.updateHistory(history)
}
