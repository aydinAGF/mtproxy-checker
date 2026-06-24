package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProxySourceDao {
    @Query("SELECT * FROM proxy_sources ORDER BY timestamp DESC")
    fun getAllSources(): Flow<List<ProxySource>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: ProxySource)

    @Delete
    suspend fun deleteSource(source: ProxySource)
    
    @Query("SELECT COUNT(*) FROM proxy_sources")
    suspend fun getSourcesCount(): Int
}
