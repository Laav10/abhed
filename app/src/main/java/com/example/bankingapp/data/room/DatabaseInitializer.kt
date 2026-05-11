package com.example.bankingapp.data.room

import android.util.Log
import com.example.bankingapp.data.models.DebitCard
import com.example.bankingapp.data.room.BankingDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DatabaseInitializer(private val database: BankingDatabase) {
    
    /**
     * Initialize database with sample data
     */
    fun initializeDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            // Force clear and repopulate
            clearAllData()
            populateSampleDebitCards()
        }
    }
    
    /**
     * Populate sample debit cards for testing
     */
    private suspend fun populateSampleDebitCards() {
        val debitCardDao = database.debitCardDao()
        
        // Check if data already exists
        val existingCards = debitCardDao.getAllDebitCards().first()
        if (existingCards.isNotEmpty()) {
            return // Data already exists
        }
        
        // Add non-PII sample cards for local testing
        val sampleCards = listOf(
            DebitCard(
                cardNumber = "123456789",
                cardHolderName = "Demo User One",
                phoneNumber = "+10000000001",
                bankName = "ABHED Bank"
            ),
            DebitCard(
                cardNumber = "987654321",
                cardHolderName = "Demo User Two",
                phoneNumber = "+10000000002",
                bankName = "ABHED Bank"
            )
        )
        
        sampleCards.forEach { card ->
            debitCardDao.insertDebitCard(card)
            Log.d("DatabaseInitializer", "Inserted card: ${card.cardNumber} -> ${card.phoneNumber}")
        }
        
        // Verify the data
        val finalCards = debitCardDao.getAllDebitCards().first()
        Log.d("DatabaseInitializer", "Final database state: ${finalCards.map { it.cardNumber }}")
    }
    
    /**
     * Clear all data (for testing purposes)
     */
    suspend fun clearAllData() {
        database.userDao().getAllUsers().first().forEach { user ->
            database.userDao().deleteUser(user.id)
        }
        
        database.debitCardDao().getAllDebitCards().first().forEach { card ->
            database.debitCardDao().deleteDebitCard(card.cardNumber)
        }
        
        database.otpDao().getAllOTPs().first().forEach { otp ->
            database.otpDao().deleteOTPsByPhoneNumber(otp.phoneNumber)
        }
    }
}
