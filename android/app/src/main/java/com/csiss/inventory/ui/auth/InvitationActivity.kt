
package com.csiss.inventory.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.csiss.inventory.MainActivity
import com.csiss.inventory.data.SupabaseClientProvider
import com.csiss.inventory.ui.theme.CSISSTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class InvitationActivity : ComponentActivity() {

    private val client = SupabaseClientProvider.client

    private var invitedUserId by mutableStateOf<String?>(null)
    private var invitationError by mutableStateOf<String?>(null)

    private var timeoutJob: Job? = null
    private var sessionJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.getInsetsController(
            window,
            window.decorView
        ).isAppearanceLightStatusBars = true

        handleInvitation(intent)

        setContent {
            CSISSTheme(
                darkTheme = false,
                dynamicColor = false
            ) {
                BackHandler {
                    leaveInvitation()
                }

                when {
                    invitedUserId != null -> {
                        SetPasswordScreen(
                            invitedUserId = invitedUserId!!,
                            onFinished = ::openLogin,
                            onCancel = ::leaveInvitation
                        )
                    }

                    invitationError != null -> {
                        InvitationStatusScreen(
                            message = invitationError,
                            onBack = ::leaveInvitation
                        )
                    }

                    else -> {
                        InvitationStatusScreen(
                            message = null,
                            onBack = ::leaveInvitation
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleInvitation(intent)
    }

    private fun handleInvitation(incomingIntent: Intent) {
        timeoutJob?.cancel()
        sessionJob?.cancel()

        invitedUserId = null
        invitationError = null

        val uri = incomingIntent.data

        if (
            uri == null ||
            uri.scheme != "com.csiss.inventory" ||
            uri.host != "auth" ||
            uri.path != "/callback"
        ) {
            invitationError = "Invalid invitation link."
            return
        }

        // Supabase can return errors in the query or fragment.
        val fragment = uri.fragment?.let {
            Uri.parse("https://callback.invalid/?$it")
        }

        val linkError =
            uri.getQueryParameter("error_description")
                ?: fragment?.getQueryParameter("error_description")

        if (linkError != null) {
            invitationError = linkError
            return
        }

        val hasTokens =
            !fragment?.getQueryParameter("access_token").isNullOrBlank() &&
                    !fragment?.getQueryParameter("refresh_token").isNullOrBlank()

        val hasCode =
            !uri.getQueryParameter("code").isNullOrBlank()

        if (!hasTokens && !hasCode) {
            invitationError =
                "The invitation link is missing authentication details. " +
                        "Please request a new invitation."
            return
        }

        // Prevent an endless verification spinner.
        timeoutJob = lifecycleScope.launch {
            delay(15_000L)

            if (invitedUserId == null && invitationError == null) {
                invitationError =
                    "We couldn't verify this invitation. " +
                            "It may have expired or already been used. " +
                            "Please request a new invitation."
            }
        }

        try {
            client.handleDeeplinks(
                intent = incomingIntent,
                onSessionSuccess = { session ->
                    timeoutJob?.cancel()

                    val userId = session.user?.id

                    if (userId.isNullOrBlank()) {
                        invitationError =
                            "Unable to identify the invited account."
                    } else {
                        // Wait for the invited account's session,
                        // not a previously signed-in Admin session.
                        sessionJob = lifecycleScope.launch {
                            val authenticated = withTimeoutOrNull(12_000L) {
                                client.auth.sessionStatus.first { status ->
                                    status is SessionStatus.Authenticated &&
                                            status.session.user?.id == userId
                                }
                            }

                            if (authenticated != null) {
                                invitationError = null
                                invitedUserId = userId
                            } else {
                                invitationError =
                                    "The invitation session could not be established. " +
                                            "Please request a new invitation."
                            }
                        }
                    }
                }
            )
        } catch (e: Exception) {
            timeoutJob?.cancel()
            invitationError =
                "Unable to verify your invitation. " +
                        "Please try a new invitation link."
        }
    }

    private fun leaveInvitation() {
        timeoutJob?.cancel()
        sessionJob?.cancel()

        lifecycleScope.launch {
            try {
                client.auth.signOut()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Return to login even if sign-out fails.
            }

            openLogin()
        }
    }

    private fun openLogin() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )

        finish()
    }
}

@Composable
private fun InvitationStatusScreen(
    message: String?,
    onBack: () -> Unit
) {
    val navy = Color(0xFF132945)
    val muted = Color(0xFF718198)
    val loading = message == null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F6FB))
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(color = navy)
            } else {
                Text(
                    text = "!",
                    color = Color(0xFFB42332),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = if (loading) {
                    "Verifying your invitation..."
                } else {
                    "Unable to Verify Invitation"
                },
                color = navy,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (!loading) {
                Text(
                    text = message.orEmpty(),
                    color = muted,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = navy,
                    contentColor = Color.White
                )
            ) {
                Text("Return to Login")
            }
        }
    }
}
