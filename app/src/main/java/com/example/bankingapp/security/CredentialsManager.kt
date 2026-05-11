package com.example.bankingapp.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class CredentialsManager(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "twilio_credentials_prefs"
        private const val KEY_ACCOUNT_SID = "account_sid"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_FROM_PHONE = "from_phone"
        private const val KEY_CREDENTIALS_SAVED = "credentials_saved"
    }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    /**
     * Save Twilio credentials securely
     */
    fun saveCredentials(accountSid: String, authToken: String, fromPhone: String) {
        encryptedPrefs.edit()
            .putString(KEY_ACCOUNT_SID, accountSid)
            .putString(KEY_AUTH_TOKEN, authToken)
            .putString(KEY_FROM_PHONE, fromPhone)
            .putBoolean(KEY_CREDENTIALS_SAVED, true)
            .apply()
    }
    
    /**
     * Get Account SID
     */
    fun getAccountSid(): String? {
        return encryptedPrefs.getString(KEY_ACCOUNT_SID, null)
    }
    
    /**
     * Get Auth Token
     */
    fun getAuthToken(): String? {
        return encryptedPrefs.getString(KEY_AUTH_TOKEN, null)
    }
    
    /**
     * Get From Phone Number
     */
    fun getFromPhone(): String? {
        return encryptedPrefs.getString(KEY_FROM_PHONE, null)
    }
    
    /**
     * Check if credentials are saved
     */
    fun areCredentialsSaved(): Boolean {
        return encryptedPrefs.getBoolean(KEY_CREDENTIALS_SAVED, false)
    }
    
    /**
     * Clear all credentials
     */
    fun clearCredentials() {
        encryptedPrefs.edit().clear().apply()
    }
    
    /**
     * Check if credentials are valid
     */
    fun areCredentialsValid(): Boolean {
        val accountSid = getAccountSid()
        val authToken = getAuthToken()
        val fromPhone = getFromPhone()
        
        return !accountSid.isNullOrBlank() && 
               !authToken.isNullOrBlank() && 
               !fromPhone.isNullOrBlank()
    }
}
