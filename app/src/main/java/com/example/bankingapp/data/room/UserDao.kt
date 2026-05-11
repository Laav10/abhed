package com.example.bankingapp.data.room

import androidx.room.*
import com.example.bankingapp.data.models.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    
    @Query("SELECT * FROM users WHERE deviceUUID = :deviceUUID LIMIT 1")
    suspend fun getUserByDeviceUUID(deviceUUID: String): User?
    
    @Query("SELECT * FROM users WHERE debitCardNumber = :debitCardNumber LIMIT 1")
    suspend fun getUserByDebitCard(debitCardNumber: String): User?
    
    @Query("SELECT * FROM users WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getUserByPhoneNumber(phoneNumber: String): User?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long
    
    @Update
    suspend fun updateUser(user: User)
    
    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: Long)
    
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>
}
