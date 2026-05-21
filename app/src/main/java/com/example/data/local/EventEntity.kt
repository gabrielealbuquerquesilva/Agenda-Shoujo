package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val dateTime: Long, // epoch timestamp
    val category: String, // "Estudo", "Amor", "Lazer", "Chá", "Segredo", "Mágico"
    val isCompleted: Boolean = false,
    val isGoogleEvent: Boolean = false,
    val googleEventId: String? = null
)
