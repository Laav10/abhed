package com.example.bankingapp.data.room

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.bankingapp.data.models.User
import com.example.bankingapp.data.models.Device
import com.example.bankingapp.data.models.DebitCard
import com.example.bankingapp.data.models.OTP
import com.example.bankingapp.data.models.BiometricData
import com.example.bankingapp.data.models.DeviceRegistration
import com.example.bankingapp.data.room.TouchDataEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [User::class, Device::class, DebitCard::class, OTP::class, TouchDataEntity::class, BiometricData::class, DeviceRegistration::class],
    version = 1,
    exportSchema = false
)
abstract class BankingDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun deviceDao(): DeviceDao
    abstract fun debitCardDao(): DebitCardDao
    abstract fun otpDao(): OTPDao
    abstract fun touchDataDao(): TouchDataDao
    abstract fun biometricDataDao(): BiometricDataDao
    abstract fun deviceRegistrationDao(): DeviceRegistrationDao
    
    companion object {
        @Volatile
        private var INSTANCE: BankingDatabase? = null
        
        // Database migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add any new columns or tables here when needed
                // For now, no changes needed
            }
        }
        
        fun getDatabase(context: Context): BankingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BankingDatabase::class.java,
                    "banking_database"
                )
                .addMigrations(MIGRATION_1_2) // Add migration support
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
