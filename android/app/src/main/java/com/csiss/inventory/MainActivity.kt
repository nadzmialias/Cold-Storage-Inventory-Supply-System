package com.csiss.inventory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.csiss.inventory.ui.theme.CSISSTheme
import com.csiss.inventory.ui.temperature.TemperatureScreen
  import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.csiss.inventory.ui.auth.CsissAuthGate
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.core.view.WindowCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.csiss.inventory.ui.inventory.InventoryScreen

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
                    var showInventory by rememberSaveable { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            verticalAlignment =
                                Alignment.CenterVertically
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
                        Row(modifier = Modifier.padding(horizontal = 12.dp)) {
                            TextButton(onClick = { showInventory = false }) {
                                Text("Temperature")
                            }

                            TextButton(onClick = { showInventory = true }) {
                                Text("Inventory")
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            if (showInventory) {
                                InventoryScreen()
                            } else {
                                TemperatureScreen(
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