package com.example.bankingapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bankingapp.data.models.DebitCard
import com.example.bankingapp.data.models.User
import com.example.bankingapp.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegistrationViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // UI States
    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()
    
    // Device UUID
    private val _deviceUUID = MutableStateFlow("")
    val deviceUUID: StateFlow<String> = _deviceUUID.asStateFlow()
    
    init {
        // Get device UUID on initialization
        _deviceUUID.value = authRepository.getDeviceUUID()
        
        // Check if device is already registered
        if (authRepository.isDeviceRegistered()) {
            _uiState.value = _uiState.value.copy(
                isDeviceRegistered = true,
                message = "Device already registered"
            )
        }
    }
    
    /**
     * Verify debit card and generate OTP using phone number from database
     */
    fun verifyDebitCardAndGenerateOTP(name: String, debitCardNumber: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val normalizedCard = debitCardNumber.filter { it.isDigit() } // digits-only
                android.util.Log.d("RegistrationVM", "verifyDebitCard called raw='$debitCardNumber' normalized='$normalizedCard'")

                // Verify debit card exists and get associated phone number
                val debitCard = authRepository.verifyDebitCard(normalizedCard)
                android.util.Log.d("RegistrationVM", "verifyDebitCard result: $debitCard")
                
                // Log the card details if found
                if (debitCard != null) {
                    android.util.Log.d("RegistrationVM", "Found card: ${debitCard.cardHolderName} - ${debitCard.cardNumber} - ${debitCard.phoneNumber}")
                }

                if (debitCard == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Invalid debit card number"
                    )
                    return@launch
                }

                // Generate OTP for the phone number from database
                val otpCode = authRepository.generateOTP(debitCard.phoneNumber)
                android.util.Log.d("RegistrationVM", "Generated OTP: $otpCode (for debug only!)")

                // Update UI state with card info and move to OTP step
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentStep = RegistrationStep.OTP_VERIFICATION,
                    debitCardInfo = debitCard,
                    message = "OTP sent to your number ${debitCard.phoneNumber.takeLast(3)}",
                    name = name,
                    phoneNumber = debitCard.phoneNumber,
                    debitCardNumber = normalizedCard,
                    otpExpiryTime = System.currentTimeMillis() + (2 * 60 * 1000) // 2 minutes
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to verify card: ${e.message}"
                )
            }
        }
    }

    
    /**
     * Verify OTP and complete registration
     */
    fun verifyOTPAndCompleteRegistration(otpCode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val currentState = _uiState.value
                val debitCard = currentState.debitCardInfo
                
                if (debitCard == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Debit card information not found"
                    )
                    return@launch
                }
                
                // Verify OTP and complete registration using the manually entered phone number
                val result = authRepository.verifyOTPAndRegister(
                    phoneNumber = currentState.phoneNumber,
                    otpCode = otpCode,
                    name = currentState.name,
                    debitCardNumber = debitCard.cardNumber
                )
                
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentStep = RegistrationStep.REGISTRATION_COMPLETE,
                        registeredUser = user,
                        message = "Registration successful! Welcome ${user?.name}"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "OTP verification failed"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Registration failed: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update user name
     */
    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }
    
    /**
     * Update debit card number
     */
    fun updateDebitCardNumber(debitCardNumber: String) {
        _uiState.value = _uiState.value.copy(debitCardNumber = debitCardNumber)
    }
    
    /**
     * Update phone number
     */
    fun updatePhoneNumber(phoneNumber: String) {
        _uiState.value = _uiState.value.copy(phoneNumber = phoneNumber)
    }
    
    /**
     * Reset to initial state
     */
    fun resetToInitialState() {
        _uiState.value = RegistrationUiState()
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    

    
    /**
     * Validate phone number format
     */
    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        // Basic phone number validation - should start with + and be at least 10 digits
        val phoneRegex = "^\\+[1-9]\\d{1,14}$".toRegex()
        return phoneNumber.matches(phoneRegex)
    }
}

// UI State
data class RegistrationUiState(
    val name: String = "",
    val debitCardNumber: String = "",
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val currentStep: RegistrationStep = RegistrationStep.INITIAL,
    val debitCardInfo: DebitCard? = null,
    val registeredUser: User? = null,
    val isDeviceRegistered: Boolean = false,
    val otpExpiryTime: Long? = null
) {
    val isOtpSent: Boolean
        get() = currentStep == RegistrationStep.OTP_VERIFICATION
    
    val isOtpVerified: Boolean
        get() = currentStep == RegistrationStep.REGISTRATION_COMPLETE
}

// Registration Steps
enum class RegistrationStep {
    INITIAL,
    OTP_VERIFICATION,
    REGISTRATION_COMPLETE
}
