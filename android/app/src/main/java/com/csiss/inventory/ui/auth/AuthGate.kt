    package com.csiss.inventory.ui.auth

    import androidx.compose.foundation.layout.*
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.runtime.saveable.rememberSaveable
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

        var activeUserId by rememberSaveable {
            mutableStateOf<String?>(null)
        }

        var profile by remember {
            mutableStateOf<CsissUser?>(null)
        }

        var profileError by remember {
            mutableStateOf<String?>(null)
        }

        // Only clear profile on genuine sign-out, not on transient token refreshes when resuming the app.
        LaunchedEffect(sessionStatus) {
            if (sessionStatus is SessionStatus.NotAuthenticated) {
                activeUserId = null
                profile = null
                profileError = null
            }
        }

        // Load the profile when a user signs in or an existing session is restored.
        LaunchedEffect(sessionUserId) {
            if (sessionUserId != null) {
                if (sessionUserId != activeUserId) {
                    profile = null
                    profileError = null
                    activeUserId = sessionUserId
                }

                if (profile == null) {
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
                    } catch (_: Exception) {
                        profileError =
                            "Unable to load your account profile."
                    }
                }
            }
        }

        val logout: () -> Unit = {
            scope.launch {
                repository.signOut()
            }
        }

        when {
            sessionStatus is SessionStatus.NotAuthenticated -> {
                LoginScreen()
            }

            profile != null -> {
                content(profile!!, logout)
            }

            sessionStatus is SessionStatus.RefreshFailure -> {
                AuthMessage(
                    "Connection problem. Check your internet connection."
                )
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
