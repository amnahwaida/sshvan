package com.sshvan.tunnelmanager.presentation.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sshvan.tunnelmanager.domain.model.ConnectionProfile
import com.sshvan.tunnelmanager.domain.model.TunnelState
import com.sshvan.tunnelmanager.domain.model.TunnelStatus
import com.sshvan.tunnelmanager.domain.usecase.DeleteProfileUseCase
import com.sshvan.tunnelmanager.domain.usecase.GetProfilesUseCase
import com.sshvan.tunnelmanager.domain.usecase.UpdateLastUsedUseCase
import com.sshvan.tunnelmanager.service.SshManager
import com.sshvan.tunnelmanager.service.TunnelForegroundService
import com.sshvan.tunnelmanager.util.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the HomeScreen.
 * Manages profile list, tunnel state, and connection actions.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val getProfilesUseCase: GetProfilesUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase,
    private val updateLastUsedUseCase: UpdateLastUsedUseCase,
    private val sshManager: SshManager
) : AndroidViewModel(application) {

    /**
     * All saved connection profiles, sorted by last used / created.
     */
    val profiles: StateFlow<List<ConnectionProfile>> = getProfilesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * Current tunnel connection state.
     */
    val tunnelState: StateFlow<TunnelState> = sshManager.tunnelState

    /**
     * Hotspot IP address (refreshed on demand).
     */
    private val _hotspotIp = MutableStateFlow<String?>(null)
    val hotspotIp: StateFlow<String?> = _hotspotIp.asStateFlow()

    /**
     * One-shot UI events (snackbar messages, etc.)
     */
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        refreshHotspotIp()
    }

    /**
     * Connect to a profile's SSH tunnel.
     */
    fun connect(profile: ConnectionProfile) {
        viewModelScope.launch {
            // Start foreground service first
            val serviceIntent = TunnelForegroundService.createConnectIntent(
                application, profile.id
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                application.startForegroundService(serviceIntent)
            } else {
                application.startService(serviceIntent)
            }

            // Connect SSH
            val result = sshManager.connect(profile)

            if (result.isSuccess) {
                updateLastUsedUseCase(profile.id)
                _uiEvent.emit(UiEvent.ShowSnackbar("Connected to ${profile.name}"))
            } else {
                // Stop the service if connection failed
                val stopIntent = TunnelForegroundService.createDisconnectIntent(application)
                application.startService(stopIntent)

                _uiEvent.emit(
                    UiEvent.ShowSnackbar(
                        result.exceptionOrNull()?.message ?: "Connection failed"
                    )
                )
            }
        }
    }

    /**
     * Disconnect the active tunnel.
     */
    fun disconnect() {
        viewModelScope.launch {
            sshManager.disconnect()

            // Stop foreground service
            val stopIntent = TunnelForegroundService.createDisconnectIntent(application)
            application.startService(stopIntent)

            _uiEvent.emit(UiEvent.ShowSnackbar("Disconnected"))
        }
    }

    /**
     * Delete a connection profile.
     */
    fun deleteProfile(profile: ConnectionProfile) {
        viewModelScope.launch {
            // If this profile is currently connected, disconnect first
            if (tunnelState.value.activeProfile?.id == profile.id && tunnelState.value.isActive) {
                disconnect()
            }
            deleteProfileUseCase(profile.id)
            _uiEvent.emit(UiEvent.ShowSnackbar("${profile.name} deleted"))
        }
    }

    /**
     * Copy local link to clipboard.
     */
    fun copyLocalLink() {
        val state = tunnelState.value
        val localPort = state.activeProfile?.localPort ?: return
        val link = NetworkUtils.generateLocalLink(localPort)
        NetworkUtils.copyToClipboard(application, "Local Link", link)
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowSnackbar("Copied: $link"))
        }
    }

    /**
     * Copy hotspot link to clipboard.
     */
    fun copyHotspotLink() {
        val state = tunnelState.value
        val localPort = state.activeProfile?.localPort ?: return
        val link = NetworkUtils.generateHotspotLink(localPort)

        viewModelScope.launch {
            if (link != null) {
                NetworkUtils.copyToClipboard(application, "Hotspot Link", link)
                _uiEvent.emit(UiEvent.ShowSnackbar("Copied: $link"))
            } else {
                _uiEvent.emit(UiEvent.ShowSnackbar("Hotspot not detected. Please enable hotspot first."))
            }
        }
    }

    /**
     * Show message when user tries to view a locked profile.
     */
    fun showLockedMessage() {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowSnackbar("This profile is locked. Details cannot be viewed."))
        }
    }

    /**
     * Refresh the hotspot IP address.
     */
    fun refreshHotspotIp() {
        _hotspotIp.value = NetworkUtils.getHotspotIpAddress()
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }
}
