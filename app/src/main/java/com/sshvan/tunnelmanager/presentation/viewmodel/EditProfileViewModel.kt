package com.sshvan.tunnelmanager.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sshvan.tunnelmanager.domain.model.AuthType
import com.sshvan.tunnelmanager.domain.model.ConnectionProfile
import com.sshvan.tunnelmanager.domain.usecase.GetProfileByIdUseCase
import com.sshvan.tunnelmanager.domain.usecase.SaveProfileUseCase
import com.sshvan.tunnelmanager.domain.repository.ProfileRepository
import com.sshvan.tunnelmanager.service.SshManager
import com.sshvan.tunnelmanager.util.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for EditProfileScreen.
 * Handles form state, validation, save, and test connection.
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getProfileByIdUseCase: GetProfileByIdUseCase,
    private val saveProfileUseCase: SaveProfileUseCase,
    private val sshManager: SshManager,
    private val repository: ProfileRepository
) : ViewModel() {

    private val profileId: Long = savedStateHandle.get<Long>("profileId") ?: 0L
    val isEditMode: Boolean = profileId > 0L

    private val _formState = MutableStateFlow(ProfileFormState())
    val formState: StateFlow<ProfileFormState> = _formState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<EditProfileEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        if (isEditMode) {
            loadProfile()
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val profile = getProfileByIdUseCase(profileId)
            if (profile != null) {
                _formState.value = ProfileFormState(
                    name = profile.name,
                    sshHost = profile.sshHost,
                    sshPort = profile.sshPort.toString(),
                    username = profile.username,
                    authType = profile.authType,
                    password = profile.password ?: "",
                    privateKeyPath = profile.privateKeyPath,
                    localPort = profile.localPort.toString(),
                    remoteHost = profile.remoteHost,
                    remotePort = profile.remotePort.toString(),
                    isLocked = profile.isLocked
                )
            }
        }
    }

    // Form field update methods
    fun updateName(value: String) {
        _formState.update { it.copy(name = value, errors = it.errors - "name") }
    }

    fun updateSshHost(value: String) {
        _formState.update { it.copy(sshHost = value, errors = it.errors - "sshHost") }
    }

    fun updateSshPort(value: String) {
        _formState.update { it.copy(sshPort = value, errors = it.errors - "sshPort") }
    }

    fun updateUsername(value: String) {
        _formState.update { it.copy(username = value, errors = it.errors - "username") }
    }

    fun updateAuthType(value: AuthType) {
        _formState.update {
            it.copy(
                authType = value,
                errors = it.errors - "password" - "privateKeyPath"
            )
        }
    }

    fun updatePassword(value: String) {
        _formState.update { it.copy(password = value, errors = it.errors - "password") }
    }

    fun updatePrivateKeyPath(value: String?) {
        _formState.update { it.copy(privateKeyPath = value, errors = it.errors - "privateKeyPath") }
    }

    fun updateLocalPort(value: String) {
        _formState.update { it.copy(localPort = value, errors = it.errors - "localPort") }
    }

    fun updateRemoteHost(value: String) {
        _formState.update { it.copy(remoteHost = value, errors = it.errors - "remoteHost") }
    }

    fun updateRemotePort(value: String) {
        _formState.update { it.copy(remotePort = value, errors = it.errors - "remotePort") }
    }

    /**
     * Save the profile to the database.
     */
    fun saveProfile() {
        val currentState = _formState.value
        val formData = ValidationUtils.ProfileFormData(
            name = currentState.name,
            sshHost = currentState.sshHost,
            sshPort = currentState.sshPort,
            username = currentState.username,
            authType = currentState.authType.name,
            password = currentState.password,
            privateKeyPath = currentState.privateKeyPath,
            localPort = currentState.localPort,
            remoteHost = currentState.remoteHost,
            remotePort = currentState.remotePort
        )

        val errors = ValidationUtils.validateProfileForm(formData)
        if (errors.isNotEmpty()) {
            _formState.update { it.copy(errors = errors) }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true) }

            try {
                val profile = ConnectionProfile(
                    id = profileId,
                    name = currentState.name.trim(),
                    sshHost = currentState.sshHost.trim(),
                    sshPort = currentState.sshPort.toIntOrNull() ?: 22,
                    username = currentState.username.trim(),
                    authType = currentState.authType,
                    password = if (currentState.authType == AuthType.PASSWORD)
                        currentState.password else null,
                    privateKeyPath = if (currentState.authType == AuthType.PRIVATE_KEY)
                        currentState.privateKeyPath else null,
                    localPort = currentState.localPort.toIntOrNull() ?: 8080,
                    remoteHost = currentState.remoteHost.trim().ifBlank { "localhost" },
                    remotePort = currentState.remotePort.toIntOrNull() ?: 3000,
                    isLocked = currentState.isLocked
                )

                saveProfileUseCase(profile)
                _uiEvent.emit(EditProfileEvent.SaveSuccess)
            } catch (e: Exception) {
                _uiEvent.emit(EditProfileEvent.ShowError("Failed to save: ${e.message}"))
            } finally {
                _formState.update { it.copy(isSaving = false) }
            }
        }
    }

    /**
     * Test the connection with current form values.
     */
    fun testConnection() {
        val currentState = _formState.value
        val formData = ValidationUtils.ProfileFormData(
            name = currentState.name.ifBlank { "Test" },
            sshHost = currentState.sshHost,
            sshPort = currentState.sshPort,
            username = currentState.username,
            authType = currentState.authType.name,
            password = currentState.password,
            privateKeyPath = currentState.privateKeyPath,
            localPort = currentState.localPort,
            remoteHost = currentState.remoteHost,
            remotePort = currentState.remotePort
        )

        val errors = ValidationUtils.validateProfileForm(formData)
        if (errors.isNotEmpty()) {
            _formState.update { it.copy(errors = errors) }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isTesting = true, testResult = null) }

            val profile = ConnectionProfile(
                name = currentState.name.ifBlank { "Test" },
                sshHost = currentState.sshHost.trim(),
                sshPort = currentState.sshPort.toIntOrNull() ?: 22,
                username = currentState.username.trim(),
                authType = currentState.authType,
                password = if (currentState.authType == AuthType.PASSWORD)
                    currentState.password else null,
                privateKeyPath = if (currentState.authType == AuthType.PRIVATE_KEY)
                    currentState.privateKeyPath else null,
                localPort = currentState.localPort.toIntOrNull() ?: 8080,
                remoteHost = currentState.remoteHost.trim().ifBlank { "localhost" },
                remotePort = currentState.remotePort.toIntOrNull() ?: 3000,
                    isLocked = currentState.isLocked
            )

            val result = sshManager.testConnection(profile)
            _formState.update {
                it.copy(
                    isTesting = false,
                    testResult = result.fold(
                        onSuccess = { msg -> TestResult.Success(msg) },
                        onFailure = { err -> TestResult.Failure(err.message ?: "Test failed") }
                    )
                )
            }
        }
    }

}

/**
 * Represents the form state for creating/editing a profile.
 */
data class ProfileFormState(
    val name: String = "",
    val sshHost: String = "",
    val sshPort: String = "22",
    val username: String = "",
    val authType: AuthType = AuthType.PASSWORD,
    val password: String = "",
    val privateKeyPath: String? = null,
    val localPort: String = "8080",
    val remoteHost: String = "localhost",
    val remotePort: String = "3000",
    val isLocked: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val isTesting: Boolean = false,
    val testResult: TestResult? = null
)

sealed class TestResult {
    data class Success(val message: String) : TestResult()
    data class Failure(val message: String) : TestResult()
}

sealed class EditProfileEvent {
    data object SaveSuccess : EditProfileEvent()
    data class ShowError(val message: String) : EditProfileEvent()
}
