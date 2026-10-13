package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.Screen
import com.example.ui.navigation.bottomNavItems
import com.example.ui.screens.AslamAiScreen
import com.example.ui.screens.BusinessRecordsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AslamRecordTheme
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.ShopViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ShopViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Apply requested clean dark theme styling
            AslamRecordTheme(darkTheme = true) {
                val snackbarHostState = remember { SnackbarHostState() }
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
                var recordsInitialTab by remember { mutableIntStateOf(0) }
                val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()

                // Notification permission launcher for Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { _ -> }

                    LaunchedEffect(Unit) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                // Listen to ViewModel user messages for Snackbars
                LaunchedEffect(Unit) {
                    viewModel.userMessage.collect { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                }

                // Handle system back navigation
                if (currentScreen != Screen.Dashboard) {
                    BackHandler {
                        currentScreen = Screen.Dashboard
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = when (currentScreen) {
                                        Screen.Settings -> "Settings"
                                        Screen.Products -> "My Products"
                                        Screen.Records -> "Business Records"
                                        Screen.Reports -> "Reports"
                                        Screen.AslamAi -> "ASLAM AI Assistant"
                                        else -> shopProfile.shopName
                                    },
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                            },
                            navigationIcon = {
                                if (currentScreen != Screen.Dashboard) {
                                    IconButton(onClick = { currentScreen = Screen.Dashboard }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { currentScreen = Screen.AslamAi },
                                    modifier = Modifier.testTag("top_app_bar_ai_chat")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "ASLAM AI Chatbot",
                                        tint = if (currentScreen == Screen.AslamAi) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (currentScreen != Screen.Settings) {
                                    IconButton(onClick = { currentScreen = Screen.Settings }) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    floatingActionButton = {
                        if (currentScreen != Screen.AslamAi) {
                            ExtendedFloatingActionButton(
                                onClick = { currentScreen = Screen.AslamAi },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                },
                                text = {
                                    Text(
                                        text = "AI मुंशी",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                },
                                containerColor = EmeraldPrimary,
                                modifier = Modifier.testTag("global_fab_ai_chatbot")
                            )
                        }
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            bottomNavItems.forEach { screen ->
                                NavigationBarItem(
                                    selected = currentScreen == screen,
                                    onClick = { currentScreen = screen },
                                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                                    label = { Text(screen.title, fontSize = 10.sp) },
                                    modifier = Modifier.testTag("nav_${screen.route}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        when (currentScreen) {
                            Screen.Dashboard -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToProducts = { currentScreen = Screen.Products },
                                onNavigateToRecords = { initialTab ->
                                    recordsInitialTab = initialTab
                                    currentScreen = Screen.Records
                                },
                                onNavigateToReports = { currentScreen = Screen.Reports },
                                onNavigateToAi = { currentScreen = Screen.AslamAi }
                            )
                            Screen.Products -> ProductsScreen(viewModel = viewModel)
                            Screen.Records -> BusinessRecordsScreen(
                                viewModel = viewModel,
                                initialTab = recordsInitialTab
                            )
                            Screen.Reports -> ReportsScreen(viewModel = viewModel)
                            Screen.Settings -> SettingsScreen(
                                viewModel = viewModel,
                                onOpenAiChat = { currentScreen = Screen.AslamAi }
                            )
                            Screen.AslamAi -> AslamAiScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
