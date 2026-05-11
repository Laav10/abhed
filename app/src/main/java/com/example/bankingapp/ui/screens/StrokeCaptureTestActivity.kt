package com.example.bankingapp.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.bankingapp.ui.theme.BankingAppTheme

class StrokeCaptureTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BankingAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StrokeCaptureOnboardingScreen(
                        userId = "test_user_${System.currentTimeMillis()}",
                        onComplete = {
                            // Show completion message
                            finish()
                        }
                    )
                }
            }
        }
    }
}
