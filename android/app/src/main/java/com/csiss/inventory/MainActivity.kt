package com.csiss.inventory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.csiss.inventory.ui.auth.CsissAuthGate
import com.csiss.inventory.ui.stock.StockScreen
import com.csiss.inventory.ui.temperature.TemperatureScreen
import com.csiss.inventory.ui.theme.CSISSTheme

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
                    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Stock, 1 = Temperature

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding(),
                        topBar = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = currentUser.fullName
                                    )
                                    Text(
                                        text = currentUser.role
                                    )
                                }
                                TextButton(
                                    onClick = onLogout
                                ) {
                                    Text("Logout")
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
                                    icon = { Text("📦") },
                                    label = { Text("Stock") }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = { Text("🌡️") },
                                    label = { Text("Temperature") }
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
                                    canEditLimits = currentUser.role == "ADMIN"
                                )
                            }
                        }
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