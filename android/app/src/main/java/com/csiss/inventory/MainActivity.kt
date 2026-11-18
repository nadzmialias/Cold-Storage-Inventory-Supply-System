package com.csiss.inventory

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.csiss.inventory.data.AlertRepository
import com.csiss.inventory.data.SystemAlertRow
import com.csiss.inventory.ui.auth.CsissAuthGate
import com.csiss.inventory.ui.notification.AlertNotificationHelper
import com.csiss.inventory.ui.notification.NotificationScreen
import com.csiss.inventory.ui.profile.ProfileScreen
import com.csiss.inventory.ui.stock.StockScreen
import com.csiss.inventory.ui.temperature.TemperatureScreen
import com.csiss.inventory.ui.theme.CSISSTheme
import com.csiss.inventory.ui.components.FluentuiSystemIconsBox
import com.csiss.inventory.ui.components.FluentuiSystemIconsTemperature
import com.csiss.inventory.ui.components.FluentuiSystemIconsAlert
import com.csiss.inventory.ui.components.FluentuiSystemIconsPerson

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(
            window,
            window.decorView
        ).isAppearanceLightStatusBars = true

        setContent {
            MaterialTheme {
                CsissAuthGate { currentUser, onLogout ->
                    val context = LocalContext.current
                    val alertRepository = remember { AlertRepository() }

                    // 0 = Stock, 1 = Temperature, 2 = Alerts, 3 = Profile
                    var selectedTab by remember { mutableIntStateOf(0) }
                    var alertsFilter by remember { mutableIntStateOf(0) }
                    var openAlerts by remember { mutableStateOf<List<SystemAlertRow>>(emptyList()) }
                    var openAlertCount by remember { mutableIntStateOf(0) }
                    var showStartupAlertPopup by remember { mutableStateOf(false) }
                    var hasShownPopupForSession by remember(currentUser.fullName) { mutableStateOf(false) }

                    // Request Android 13+ notification permission for system pop-up banners
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { _ -> }

                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    // Background check when user logs in or switches tabs
                    LaunchedEffect(currentUser.fullName, selectedTab) {
                        runCatching {
                            val allAlerts = alertRepository.loadAndSyncAlerts()
                            val activeList = allAlerts.filter { !it.isResolved }
                            openAlerts = activeList
                            openAlertCount = activeList.size

                            if (activeList.isNotEmpty()) {
                                AlertNotificationHelper.showSystemPopupIfNeeded(context, activeList)
                                if (!hasShownPopupForSession) {
                                    showStartupAlertPopup = true
                                    hasShownPopupForSession = true
                                }
                            }
                        }
                    }

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding(),

                        topBar = {
                            Surface(
                                color = Color.White
                            ) {
                                Text(
                                    text = "CSISS",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 20.dp,
                                            vertical = 14.dp
                                        ),
                                    color = Color(0xFF132945),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = Color.White
                            ) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = {
                                        Icon(
                                            imageVector = FluentuiSystemIconsBox,
                                            contentDescription = null
                                        )
                                    },
                                    label = { Text("Stock") }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = {
                                        Icon(
                                            imageVector = FluentuiSystemIconsTemperature,
                                            contentDescription = null
                                        )
                                    },
                                    label = { Text("Temperature") }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = {
                                        alertsFilter = 0
                                        selectedTab = 2
                                    },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (openAlertCount > 0) {
                                                    Badge(
                                                        containerColor = Color(0xFFE45465),
                                                        contentColor = Color.White
                                                    ) {
                                                        Text("$openAlertCount")
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = FluentuiSystemIconsAlert,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    label = { Text("Alerts") }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 3,
                                    onClick = { selectedTab = 3 },
                                    icon = {
                                        Icon(
                                            imageVector = FluentuiSystemIconsPerson,
                                            contentDescription = null
                                        )
                                    },
                                    label = { Text("Profile") }
                                )
                            }

                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (selectedTab) {
                                0 -> StockScreen()
                                1 -> TemperatureScreen(
                                    useDemoData = false,
                                    canEditLimits = currentUser.role == "ADMIN",
                                    onOpenAlerts = {
                                        alertsFilter = 2
                                        selectedTab = 2
                                    }
                                )
                                2 -> NotificationScreen(
                                    initialFilter = alertsFilter,
                                    onOpenCountChanged = { newCount ->
                                        openAlertCount = newCount
                                    }
                                )
                                3 -> ProfileScreen(
                                    user = currentUser,
                                    onLogout = onLogout
                                )
                            }
                        }
                    }

                    // Urgent Pop-Up Alert Dialog for Staff & Admin
                    if (showStartupAlertPopup && openAlerts.isNotEmpty()) {
                        AlertDialog(
                            onDismissRequest = { showStartupAlertPopup = false },
                            title = {
                                Text(
                                    text = "⚠️ Attention Required (${openAlerts.size} Active Alerts)",
                                    color = Color(0xFFE45465),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "The following cold storage items require immediate staff attention:",
                                        fontSize = 13.sp,
                                        color = Color(0xFF182B48)
                                    )
                                    openAlerts.take(3).forEach { alert ->
                                        Surface(
                                            color = Color(0xFFFFF1F2),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = alert.title,
                                                    color = Color(0xFFE45465),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = alert.message,
                                                    color = Color(0xFF182B48),
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                    if (openAlerts.size > 3) {
                                        Text(
                                            text = "+ ${openAlerts.size - 3} more active alerts...",
                                            fontSize = 12.sp,
                                            color = Color(0xFF718198)
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showStartupAlertPopup = false
                                        alertsFilter = 0
                                        selectedTab = 2
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF132945),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("View Alerts Now")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showStartupAlertPopup = false }) {
                                    Text("Dismiss")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CSISSTheme {
        Greeting("Android")
    }
}