package com.sshvan.tunnelmanager.presentation.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sshvan.tunnelmanager.domain.usecase.ProfileExportImportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportImportUseCase: ProfileExportImportUseCase
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent

    fun exportProfiles(uri: Uri) {
        viewModelScope.launch {
            val result = exportImportUseCase.exportProfiles(uri)
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
