package com.example.servicemaintainreminder.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "service_history",
    foreignKeys = [
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId")]
)
data class ServiceHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val serviceDate: Long,
    val description: String,
    val cost: Double
)
