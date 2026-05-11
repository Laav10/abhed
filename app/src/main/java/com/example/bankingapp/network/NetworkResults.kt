package com.example.bankingapp.network

// Result classes for API responses
sealed class BackendSyncResult {
    data class Success(
        val deviceId: String,
        val userId: String,
        val touchDataSynced: Boolean
    ) : BackendSyncResult()
    
    data class Failure(
        val errorMessage: String
    ) : BackendSyncResult()
}

sealed class DeviceStatusResult {
    data class Success(
        val status: String,
        val userId: String?,
        val isActive: Boolean
    ) : DeviceStatusResult()
    
    data class Failure(
        val errorMessage: String
    ) : DeviceStatusResult()
}
