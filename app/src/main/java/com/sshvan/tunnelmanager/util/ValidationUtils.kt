package com.sshvan.tunnelmanager.util

/**
 * Input validation utilities for SSH tunnel configuration fields.
 */
object ValidationUtils {

    fun validateProfileName(name: String): String? =
        if (name.isBlank()) "Profile name is required" else null

    fun validateHost(host: String): String? = when {
        host.isBlank() -> "Host is required"
        host.contains(" ") -> "Host cannot contain spaces"
        else -> null
    }

    fun validatePort(portStr: String, fieldName: String): String? {
        if (portStr.isBlank()) return "$fieldName is required"
        val port = portStr.toIntOrNull()
        return when {
            port == null -> "$fieldName must be a number"
            port !in 1..65535 -> "$fieldName must be between 1 and 65535"
            else -> null
        }
    }

    fun validateUsername(username: String): String? =
        if (username.isBlank()) "Username is required" else null

    fun validatePassword(password: String): String? =
        if (password.isBlank()) "Password is required" else null

    fun validatePrivateKeyPath(path: String?): String? =
        if (path.isNullOrBlank()) "Private key file is required" else null

    /**
     * Validate all fields of a profile form and return a map of field name to error message.
     * Empty map means no errors.
     */
    data class ProfileFormData(
        val name: String,
        val sshHost: String,
        val sshPort: String,
        val username: String,
        val authType: String,
        val password: String,
        val privateKeyPath: String?,
        val localPort: String,
        val remoteHost: String,
        val remotePort: String
    )

    fun validateProfileForm(form: ProfileFormData): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        validateProfileName(form.name)?.let { errors["name"] = it }
        validateHost(form.sshHost)?.let { errors["sshHost"] = it }
        validatePort(form.sshPort, "SSH Port")?.let { errors["sshPort"] = it }
        validateUsername(form.username)?.let { errors["username"] = it }
        validateHost(form.remoteHost)?.let { errors["remoteHost"] = it }
        validatePort(form.localPort, "Local Port")?.let { errors["localPort"] = it }
        validatePort(form.remotePort, "Remote Port")?.let { errors["remotePort"] = it }

        if (form.authType == "PASSWORD") {
            validatePassword(form.password)?.let { errors["password"] = it }
        } else {
            validatePrivateKeyPath(form.privateKeyPath)?.let { errors["privateKeyPath"] = it }
        }

        return errors
    }
}
