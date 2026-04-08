package com.example.servicemaintainreminder.ui

import android.app.Application
import androidx.lifecycle.*
import com.example.servicemaintainreminder.data.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ServiceRepository
    val allItems: LiveData<List<Item>>
    val allHistory: LiveData<List<ServiceHistory>>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ServiceRepository(db.itemDao(), db.serviceHistoryDao())
        allItems = repository.allItems.asLiveData()
        allHistory = repository.allHistory.asLiveData()
    }

    fun insertItem(item: Item) = viewModelScope.launch {
        repository.insertItem(item)
    }

    fun updateItem(item: Item) = viewModelScope.launch {
        repository.updateItem(item)
    }

    fun deleteItem(item: Item) = viewModelScope.launch {
        repository.deleteItem(item)
    }

    fun searchItems(query: String): LiveData<List<Item>> {
        return repository.searchItems(query).asLiveData()
    }

    fun getHistory(itemId: Long): LiveData<List<ServiceHistory>> {
        return repository.getHistoryByItemId(itemId).asLiveData()
    }

    fun addHistory(history: ServiceHistory) = viewModelScope.launch {
        repository.insertHistory(history)
    }

    fun deleteHistory(history: ServiceHistory) = viewModelScope.launch {
        repository.deleteHistory(history)
    }

    fun updateHistory(history: ServiceHistory) = viewModelScope.launch {
        repository.updateHistory(history)
    }
}
