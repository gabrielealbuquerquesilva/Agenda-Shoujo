package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.local.AppDatabase
import com.example.data.network.OAuthTokenManager
import com.example.data.repository.EventRepository
import com.example.ui.screens.MainScreen
import com.example.ui.screens.MainViewModel
import com.example.ui.screens.MainViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = EventRepository(database.eventDao())
        val tokenManager = OAuthTokenManager(applicationContext)

        val factory = MainViewModelFactory(application, repository, tokenManager)
        val viewModel: MainViewModel by viewModels { factory }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
