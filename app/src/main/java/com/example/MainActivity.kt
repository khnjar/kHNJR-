package com.example

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.AppDatabase
import com.example.data.MedicalRepository
import com.example.ui.MedicalApp
import com.example.ui.MedicalViewModel
import com.example.ui.MedicalViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.example.work.DailyReminderWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Support immersive full-screen layout
        enableEdgeToEdge()

        // Safely ask for permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Schedule Daily Reminder Worker
        try {
            val workRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
                .build()
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "daily_medical_reminder",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Local Room persistence elements
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = MedicalRepository(database.medicalDao())

        setContent {
            val viewModel: MedicalViewModel = viewModel(
                factory = MedicalViewModelFactory(repository)
            )
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MedicalApp(viewModel = viewModel)
                }
            }
        }
    }
}
