package com.example.demo_musicsound.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.demo_musicsound.data.LocalBeatDao

class CommunityViewModelFactory(
    private val repo: FirebaseCommunityRepository,
    private val beatDao: LocalBeatDao
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CommunityViewModel::class.java)) {
            return CommunityViewModel(
                repo = repo,
                beatDao = beatDao
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}