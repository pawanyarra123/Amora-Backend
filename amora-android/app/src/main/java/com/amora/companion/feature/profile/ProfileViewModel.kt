package com.amora.companion.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amora.companion.core.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val selectedLanguage: Flow<String> = preferencesRepository.selectedLanguage

    fun selectLanguage(lang: String) {
        viewModelScope.launch {
            preferencesRepository.setSelectedLanguage(lang)
        }
    }
}
