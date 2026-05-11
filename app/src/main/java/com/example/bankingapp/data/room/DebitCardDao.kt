package com.example.bankingapp.data.room

import androidx.room.*
import com.example.bankingapp.data.models.DebitCard
import kotlinx.coroutines.flow.Flow

@Dao
interface DebitCardDao {
    
    @Query("SELECT * FROM debit_cards WHERE cardNumber = :cardNumber LIMIT 1")
    suspend fun getDebitCardByNumber(cardNumber: String): DebitCard?
    
    @Query("SELECT * FROM debit_cards WHERE phoneNumber = :phoneNumber")
    suspend fun getDebitCardsByPhoneNumber(phoneNumber: String): List<DebitCard>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebitCard(debitCard: DebitCard)
    
    @Update
    suspend fun updateDebitCard(debitCard: DebitCard)
    
    @Query("DELETE FROM debit_cards WHERE cardNumber = :cardNumber")
    suspend fun deleteDebitCard(cardNumber: String)
    
    @Query("SELECT * FROM debit_cards")
    fun getAllDebitCards(): Flow<List<DebitCard>>
}
