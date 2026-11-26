package com.csiss.inventory.ui.auth

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.csiss.inventory.data.Authentication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val HeaderDarkNavy = Color(0xFF0F2138)
private val HeaderBadgeBg = Color(0xFF1A3557)
private val HeaderSubtitle = Color(0xFFBAE6FD)
private val IceBlue = Color(0xFF34B9DB)
private val Navy = Color(0xFF132945)
private val FrostSurface = Color(0xFFF7FAFD)
private val TextPrimary = Color(0xFF182B48)
private val TextSecondary = Color(0xFF526075)
private val LinkBlue = Color(0xFF0369A1)
private val InputBorder = Color(0xFF7889A0)
private val DisabledBg = Color(0xFFE2E8F0)
private val DisabledText = Color(0xFF475569)
private val ErrorBg = Color(0xFFFDECEF)
private val ErrorBorder = Color(0xFFF7B8C0)
private val ErrorText = Color(0xFFB42332)
private val DividerColor = Color(0xFFE2E8F0)

private val FieldShape = RoundedCornerShape(12.dp)
private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

@Composable
fun LoginScreen() {
    val repository = remember { Authentication() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val view = LocalView.current

    // Switch status bar icons to light over the dark navy header, then restore on exit.
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousLightStatusBars = controller?.isAppearanceLightStatusBars ?: true
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = previousLightStatusBars
        }
    }

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPassword by rememberSaveable { mutableStateOf(false) }

    val canLogin = email.isNotBlank() && password.isNotBlank() && !loading

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
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    errorMessage = "Unable to sign in. Check your email and password, then try again."
                } finally {
                    loading = false
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(HeaderDarkNavy)
            .imePadding()
    ) {
        val density = LocalDensity.current
        val imeVisible = WindowInsets.ime.getBottom(density) > 0
        val isCollapsed = imeVisible || maxHeight < 600.dp

        val collapseProgress by animateFloatAsState(
            targetValue = if (isCollapsed) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "loginSpringDock"
        )

        val sheetCornerRadius = lerp(28.dp, 20.dp, collapseProgress)
        val formVerticalPadding = lerp(26.dp, 18.dp, collapseProgress)
        val sectionSpacing = lerp(20.dp, 14.dp, collapseProgress)

        ColdGridBackdrop(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            LoginBrandHeader(collapseProgress = collapseProgress)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(
                    topStart = sheetCornerRadius,
                    topEnd = sheetCornerRadius
                ),
                color = FrostSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = 24.dp,
                            vertical = formVerticalPadding
                        ),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
                    ) {
                        Column {
                            Text(
                                text = "Sign in",
                                color = Navy,
                                fontSize = 26.sp,
                                lineHeight = 30.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.graphicsLayer {
                                    val scale = 1f - 0.16f * collapseProgress
                                    scaleX = scale
                                    scaleY = scale
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                }
                            )
                            AnimatedVisibility(
                                visible = !isCollapsed,
                                enter = fadeIn(
                                    animationSpec = tween(durationMillis = 180, delayMillis = 60)
                                ) + slideInVertically(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    initialOffsetY = { -it / 2 }
                                ) + expandVertically(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    expandFrom = Alignment.Top
                                ),
                                exit = fadeOut(
                                    animationSpec = tween(durationMillis = 140)
                                ) + slideOutVertically(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    targetOffsetY = { -it / 2 }
                                ) + shrinkVertically(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    shrinkTowards = Alignment.Top
                                )
                            ) {
                                Text(
                                    text = "Use your staff or administrator account.",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                errorMessage = null
                            },
                            label = {
                                Text("Email address")
                            },
                            leadingIcon = {
                                MailVectorIcon(tint = TextSecondary)
                            },
                            singleLine = true,
                            enabled = !loading,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    focusManager.moveFocus(FocusDirection.Down)
                                }
                            ),
                            shape = FieldShape,
                            colors = loginFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    errorMessage = null
                                },
                                label = {
                                    Text("Password")
                                },
                                leadingIcon = {
                                    LockVectorIcon(tint = TextSecondary)
                                },
                                singleLine = true,
                                enabled = !loading,
                                visualTransformation = if (passwordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    TextButton(
                                        onClick = { passwordVisible = !passwordVisible },
                                        enabled = !loading
                                    ) {
                                        Text(
                                            text = if (passwordVisible) "Hide" else "Show",
                                            color = Navy,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { login() }
                                ),
                                shape = FieldShape,
                                colors = loginFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                TextButton(
                                    onClick = { showForgotPassword = true },
                                    enabled = !loading,
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    modifier = Modifier.heightIn(min = 36.dp)
                                ) {
                                    Text(
                                        text = "Forgot password?",
                                        color = LinkBlue,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        errorMessage?.let { message ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = FieldShape,
                                color = ErrorBg,
                                border = BorderStroke(1.dp, ErrorBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "!",
                                        color = ErrorText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = message,
                                        color = ErrorText,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = login,
                            enabled = canLogin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = FieldShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Navy,
                                contentColor = Color.White,
                                disabledContainerColor = DisabledBg,
                                disabledContentColor = DisabledText
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
                    }

                    AnimatedVisibility(
                        visible = !isCollapsed,
                        enter = fadeIn(
                            animationSpec = tween(durationMillis = 180, delayMillis = 60)
                        ) + expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ),
                        exit = fadeOut(
                            animationSpec = tween(durationMillis = 140)
                        ) + shrinkVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            HorizontalDivider(color = DividerColor)
                            Text(
                                text = "Invite-only access. Contact your administrator for an account.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    if (showForgotPassword) {
        ForgotPasswordDialog(
            initialEmail = email,
            repository = repository,
            onDismiss = { showForgotPassword = false }
        )
    }
}

@Composable
private fun LoginBrandHeader(collapseProgress: Float) {
    val headerHeight = lerp(154.dp, 54.dp, collapseProgress)
    val badgeSize = lerp(46.dp, 32.dp, collapseProgress)
    val badgeCorner = lerp(14.dp, 10.dp, collapseProgress)
    val badgeTop = lerp(22.dp, 11.dp, collapseProgress)

    // Dock title beside the shrinking badge and scale via graphicsLayer to avoid text re-layout jitter.
    val titleX = lerp(24.dp, 68.dp, collapseProgress)
    val titleY = lerp(80.dp, 11.dp, collapseProgress)
    val titleScale = 1f - 0.30f * collapseProgress

    val subtitleAlpha = (1f - collapseProgress * 2.2f).coerceIn(0f, 1f)
    val subtitleSlideUp = lerp(0.dp, (-10).dp, collapseProgress)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .clipToBounds()
    ) {
        Box(modifier = Modifier.offset(x = 24.dp, y = badgeTop)) {
            CsissBrandMark(size = badgeSize, cornerRadius = badgeCorner)
        }

        Text(
            text = "CSISS",
            color = Color.White,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            modifier = Modifier
                .offset(x = titleX, y = titleY)
                .graphicsLayer {
                    scaleX = titleScale
                    scaleY = titleScale
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
        )

        if (subtitleAlpha > 0.01f) {
            Text(
                text = "Cold Storage Inventory Supply System",
                color = HeaderSubtitle,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier
                    .offset(x = 24.dp, y = 115.dp + subtitleSlideUp)
                    .graphicsLayer { alpha = subtitleAlpha }
            )
        }
    }
}

@Composable
private fun ColdGridBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val spacing = 28.dp.toPx()
        val lineColor = HeaderSubtitle.copy(alpha = 0.06f)
        val strokeWidth = 1.dp.toPx()

        var x = 0f
        while (x <= size.width) {
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = strokeWidth
            )
            x += spacing
        }

        var y = 0f
        while (y <= size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokeWidth
            )
            y += spacing
        }
    }
}

@Composable
private fun CsissBrandMark(
    size: Dp,
    cornerRadius: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(HeaderBadgeBg, RoundedCornerShape(cornerRadius))
            .border(1.dp, IceBlue.copy(alpha = 0.4f), RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.5f)) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val r = this.size.minDimension / 2f
            val diag = r * 0.72f
            val stroke = 1.9.dp.toPx()

            drawLine(IceBlue, Offset(c.x, c.y - r), Offset(c.x, c.y + r), stroke, StrokeCap.Round)
            drawLine(IceBlue, Offset(c.x - r, c.y), Offset(c.x + r, c.y), stroke, StrokeCap.Round)
            drawLine(IceBlue, Offset(c.x - diag, c.y - diag), Offset(c.x + diag, c.y + diag), stroke, StrokeCap.Round)
            drawLine(IceBlue, Offset(c.x - diag, c.y + diag), Offset(c.x + diag, c.y - diag), stroke, StrokeCap.Round)
            drawCircle(HeaderDarkNavy, radius = r * 0.28f, center = c)
            drawCircle(
                color = HeaderSubtitle,
                radius = r * 0.28f,
                center = c,
                style = Stroke(width = stroke)
            )
        }
    }
}

@Composable
private fun MailVectorIcon(tint: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = 1.6.dp.toPx()
        val inset = stroke
        val rectTop = size.height * 0.16f
        val rectHeight = size.height * 0.68f
        drawRoundRect(
            color = tint,
            topLeft = Offset(inset, rectTop),
            size = Size(size.width - inset * 2f, rectHeight),
            cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
            style = Stroke(width = stroke)
        )
        val flap = Path().apply {
            moveTo(inset + 1f, rectTop + 2f)
            lineTo(size.width / 2f, size.height * 0.54f)
            lineTo(size.width - inset - 1f, rectTop + 2f)
        }
        drawPath(
            path = flap,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun LockVectorIcon(tint: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = 1.6.dp.toPx()
        val bodyTop = size.height * 0.44f
        val bodyLeft = size.width * 0.16f
        val bodyWidth = size.width * 0.68f
        val bodyHeight = size.height * 0.44f

        drawRoundRect(
            color = tint,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
            style = Stroke(width = stroke)
        )

        val shackleLeft = size.width * 0.30f
        val shackleRight = size.width * 0.70f
        val shackleTop = size.height * 0.14f
        val shackle = Path().apply {
            moveTo(shackleLeft, bodyTop)
            lineTo(shackleLeft, size.height * 0.30f)
            cubicTo(
                shackleLeft, shackleTop,
                shackleRight, shackleTop,
                shackleRight, size.height * 0.30f
            )
            lineTo(shackleRight, bodyTop)
        }
        drawPath(
            path = shackle,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun loginFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Navy,
    unfocusedBorderColor = InputBorder,
    errorBorderColor = ErrorText,
    focusedLabelColor = Navy,
    unfocusedLabelColor = TextSecondary,
    errorLabelColor = ErrorText,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = FrostSurface,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = Navy
)

@Composable
private fun ForgotPasswordDialog(
    initialEmail: String,
    repository: Authentication,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var resetEmail by rememberSaveable { mutableStateOf(initialEmail.trim()) }
    var sending by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val emailValid = EMAIL_REGEX.matches(resetEmail.trim())

    val submitReset: () -> Unit = {
        if (emailValid && !sending) {
            focusManager.clearFocus()
            scope.launch {
                sending = true
                error = null
                try {
                    repository.sendPasswordResetEmail(resetEmail)
                    sent = true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val raw = e.message.orEmpty()
                    error = if (
                        raw.contains("rate", ignoreCase = true) ||
                        raw.contains("security purposes", ignoreCase = true)
                    ) {
                        "Please wait a minute before requesting another reset link."
                    } else {
                        "Unable to send reset link. Check your connection and try again."
                    }
                } finally {
                    sending = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = if (sent) "Check your email" else "Reset password",
                color = Navy,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (sent) {
                Text(
                    text = "If an account exists for ${resetEmail.trim()}, a password reset link has been sent. Open the link on this device to choose a new password.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your account email and we will send you a password reset link.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = {
                            resetEmail = it
                            error = null
                        },
                        label = { Text("Email address") },
                        singleLine = true,
                        enabled = !sending,
                        isError = resetEmail.isNotBlank() && !emailValid,
                        supportingText = {
                            if (resetEmail.isNotBlank() && !emailValid) {
                                Text("Enter a valid email address.")
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitReset() }
                        ),
                        shape = FieldShape,
                        colors = loginFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    error?.let { msg ->
                        Text(
                            text = msg,
                            color = ErrorText,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (sent) {
                Button(
                    onClick = onDismiss,
                    shape = FieldShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Navy,
                        contentColor = Color.White
                    )
                ) {
                    Text("Back to Sign in", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    enabled = emailValid && !sending,
                    shape = FieldShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Navy,
                        contentColor = Color.White,
                        disabledContainerColor = DisabledBg,
                        disabledContentColor = DisabledText
                    ),
                    onClick = submitReset
                ) {
                    if (sending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Send Reset Link", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        dismissButton = {
            if (!sent) {
                TextButton(
                    enabled = !sending,
                    onClick = onDismiss
                ) {
                    Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    )
}
