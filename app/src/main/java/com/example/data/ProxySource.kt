package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "proxy_sources")
data class ProxySource(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val name: String,
    val isDefault: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
