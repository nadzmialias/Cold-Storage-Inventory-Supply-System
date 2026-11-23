package com.csiss.inventory

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.csiss.inventory.data.AlertRepository
import com.csiss.inventory.data.SystemAlertRow
import com.csiss.inventory.ui.auth.CsissAuthGate
import com.csiss.inventory.ui.components.BootstrapExclamationSquare
import com.csiss.inventory.ui.components.FluentuiSystemIconsAlert
import com.csiss.inventory.ui.components.FluentuiSystemIconsBox
import com.csiss.inventory.ui.components.FluentuiSystemIconsClipboardBulletListRtl
import com.csiss.inventory.ui.components.FluentuiSystemIconsPerson
import com.csiss.inventory.ui.components.FluentuiSystemIconsSupplier
import com.csiss.inventory.ui.components.FluentuiSystemIconsTemperature
import com.csiss.inventory.ui.components.HeroiconsUserGroup
import com.csiss.inventory.ui.components.MaterialIconsInventory
import com.csiss.inventory.ui.components.VscodeCodiconsGraphLine
import com.csiss.inventory.ui.inventory.InventoryScreen
import com.csiss.inventory.ui.notification.AlertNotificationHelper
import com.csiss.inventory.ui.notification.NotificationScreen
import com.csiss.inventory.ui.profile.ProfileScreen
import com.csiss.inventory.ui.reorder.ReorderScreen
import com.csiss.inventory.ui.report.ReportScreen
import com.csiss.inventory.ui.stock.StockScreen
import com.csiss.inventory.ui.supplier.SupplierScreen
import com.csiss.inventory.ui.temperature.TemperatureScreen
import com.csiss.inventory.ui.theme.CSISSTheme
import com.csiss.inventory.ui.user.UserManagementScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Stock : Screen("stock")
    object Inventory : Screen("inventory")
    object Temperature : Screen("temperature")
    object Alerts : Screen("alerts")
    object Profile : Screen("profile")
    object Supplier : Screen("supplier")
    object UserManagement : Screen("user_management")
    object Reorder : Screen("reorder")
    object Reports : Screen("reports")
}

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
                var lastRoute by rememberSaveable { mutableStateOf(Screen.Stock.route) }

                CsissAuthGate { currentUser, onLogout ->
                    val context = LocalContext.current
                    val alertRepository = remember { AlertRepository() }

                    val initialRoute = remember { lastRoute }
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    LaunchedEffect(currentRoute) {
                        if (currentRoute != null) {
                            lastRoute = currentRoute
                        }
                    }

                    val handleLogout: () -> Unit = {
                        lastRoute = Screen.Stock.route
                        onLogout()
                    }

                    var alertsFilter by remember { mutableIntStateOf(0) }
                    var openAlerts by remember { mutableStateOf<List<SystemAlertRow>>(emptyList()) }
                    var openAlertCount by remember { mutableIntStateOf(0) }
                    var showStartupAlertPopup by remember { mutableStateOf(false) }
                    var hasShownPopupForSession by remember(currentUser.fullName) {
                        mutableStateOf(false)
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
                    LaunchedEffect(currentUser.fullName, currentRoute) {
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
                                supplierSelected = currentRoute == Screen.Supplier.route,
                                profileSelected = currentRoute == Screen.Profile.route,
                                userSelected = currentRoute == Screen.UserManagement.route,
                                onSupplierClick = {
                                    drawerScope.launch {
                                        drawerState.close()
                                        navController.navigate(Screen.Supplier.route) {
                                            popUpTo(navController.graph.startDestinationId)
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                onProfileClick = {
                                    drawerScope.launch {
                                        drawerState.close()
                                        navController.navigate(Screen.Profile.route) {
                                            popUpTo(navController.graph.startDestinationId)
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                onLogoutClick = {
                                    drawerScope.launch {
                                        drawerState.close()
                                        showDrawerLogoutDialog = true
                                    }
                                },
                                onUserClick = {
                                    drawerScope.launch {
                                        drawerState.close()
                                        navController.navigate(Screen.UserManagement.route) {
                                            popUpTo(navController.graph.startDestinationId)
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                reorderSelected = currentRoute == Screen.Reorder.route,
                                onReorderClick = {
                                    drawerScope.launch {
                                        drawerState.close()
                                        navController.navigate(Screen.Reorder.route) {
                                            popUpTo(navController.graph.startDestinationId)
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                reportsSelected = currentRoute == Screen.Reports.route,
                                onReportsClick = {
                                    drawerScope.launch {
                                        drawerState.close()
                                        navController.navigate(Screen.Reports.route) {
                                            popUpTo(navController.graph.startDestinationId)
                                            launchSingleTop = true
                                        }
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
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IconButton(
                                            onClick = {
                                                drawerScope.launch {
                                                    drawerState.open()
                                                }
                                            },
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .padding(start = 12.dp)
                                                .semantics {
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

                                        Text(
                                            text = "CSISS",
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            color = Color(0xFF132945),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            },
                            bottomBar = {
                                NavigationBar(
                                    containerColor = Color.White
                                ) {
                                    NavigationBarItem(
                                        selected = currentRoute == Screen.Stock.route,
                                        onClick = {
                                            navController.navigate(Screen.Stock.route) {
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = FluentuiSystemIconsBox,
                                                contentDescription = null
                                            )
                                        },
                                        label = { BottomNavLabel("Stock") }
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute == Screen.Inventory.route,
                                        onClick = {
                                            navController.navigate(Screen.Inventory.route) {
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = MaterialIconsInventory,
                                                contentDescription = null
                                            )
                                        },
                                        label = { BottomNavLabel("Inventory") }
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute == Screen.Temperature.route,
                                        onClick = {
                                            navController.navigate(Screen.Temperature.route) {
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = FluentuiSystemIconsTemperature,
                                                contentDescription = null
                                            )
                                        },
                                        label = { BottomNavLabel("Temp") }
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute == Screen.Alerts.route,
                                        onClick = {
                                            alertsFilter = 0
                                            navController.navigate(Screen.Alerts.route) {
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
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
                                }
                            }
                        ) { innerPadding ->
                            NavHost(
                                navController = navController,
                                startDestination = initialRoute,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                enterTransition = { EnterTransition.None },
                                exitTransition = { ExitTransition.None },
                                popEnterTransition = { EnterTransition.None },
                                popExitTransition = { ExitTransition.None }
                            ) {
                                composable(Screen.Stock.route) {
                                    StockScreen(currentUserId = currentUser.userId)
                                }
                                composable(Screen.Temperature.route) {
                                    TemperatureScreen(
                                        canEditLimits = currentUser.role == "ADMIN",
                                        onOpenAlerts = {
                                            alertsFilter = 2
                                            navController.navigate(Screen.Alerts.route) {
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                                composable(Screen.Alerts.route) {
                                    NotificationScreen(
                                        initialFilter = alertsFilter,
                                        userRole = currentUser.role,
                                        onOpenCountChanged = { newCount ->
                                            openAlertCount = newCount
                                        }
                                    )
                                }
                                composable(Screen.Profile.route) {
                                    ProfileScreen(
                                        user = currentUser,
                                        onLogout = handleLogout
                                    )
                                }
                                composable(Screen.Inventory.route) {
                                    InventoryScreen(
                                        userRole = currentUser.role,
                                        onHome = { navController.navigate(Screen.Stock.route) },
                                        onAlerts = {
                                            alertsFilter = 0
                                            navController.navigate(Screen.Alerts.route)
                                        },
                                        onProfile = { navController.navigate(Screen.Profile.route) }
                                    )
                                }
                                composable(Screen.Supplier.route) {
                                    SupplierScreen(
                                        onHome = { navController.navigate(Screen.Stock.route) },
                                        onAlerts = {
                                            alertsFilter = 0
                                            navController.navigate(Screen.Alerts.route)
                                        },
                                        onProfile = { navController.navigate(Screen.Profile.route) }
                                    )
                                }
                                composable(Screen.UserManagement.route) {
                                    if (currentUser.role == "ADMIN") {
                                        UserManagementScreen(
                                            currentUserId = currentUser.userId
                                        )
                                    }
                                }
                                composable(Screen.Reorder.route) {
                                    ReorderScreen()
                                }
                                composable(Screen.Reports.route) {
                                    ReportScreen()
                                }
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
                                        navController.navigate(Screen.Alerts.route) {
                                            popUpTo(navController.graph.startDestinationId)
                                            launchSingleTop = true
                                        }
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
                                        handleLogout()
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
    onLogoutClick: () -> Unit,
    userSelected: Boolean,
    onUserClick: () -> Unit,
    reorderSelected: Boolean,
    onReorderClick: () -> Unit,
    reportsSelected: Boolean,
    onReportsClick: () -> Unit
) {
    val navy = Color(0xFF132945)
    val ink = Color(0xFF182B48)
    val slate = Color(0xFF526075)
    val cardBg = Color(0xFFF5F7FA)
    val border = Color(0xFFE2E8F0)
    val divider = Color(0xFFEDF2F7)
    val dangerText = Color(0xFFC81E32)

    val initial = userName.trim().take(1).uppercase().ifBlank { "U" }
    val roleLabel = if (role == "ADMIN") "Administrator" else "Staff"

    ModalDrawerSheet(
        modifier = Modifier.width(288.dp),
        drawerContainerColor = Color.White,
        drawerShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "CSISS",
                    color = navy,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Cold storage",
                    color = slate,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(Modifier.height(16.dp))

            Surface(
                color = cardBg,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(navy, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = userName,
                            color = ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = roleLabel,
                            color = slate,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = divider)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DrawerSectionTitle("Management")

                DrawerMenuItem(
                    label = "Supplier Management",
                    icon = FluentuiSystemIconsSupplier,
                    selected = supplierSelected,
                    onClick = onSupplierClick
                )

                DrawerMenuItem(
                    label = "Reorder Management",
                    icon = FluentuiSystemIconsClipboardBulletListRtl,
                    selected = reorderSelected,
                    onClick = onReorderClick
                )

                DrawerMenuItem(
                    label = "Reports",
                    icon = VscodeCodiconsGraphLine,
                    selected = reportsSelected,
                    onClick = onReportsClick
                )

                ComingSoonDrawerRow(
                    title = "Supplier Claims",
                    imageVector = BootstrapExclamationSquare
                )

                if (role == "ADMIN") {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = divider)
                    Spacer(Modifier.height(4.dp))
                    DrawerSectionTitle("Administration")

                    DrawerMenuItem(
                        label = "User Management",
                        icon = HeroiconsUserGroup,
                        selected = userSelected,
                        onClick = onUserClick
                    )
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = divider)
                Spacer(Modifier.height(4.dp))
                DrawerSectionTitle("Account")

                DrawerMenuItem(
                    label = "My Profile",
                    icon = FluentuiSystemIconsPerson,
                    selected = profileSelected,
                    onClick = onProfileClick
                )
            }

            HorizontalDivider(color = divider)
            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, border),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = dangerText
                )
            ) {
                Text(
                    text = "Log out",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DrawerSectionTitle(text: String) {
    Text(
        text = text,
        color = Color(0xFF526075),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun DrawerMenuItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val navy = Color(0xFF132945)
    val activeBg = Color(0xFFEEF4FA)
    val ink = Color(0xFF182B48)
    val iconMuted = Color(0xFF526075)

    Surface(
        onClick = onClick,
        color = if (selected) activeBg else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) navy else iconMuted
            )
            Text(
                text = label,
                color = if (selected) navy else ink,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ComingSoonDrawerRow(
    title: String,
    imageVector: ImageVector
) {
    val upcomingMuted = Color(0xFF64748B)
    val tagBg = Color(0xFFF1F5F9)
    val tagText = Color(0xFF526075)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = upcomingMuted
        )
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = upcomingMuted,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Surface(
            color = tagBg,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = "Coming soon",
                color = tagText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
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