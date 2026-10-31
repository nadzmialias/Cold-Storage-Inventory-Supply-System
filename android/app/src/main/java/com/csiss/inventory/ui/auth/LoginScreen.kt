package com.csiss.inventory.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.csiss.inventory.data.Authentication

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch


// =====================================
// DESIGN COLORS
// =====================================

private val Navy = Color(0xFF102B46)
private val IceBlue = Color(0xFF46B5DC)
private val Background = Color(0xFFF7F9FC)
private val TextPrimary = Color(0xFF172B3D)
private val TextSecondary = Color(0xFF64748B)
private val FieldBorder = Color(0xFFD5DFE8)


// =====================================
// LOGIN SCREEN
// =====================================

@Composable
fun LoginScreen() {

    val repository = remember {
        Authentication()
    }

    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var email by rememberSaveable {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val canLogin =
        email.isNotBlank() &&
                password.isNotBlank() &&
                !loading

    // =====================================
    // LOGIN ACTION
    // =====================================

    val login: () -> Unit = {

        if (canLogin) {

            focusManager.clearFocus()

            scope.launch {

                loading = true
                errorMessage = null

                try {

                    repository.signIn(
                        email = email.trim(),
                        password = password
                    )

                    // AuthGate handles navigation.

                } catch (e: CancellationException) {

                    throw e

                } catch (e: Exception) {

                    errorMessage =
                        "Unable to sign in. Check your credentials."
                } finally {

                    loading = false
                }
            }
        }
    }


    // =====================================
    // RESPONSIVE NON-SCROLLING LAYOUT
    // =====================================

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .safeDrawingPadding()
            .imePadding()
    ) {

        // Available height changes when keyboard opens.
        val compact = maxHeight < 600.dp
        val veryCompact = maxHeight < 420.dp

        val sectionSpacing =
            if (compact) 18.dp else 34.dp

        val fieldSpacing =
            if (compact) 12.dp else 20.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),

            verticalArrangement = Arrangement.Center
        ) {

            // =====================================
            // BRAND IDENTITY
            // =====================================

            if (!veryCompact) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(40.dp)
                            .background(
                                IceBlue,
                                RoundedCornerShape(4.dp)
                            )
                    )

                    Column {

                        Text(
                            text = "CSISS",
                            color = Navy,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "COLD STORAGE INVENTORY",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(
                        sectionSpacing
                    )
                )
            }


            // =====================================
            // FORM HEADER
            // =====================================

            Text(
                text = "Sign in",
                color = TextPrimary,
                fontSize = if (compact) 25.sp else 30.sp,
                fontWeight = FontWeight.Bold
            )

            if (!veryCompact) {

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "Access your CSISS workspace",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            Spacer(
                modifier = Modifier.height(
                    sectionSpacing
                )
            )


            // =====================================
            // EMAIL
            // =====================================

            OutlinedTextField(
                value = email,

                onValueChange = {
                    email = it
                    errorMessage = null
                },

                label = {
                    Text("Email address")
                },

                singleLine = true,

                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),

                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(
                            FocusDirection.Down
                        )
                    }
                ),

                shape = RoundedCornerShape(12.dp),

                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Navy,
                    unfocusedBorderColor = FieldBorder,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),

                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(
                    fieldSpacing
                )
            )


            // =====================================
            // PASSWORD
            // =====================================

            OutlinedTextField(
                value = password,

                onValueChange = {
                    password = it
                    errorMessage = null
                },

                label = {
                    Text("Password")
                },

                singleLine = true,

                visualTransformation =
                    if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },

                trailingIcon = {

                    TextButton(
                        onClick = {
                            passwordVisible = !passwordVisible
                        }
                    ) {

                        Text(
                            text = if (passwordVisible) {
                                "Hide"
                            } else {
                                "Show"
                            },
                            color = Navy,
                            fontSize = 12.sp
                        )
                    }
                },

                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),

                keyboardActions = KeyboardActions(
                    onDone = {
                        login()
                    }
                ),

                shape = RoundedCornerShape(12.dp),

                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Navy,
                    unfocusedBorderColor = FieldBorder,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),

                modifier = Modifier.fillMaxWidth()
            )


            // =====================================
            // ERROR MESSAGE
            // =====================================

            errorMessage?.let { message ->

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            Spacer(
                modifier = Modifier.height(
                    if (compact) 16.dp else 26.dp
                )
            )


            // =====================================
            // SIGN IN BUTTON
            // =====================================

            Button(
                onClick = login,

                enabled = canLogin,

                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),

                shape = RoundedCornerShape(12.dp),

                colors = ButtonDefaults.buttonColors(
                    containerColor = Navy,
                    contentColor = Color.White
                )
            ) {

                if (loading) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )

                } else {

                    Text(
                        text = "Sign in",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }


            // =====================================
            // FOOTER
            // =====================================

            if (!compact) {

                Spacer(
                    modifier = Modifier.height(28.dp)
                )

                Text(
                    text = "Access restricted to authorized personnel",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}