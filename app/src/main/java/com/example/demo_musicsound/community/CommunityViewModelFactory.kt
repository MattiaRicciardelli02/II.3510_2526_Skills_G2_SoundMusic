package com.example.demo_musicsound.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CommunityViewModelFactory(
    private val repo: FirebaseCommunityRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CommunityViewModel(repo) as T
    }
}
