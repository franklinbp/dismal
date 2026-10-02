package com.dismal.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dismal.app.navigation.AppNavigation
import com.dismal.app.sync.SyncScheduler
import com.dismal.app.ui.theme.DismalTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private companion object {
        const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DismalTheme {
                AppNavigation()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching {
            SyncScheduler.requestForegroundSync(this)
        }.onFailure { error ->
            Log.e(TAG, "Unable to request foreground sync", error)
        }
    }
}
