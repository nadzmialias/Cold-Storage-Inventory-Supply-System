
package com.csiss.inventory.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csiss.inventory.data.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val Navy = Color(0xFF132945)
private val Background = Color(0xFFF1F6FB)
private val Muted = Color(0xFF718198)

@Composable
fun SetPasswordScreen(
    invitedUserId: String,
    isPasswordReset: Boolean = false,
    onFinished: () -> Unit,
    onCancel: () -> Unit
) {
    val auth = remember {
        SupabaseClientProvider.client.auth
    }
    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    val sessionStatus by auth.sessionStatus.collectAsState()

    val correctSession =
        (sessionStatus as? SessionStatus.Authenticated)
            ?.session?.user?.id == invitedUserId

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val validPassword =
        password.length >= 8 &&
                password == confirmPassword

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "CSISS",
            color = Navy,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = if (isPasswordReset) "Reset Your Password" else "Set Your Password",
            color = Navy,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (isPasswordReset) {
                "Your reset link has been verified. Create a new password for your account."
            } else {
                "Your account invitation has been accepted. Create a password to finish setting up your account."
            },
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )

        if (!correctSession) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Navy
            )

            Text(
                text = "Finishing secure sign-in...",
                color = Muted,
                fontSize = 12.sp
            )
        }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("New password") },
            singleLine = true,
            enabled = !saving,
            visualTransformation =
                if (showPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
            trailingIcon = {
                TextButton(
                    onClick = {
                        showPassword = !showPassword
                    }
                ) {
                    Text(if (showPassword) "Hide" else "Show")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm password") },
            singleLine = true,
            enabled = !saving,
            visualTransformation = PasswordVisualTransformation(),
            isError = confirmPassword.isNotEmpty() &&
                    password != confirmPassword,
            supportingText = {
                if (
                    confirmPassword.isNotEmpty() &&
                    password != confirmPassword
                ) {
                    Text("Passwords do not match.")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Text(
            text = "Use at least 8 characters.",
            color = Muted,
            fontSize = 12.sp
        )

        Button(
            onClick = {
                scope.launch {
                    saving = true
                    error = null

                    try {

                        val newPassword = password

                        auth.updateUser {
                            this.password = newPassword
                        }


                        // Clear the temporary callback session before returning to sign in.
                        auth.signOut()

                        Toast.makeText(
                            context,
                            if (isPasswordReset) {
                                "Password updated. Sign in with your new password."
                            } else {
                                "Password saved. You can now sign in."
                            },
                            Toast.LENGTH_LONG
                        ).show()

                        onFinished()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        error = friendlyPasswordError(e, isPasswordReset)
                    } finally {
                        saving = false
                    }
                }
            },
            enabled = validPassword && correctSession && !saving,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Navy
            )
        ) {
            if (saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (isPasswordReset) "Update Password" else "Save Password",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        TextButton(
            onClick = onCancel,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Return to Login",
                color = Muted
            )
        }
    }

    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = {
                Text(
                    if (isPasswordReset) "Unable to reset password" else "Unable to set password"
                )
            },
            text = {
                Text(message)
            },
            confirmButton = {
                TextButton(onClick = { error = null }) {
                    Text("Got it", color = Navy)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

private fun friendlyPasswordError(e: Exception, isPasswordReset: Boolean): String {
    val raw = e.message.orEmpty()
    return when {
        raw.contains("same_password", ignoreCase = true) ||
            raw.contains("different from the old password", ignoreCase = true) ->
            "Your new password must be different from your current password."

        raw.contains("weak_password", ignoreCase = true) ||
            raw.contains("password should be at least", ignoreCase = true) ->
            "Please choose a stronger password with at least 8 characters."

        raw.contains("session", ignoreCase = true) ||
            raw.contains("expired", ignoreCase = true) ||
            raw.contains("unauthorized", ignoreCase = true) ->
            if (isPasswordReset) {
                "Your reset link session has expired. Please request a new reset link."
            } else {
                "Your invitation session has expired. Please request a new invitation."
            }

        raw.contains("rate", ignoreCase = true) ||
            raw.contains("too many", ignoreCase = true) ->
            "Too many attempts. Please wait a moment and try again."

        else ->
            if (isPasswordReset) {
                "Unable to update your password. Check your connection and try again."
            } else {
                "Unable to save your password. Check your connection and try again."
            }
    }
}


