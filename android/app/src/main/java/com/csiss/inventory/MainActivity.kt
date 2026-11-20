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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.view.WindowCompat
import com.csiss.inventory.data.AlertRepository
import com.csiss.inventory.data.SystemAlertRow
import com.csiss.inventory.ui.auth.CsissAuthGate
import com.csiss.inventory.ui.notification.AlertNotificationHelper
import com.csiss.inventory.ui.notification.NotificationScreen
import com.csiss.inventory.ui.profile.ProfileScreen
import com.csiss.inventory.ui.stock.StockScreen
import com.csiss.inventory.ui.inventory.InventoryScreen
import com.csiss.inventory.ui.supplier.SupplierScreen
import com.csiss.inventory.ui.temperature.TemperatureScreen
import com.csiss.inventory.ui.theme.CSISSTheme
import com.csiss.inventory.ui.components.FluentuiSystemIconsBox
import com.csiss.inventory.ui.components.FluentuiSystemIconsTemperature
import com.csiss.inventory.ui.components.FluentuiSystemIconsAlert
import com.csiss.inventory.ui.components.FluentuiSystemIconsPerson
import com.csiss.inventory.ui.components.FluentuiSystemIconsSupplier

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

                    // 0 = Stock, 1 = Temperature, 2 = Alerts, 3 = Profile,  3 = Profile, 4 = Inventory, 5 = Supplier
                    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
                    var alertsFilter by remember { mutableIntStateOf(0) }
                    var openAlerts by remember { mutableStateOf<List<SystemAlertRow>>(emptyList()) }
                    var openAlertCount by remember { mutableIntStateOf(0) }
                    var showStartupAlertPopup by remember { mutableStateOf(false) }
                    var hasShownPopupForSession by remember(currentUser.fullName) {
                        mutableStateOf(
                            false
                        )
                    }
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    val drawerScope = rememberCoroutineScope()
                    var showDrawerLogoutDialog by remember { mutableStateOf(false) }
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

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            CompactManagementDrawer(
                                userName = currentUser.fullName,
                                role = currentUser.role,
                                supplierSelected = selectedTab == 5,
                                profileSelected = selectedTab == 3,
                                onSupplierClick = {
                                    drawerScope.launch {
                                        drawerState.close()
                                        selectedTab = 5
                                    }
                                },
                                onProfileClick = {
                                    drawerScope.launch {
                                        drawerState.close()
                                        selectedTab = 3
                                    }
                                },
                                onLogoutClick = {
                                    drawerScope.launch {
                                        drawerState.close()
                                        showDrawerLogoutDialog = true
                                    }
                                }
                            )
                        }
                    ) {
                        Scaffold(

                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding(),

                            topBar = {
                                Surface(color = Color.White) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 20.dp, end = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "CSISS",
                                            modifier = Modifier.padding(vertical = 14.dp),
                                            color = Color(0xFF132945),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(
                                            onClick = {
                                                drawerScope.launch {
                                                    drawerState.open()
                                                }
                                            },
                                            modifier = Modifier.semantics {
                                                contentDescription = "Open navigation menu"
                                            }
                                        ) {
                                            Column(
                                                modifier = Modifier.width(22.dp),
                                                verticalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                repeat(3) {
                                                    HorizontalDivider(
                                                        thickness = 2.dp,
                                                        color = Color(0xFF132945)
                                                    )
                                                }
                                            }
                                        }
                                    }
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
                                        label = { BottomNavLabel("Stock") }
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 4,
                                        onClick = { selectedTab = 4 },
                                        icon = {
                                            Icon(
                                                imageVector = FluentuiSystemIconsBox,
                                                contentDescription = null
                                            )
                                        },
                                        label = { BottomNavLabel("Inventory") }
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
                                        label = { BottomNavLabel("Temp") }
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
                                        label = { BottomNavLabel("Alerts") }
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
                                        label = { BottomNavLabel("Profile") }
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

                                    4 -> InventoryScreen(
                                        onHome = { selectedTab = 0 },
                                        onAlerts = {
                                            alertsFilter = 0
                                            selectedTab = 2
                                        },
                                        onProfile = { selectedTab = 3 }
                                    )

                                    5 -> SupplierScreen(
                                        onHome = { selectedTab = 0 },
                                        onAlerts = {
                                            alertsFilter = 0
                                            selectedTab = 2
                                        },
                                        onProfile = { selectedTab = 3 }
                                    )
                                }
                            }
                        } // End Scaffold
                    } // End ModalNavigationDrawer


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


                    if (showDrawerLogoutDialog) {
                        AlertDialog(
                            onDismissRequest = {
                                showDrawerLogoutDialog = false
                            },
                            title = {
                                Text("Log out?")
                            },
                            text = {
                                Text("Do you want to end your CSISS session?")
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showDrawerLogoutDialog = false
                                        onLogout()
                                    }
                                ) {
                                    Text(
                                        text = "Log out",
                                        color = Color(0xFFE45465)
                                    )
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showDrawerLogoutDialog = false
                                    }
                                ) {
                                    Text("Cancel")
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
private fun BottomNavLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}


@Composable
private fun CompactManagementDrawer(
    userName: String,
    role: String,
    supplierSelected: Boolean,
    profileSelected: Boolean,
    onSupplierClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val navy = Color(0xFF132945)
    val muted = Color(0xFF718198)

    ModalDrawerSheet(
        modifier = Modifier.widthIn(max = 320.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "CSISS",
                color = navy,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Surface(
                color = Color(0xFFF1F6FB),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = userName,
                        color = navy,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = if (role == "ADMIN")
                            "Administrator"
                        else
                            "Staff",
                        color = muted,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            HorizontalDivider()

            Text(
                text = "MANAGEMENT",
                color = muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 12.dp)
            )

            NavigationDrawerItem(
                label = {
                    Text("Supplier Management")
                },
                selected = supplierSelected,
                onClick = onSupplierClick,
                icon = {
                    Icon(
                        imageVector = FluentuiSystemIconsSupplier,
                        contentDescription = null
                    )
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0xFFE9DFF7),
                    selectedTextColor = navy,
                    selectedIconColor = navy
                )
            )

            ComingSoonDrawerRow("Reorder Management")
            ComingSoonDrawerRow("Supplier Claims")
            ComingSoonDrawerRow("Reports")

            if (role == "ADMIN") {
                HorizontalDivider()

                Text(
                    text = "ADMINISTRATION",
                    color = muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )

                ComingSoonDrawerRow("User Management")
            }

            HorizontalDivider()

            Text(
                text = "ACCOUNT",
                color = muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 12.dp)
            )

            NavigationDrawerItem(
                label = {
                    Text("My Profile")
                },
                selected = profileSelected,
                onClick = onProfileClick,
                icon = {
                    Icon(
                        imageVector = FluentuiSystemIconsPerson,
                        contentDescription = null
                    )
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0xFFE9DFF7),
                    selectedTextColor = navy,
                    selectedIconColor = navy
                )
            )

            NavigationDrawerItem(
                label = {
                    Text(
                        text = "Log out",
                        color = Color(0xFFE45465)
                    )
                },
                selected = false,
                onClick = onLogoutClick
            )
        }
    }
}

@Composable
private fun ComingSoonDrawerRow(
    title: String
) {
    val muted = Color(0xFF718198)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 12.dp,
                top = 12.dp,
                bottom = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.width(24.dp))

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = muted,
            fontSize = 14.sp
        )

        Text(
            text = "Soon",
            color = muted,
            fontSize = 11.sp
        )
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