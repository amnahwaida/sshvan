package com.sshvan.tunnelmanager.presentation.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sshvan.tunnelmanager.domain.usecase.ProfileExportImportUseCase
import com.sshvan.tunnelmanager.domain.repository.ProfileRepository
import com.sshvan.tunnelmanager.domain.model.ConnectionProfile
import kotlinx.coroutines.flow.first
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportImportUseCase: ProfileExportImportUseCase,
    private val repository: ProfileRepository
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent

    val profiles: StateFlow<List<ConnectionProfile>> = repository.getAllProfiles()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    fun exportProfiles(uri: Uri, selectedProfileIds: Set<Long>, exportAsLocked: Boolean) {
        viewModelScope.launch {
            val result = exportImportUseCase.exportProfiles(uri, selectedProfileIds, exportAsLocked)
            if (result.isSuccess) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Profiles exported successfully"))
            } else {
                _uiEvent.emit(UiEvent.ShowSnackbar("Failed to export: ${result.exceptionOrNull()?.message}"))
            }
        }
    }

    fun importProfiles(uri: Uri) {
        viewModelScope.launch {
            val result = exportImportUseCase.importProfiles(uri)
            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                _uiEvent.emit(UiEvent.ShowSnackbar("Successfully imported $count profiles"))
            } else {
                _uiEvent.emit(UiEvent.ShowSnackbar("Failed to import: ${result.exceptionOrNull()?.message}"))
            }
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }
}
