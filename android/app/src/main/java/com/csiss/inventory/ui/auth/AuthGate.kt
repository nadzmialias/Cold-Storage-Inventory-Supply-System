package com.csiss.inventory.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.csiss.inventory.data.Authentication
import com.csiss.inventory.data.CsissUser
import com.csiss.inventory.data.SupabaseClientProvider

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun CsissAuthGate(
    content: @Composable (
        CsissUser,
        () -> Unit
    ) -> Unit
) {

    val auth = remember {
        SupabaseClientProvider.client.auth
    }

    val repository = remember {
        Authentication()
    }

    val scope = rememberCoroutineScope()

    val sessionStatus by auth.sessionStatus.collectAsState()

    val sessionUserId =
        (sessionStatus as? SessionStatus.Authenticated)
            ?.session
            ?.user
            ?.id

    var profile by remember(sessionUserId) {
        mutableStateOf<CsissUser?>(null)
    }

    var profileError by remember(sessionUserId) {
        mutableStateOf<String?>(null)
    }

    // Check the profile whenever a user signs in
    // or an existing session is restored.
    LaunchedEffect(sessionUserId) {

        if (sessionUserId != null) {

            try {

                val user = repository.getCurrentProfile()

                if (
                    user != null &&
                    user.status == "ACTIVE" &&
                    user.role in listOf("ADMIN", "STAFF")
                ) {

                    profile = user

                } else {

                    profileError =
                        "This account is not authorized for CSISS."
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {

                profileError =
                    "Unable to load your account profile."
            }
        }
    }

    val logout: () -> Unit = {
        scope.launch {
            repository.signOut()
        }
    }

    when {

        sessionStatus is SessionStatus.RefreshFailure -> {
            AuthMessage(
                "Connection problem. Check your internet connection."
            )
        }

        sessionUserId == null &&
                sessionStatus is SessionStatus.NotAuthenticated -> {

            LoginScreen()
        }

        sessionUserId == null -> {
            AuthLoading()
        }

        profile != null -> {

            content(profile!!, logout)
        }

        profileError != null -> {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text(profileError!!)

                Spacer(Modifier.height(16.dp))

                Button(onClick = logout) {
                    Text("Sign Out")
                }
            }
        }

        else -> {
            AuthLoading()
        }
    }
}

@Composable
private fun AuthLoading() {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        CircularProgressIndicator()
    }
}

@Composable
private fun AuthMessage(message: String) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {

        Text(message)
    }
}