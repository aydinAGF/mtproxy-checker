package com.example.ui.sources

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ProxySource
import com.example.data.ProxySourceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProxySourcesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ProxySourceRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ProxySourceRepository(database.proxySourceDao())
        
        viewModelScope.launch {
            repository.initializeDefaultIfNeeded()
        }
    }

    val sources: StateFlow<List<ProxySource>> = repository.allSources
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addSource(name: String, url: String) {
        viewModelScope.launch {
            repository.insertSource(ProxySource(name = name, url = url))
        }
    }

    fun deleteSource(source: ProxySource) {
        viewModelScope.launch {
            repository.deleteSource(source)
        }
    }
}
