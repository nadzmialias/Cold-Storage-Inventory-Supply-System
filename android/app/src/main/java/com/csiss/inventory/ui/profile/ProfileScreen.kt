
package com.csiss.inventory.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csiss.inventory.data.CsissUser

private val Navy = Color(0xFF132945)
private val Background = Color(0xFFF1F6FB)
private val TextDark = Color(0xFF182B48)
private val TextMuted = Color(0xFF718198)
private val AlertRed = Color(0xFFE45465)

@Composable
fun ProfileScreen(
    user: CsissUser,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Page header
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "ACCOUNT",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )

            Text(
                text = "My Profile",
                color = TextDark,
                fontSize = 27.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // User information card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // Avatar using the user's first initial
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                Color(0xFFE7EFF9),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.fullName
                                .trim()
                                .take(1)
                                .uppercase()
                                .ifBlank { "U" },
                            color = Navy,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = user.fullName,
                            color = TextDark,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = if (user.role == "ADMIN") {
                                "Administrator"
                            } else {
                                "Staff"
                            },
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                }

                HorizontalDivider(
                    color = Color(0xFFE8EDF3)
                )

                ProfileInfoRow(
                    label = "Display Name",
                    value = user.fullName
                )

                ProfileInfoRow(
                    label = "Username",
                    value = user.username
                )

                ProfileInfoRow(
                    label = "Account Role",
                    value = user.role
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Logout action
        OutlinedButton(
            onClick = {
                showLogoutDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = AlertRed
            )
        ) {
            Text(
                text = "Log out",
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = {
                showLogoutDialog = false
            },
            title = {
                Text("Log out of CSISS?")
            },
            text = {
                Text(
                    "You will return to the login screen."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text(
                        text = "Log out",
                        color = AlertRed
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 13.sp
        )

        Text(
            text = value,
            color = TextDark,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
