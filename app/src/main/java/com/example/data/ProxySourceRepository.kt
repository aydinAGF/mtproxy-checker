package com.example.data

import kotlinx.coroutines.flow.Flow

class ProxySourceRepository(private val dao: ProxySourceDao) {
    val allSources: Flow<List<ProxySource>> = dao.getAllSources()

    suspend fun insertSource(source: ProxySource) {
        dao.insertSource(source)
    }

    suspend fun deleteSource(source: ProxySource) {
        dao.deleteSource(source)
    }
    
    suspend fun initializeDefaultIfNeeded() {
        if (dao.getSourcesCount() == 0) {
            dao.insertSource(
                ProxySource(
                    url = "https://raw.githubusercontent.com/SoliSpirit/mtproto/master/all_proxies.txt",
                    name = "SoliSpirit Default",
                    isDefault = true
                )
            )
        }
    }
}
