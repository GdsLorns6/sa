package com.smd.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.smd.android.ui.navigation.AppNavHost
import com.smd.android.ui.theme.SteamManifestDownloaderTheme

class MainActivity : ComponentActivity() {

    private var pendingFileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle file opened via intent
        pendingFileUri = intent?.data

        setContent {
            val navController = rememberNavController()
            SteamManifestDownloaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(
                        navController = navController,
                        pendingFileUri = pendingFileUri
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle file opened while app is already running
        intent.data?.let { uri ->
            pendingFileUri = uri
        }
    }
}
