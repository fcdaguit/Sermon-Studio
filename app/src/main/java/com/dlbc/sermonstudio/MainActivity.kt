package com.dlbc.sermonstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.dlbc.sermonstudio.data.prefs.SettingsStore
import com.dlbc.sermonstudio.data.prefs.ThemeMode
import com.dlbc.sermonstudio.ui.nav.AppNavHost
import com.dlbc.sermonstudio.ui.theme.SermonStudioTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settings: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val dynamic by settings.dynamicColor.collectAsState(initial = true)

            SermonStudioTheme(themeMode = themeMode, dynamicColor = dynamic) {
                AppNavHost(navController = rememberNavController())
            }
        }
    }
}
