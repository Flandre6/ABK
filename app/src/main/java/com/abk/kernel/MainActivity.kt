package com.abk.kernel

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abk.kernel.ui.screens.AuthGateScreen
import com.abk.kernel.ui.screens.BuildScreen
import com.abk.kernel.ui.screens.FlashScreen
import com.abk.kernel.ui.screens.SettingsScreen
import com.abk.kernel.ui.screens.StatusScreen
import com.abk.kernel.ui.theme.AbkTheme
import com.abk.kernel.viewmodel.AuthStep
import com.abk.kernel.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val vm: MainViewModel = viewModel()
            val state by vm.uiState.collectAsState()

            AbkTheme(themeMode = state.themeMode) {
                if (state.authStep != AuthStep.READY) {
                    AuthGateScreen(vm)
                } else {
                    AbkMainScaffold(vm)
                }
            }
        }
    }
}

private enum class AbkTab(val label: String) {
    Status("当前状态"),
    Build("构建内核"),
    Flash("刷写"),
    Settings("设置")
}

@Composable
private fun AbkMainScaffold(vm: MainViewModel) {
    var selectedTab by rememberSaveable { mutableStateOf(AbkTab.Status) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AbkTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    AbkTab.Status -> Icons.Default.Info
                                    AbkTab.Build -> Icons.Default.Memory
                                    AbkTab.Flash -> Icons.Default.FlashOn
                                    AbkTab.Settings -> Icons.Default.Settings
                                },
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "abk-tab"
            ) { tab ->
                when (tab) {
                    AbkTab.Status -> StatusScreen(vm)
                    AbkTab.Build -> BuildScreen(vm)
                    AbkTab.Flash -> FlashScreen(vm)
                    AbkTab.Settings -> SettingsScreen(vm)
                }
            }
        }
    }
}
