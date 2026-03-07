package com.example.servicemaintainreminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val lastServiceDate: Long,
    val originalLastServiceDate: Long = 0L,
    val serviceIntervalValue: Int,
    val serviceIntervalUnit: String, // "Days" or "Months"
    val nextServiceDate: Long,
    val note: String = "",
    val isActive: Boolean = true
)
