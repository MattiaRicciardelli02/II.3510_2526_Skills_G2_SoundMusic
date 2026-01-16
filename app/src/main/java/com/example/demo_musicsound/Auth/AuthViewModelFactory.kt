package com.example.demo_musicsound.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.demo_musicsound.community.FirebaseCommunityRepository
import com.example.demo_musicsound.data.LocalBeatDao

class AuthViewModelFactory(
    private val localBeatDao: LocalBeatDao,
    private val repo: FirebaseCommunityRepository? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AuthViewModel(
            localBeatDao = localBeatDao,
            repo = repo
        ) as T
    }
}