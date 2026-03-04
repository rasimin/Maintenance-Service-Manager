package com.example.servicemaintainreminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_history")
data class ServiceHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val serviceDate: Long,
    val description: String,
    val cost: Double
)
