package com.csiss.inventory.ui.user

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.csiss.inventory.data.ManagedUser
import com.csiss.inventory.data.UserDraft
import com.csiss.inventory.ui.components.CsissLightRefreshIndicator

private val Navy = Color(0xFF132945)
private val IceBlue = Color(0xFF34B9DB)
private val IceBlueSoft = Color(0xFFE6F6FB)
private val Background = Color(0xFFF1F6FB)
private val Ink = Color(0xFF182B48)
private val Muted = Color(0xFF718198)
private val CardBorder = Color(0xFFE0E6EF)
private val DividerColor = Color(0xFFEDF2F7)
private val AvatarBg = Color(0xFFEAF2FA)
private val Red = Color(0xFFE45465)
private val RedDark = Color(0xFFB42332)
private val RedBg = Color(0xFFFDECEF)
private val RedBorder = Color(0xFFF7B8C0)
private val Green = Color(0xFF159C76)
private val GreenDark = Color(0xFF117A5C)
private val GreenBg = Color(0xFFE3F7F0)
private val NeutralBadge = Color(0xFF495A70)
private val NeutralBadgeBg = Color(0xFFE8EEF6)

private val CardShape = RoundedCornerShape(18.dp)
private val FieldShape = RoundedCornerShape(12.dp)
private val BadgeShape = RoundedCornerShape(50.dp)
private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    currentUserId: String,
    viewModel: UserManagementViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var page by rememberSaveable { mutableStateOf("list") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var confirmStatus by remember { mutableStateOf(false) }

    val selected = uiState.users.firstOrNull { it.id == selectedId }
    val pullToRefreshState = rememberPullToRefreshState()

    val back: () -> Unit = {
        if (!uiState.isSaving) {
            page = when (page) {
                "edit" -> if (selected != null) "details" else "list"
                else -> "list"
            }
            viewModel.clearError()
        }
    }

    BackHandler(enabled = page != "list", onBack = back)

    // Guard against stale selection after reload or process restoration
    LaunchedEffect(page, selected, uiState.hasLoadedOnce) {
        if (uiState.hasLoadedOnce && (page == "details" || page == "edit") && selected == null) {
            page = "list"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        when (page) {
            "list" -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isLoading,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize(),
                    state = pullToRefreshState,
                    indicator = {
                        CsissLightRefreshIndicator(
                            state = pullToRefreshState,
                            isRefreshing = uiState.isLoading,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                ) {
                    val filtered = uiState.users.filter { user ->
                        val matchesSearch =
                            user.fullName.contains(search, true) ||
                                user.username.contains(search, true) ||
                                user.email.contains(search, true)

                        val matchesStatus =
                            filter == "All" || user.status.equals(filter, true)

                        matchesSearch && matchesStatus
                    }

                    val filterOptions = listOf("All", "Active", "Inactive")

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 12.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "ADMINISTRATION",
                                    color = Muted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "User Management",
                                    color = Ink,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = search,
                                onValueChange = { search = it },
                                label = { Text("Search users...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = FieldShape,
                                colors = userFieldColors()
                            )
                        }

                        item {
                            UserSegmentedTabs(
                                options = filterOptions,
                                selected = filter,
                                onSelect = { filter = it }
                            )
                        }

                        item {
                            Button(
                                onClick = {
                                    selectedId = null
                                    viewModel.clearError()
                                    page = "add"
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = FieldShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Navy,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "+ Add New User",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        item {
                            Text(
                                text = "Users (${filtered.size})",
                                color = Ink,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!uiState.hasLoadedOnce && uiState.isLoading) {
                            item {
                                UserEmptyCard(
                                    title = "Loading users...",
                                    subtitle = null
                                )
                            }
                        } else if (filtered.isEmpty()) {
                            item {
                                UserEmptyCard(
                                    title = if (!uiState.hasLoadedOnce) {
                                        "No user data available."
                                    } else {
                                        "No users found."
                                    },
                                    subtitle = if (search.isNotBlank() || filter != "All") {
                                        "Try adjusting your search or status filter."
                                    } else null
                                )
                            }
                        } else {
                            items(filtered, key = { it.id }) { user ->
                                UserCard(user) {
                                    selectedId = user.id
                                    page = "details"
                                }
                            }
                        }
                    }
                }
            }

            "details" -> {
                if (selected != null) {
                    UserDetails(
                        user = selected,
                        isCurrentUser = selected.id == currentUserId,
                        saving = uiState.isSaving,
                        onBack = back,
                        onEdit = {
                            viewModel.clearError()
                            page = "edit"
                        },
                        onChangeStatus = { confirmStatus = true }
                    )
                }
            }

            "add" -> {
                UserForm(
                    editing = null,
                    saving = uiState.isSaving,
                    onBack = back,
                    onSave = { draft ->
                        viewModel.saveUser(editingId = null, draft = draft) {
                            page = "list"
                        }
                    }
                )
            }

            "edit" -> {
                if (selected != null) {
                    UserForm(
                        editing = selected,
                        saving = uiState.isSaving,
                        onBack = back,
                        onSave = { draft ->
                            viewModel.saveUser(editingId = selected.id, draft = draft) {
                                page = "details"
                            }
                        }
                    )
                }
            }
        }
    }

    uiState.error?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color = RedBg, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "!",
                        color = RedDark,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            title = {
                Text(
                    text = if (page == "add" || page == "edit") {
                        "Please check your details"
                    } else {
                        "Something went wrong"
                    },
                    color = Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = message,
                    color = Muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearError() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = FieldShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Navy,
                        contentColor = Color.White
                    )
                ) {
                    Text("Got it", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    if (confirmStatus && selected != null) {
        val target = selected
        val isDeactivating = target.status == "ACTIVE"
        val newStatus = if (isDeactivating) "INACTIVE" else "ACTIVE"

        AlertDialog(
            onDismissRequest = { if (!uiState.isSaving) confirmStatus = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = if (isDeactivating) "Deactivate account?" else "Activate account?",
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Set ${target.fullName} to ${if (isDeactivating) "Inactive" else "Active"}?",
                    color = Muted,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    enabled = !uiState.isSaving,
                    shape = FieldShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDeactivating) Red else Green,
                        contentColor = Color.White
                    ),
                    onClick = {
                        viewModel.changeStatus(target.id, newStatus) {
                            confirmStatus = false
                        }
                    }
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = if (isDeactivating) "Deactivate" else "Activate",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !uiState.isSaving,
                    onClick = { confirmStatus = false }
                ) {
                    Text("Cancel", color = Muted, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
private fun UserSegmentedTabs(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FieldShape,
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (isSelected) Navy else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { onSelect(option) }
                        )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = option,
                            color = if (isSelected) Color.White else Muted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserEmptyCard(
    title: String,
    subtitle: String?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                color = Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = Muted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun UserStatusBadge(
    text: String,
    textColor: Color,
    backgroundColor: Color
) {
    Surface(
        shape = BadgeShape,
        color = backgroundColor
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun UserCard(
    user: ManagedUser,
    onClick: () -> Unit
) {
    val active = user.status == "ACTIVE"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White,
        shape = CardShape,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(AvatarBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.fullName.trim().take(1).uppercase().ifBlank { "U" },
                        color = Navy,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        text = user.fullName,
                        color = Ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = user.email.ifBlank { "@${user.username}" },
                        color = Muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "›",
                    color = IceBlue,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (user.role == "ADMIN") "Administrator" else "Staff",
                    color = Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                UserStatusBadge(
                    text = if (active) "Active" else "Inactive",
                    textColor = if (active) GreenDark else NeutralBadge,
                    backgroundColor = if (active) GreenBg else NeutralBadgeBg
                )
            }
        }
    }
}

@Composable
private fun UserDetails(
    user: ManagedUser,
    isCurrentUser: Boolean,
    saving: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onChangeStatus: () -> Unit
) {
    val active = user.status == "ACTIVE"
    val roleLabel = if (user.role == "ADMIN") "Administrator" else "Staff"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeading(
                title = "User Details",
                onBack = onBack,
                backEnabled = !saving
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = CardShape,
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(AvatarBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.fullName.trim().take(1).uppercase().ifBlank { "U" },
                            color = Navy,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = user.fullName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )

                    Text(
                        text = "@${user.username}",
                        color = Muted,
                        fontSize = 13.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UserStatusBadge(
                            text = roleLabel,
                            textColor = Navy,
                            backgroundColor = IceBlueSoft
                        )
                        UserStatusBadge(
                            text = if (active) "Active" else "Inactive",
                            textColor = if (active) GreenDark else NeutralBadge,
                            backgroundColor = if (active) GreenBg else NeutralBadgeBg
                        )
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = CardShape,
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow("Full Name", user.fullName)
                    HorizontalDivider(color = DividerColor)
                    DetailRow("Email", user.email)
                    HorizontalDivider(color = DividerColor)
                    DetailRow("Username", user.username)
                    HorizontalDivider(color = DividerColor)
                    DetailRow("Role", roleLabel)
                    HorizontalDivider(color = DividerColor)
                    DetailRow(
                        label = "Status",
                        value = if (active) "Active" else "Inactive",
                        valueColor = if (active) GreenDark else Muted
                    )
                    HorizontalDivider(color = DividerColor)
                    DetailRow("Created", user.createdAt.take(10))
                }
            }
        }

        if (!isCurrentUser) {
            item {
                Button(
                    onClick = onEdit,
                    enabled = !saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Navy,
                        contentColor = Color.White
                    ),
                    shape = FieldShape
                ) {
                    Text("Edit User", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            item {
                OutlinedButton(
                    onClick = onChangeStatus,
                    enabled = !saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = FieldShape,
                    border = BorderStroke(
                        1.dp,
                        if (active) RedBorder else Green.copy(alpha = 0.45f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = if (active) Red else GreenDark
                    )
                ) {
                    Text(
                        text = if (active) "Deactivate User" else "Activate User",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            item {
                Text(
                    text = "You cannot change your own account's role or status here.",
                    color = Muted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = Ink
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = Muted,
            fontSize = 13.sp,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = value.ifBlank { "—" },
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun UserForm(
    editing: ManagedUser?,
    saving: Boolean,
    onBack: () -> Unit,
    onSave: (UserDraft) -> Unit
) {
    var name by rememberSaveable(editing?.id) {
        mutableStateOf(editing?.fullName.orEmpty())
    }
    var username by rememberSaveable(editing?.id) {
        mutableStateOf(editing?.username.orEmpty())
    }
    var email by rememberSaveable(editing?.id) {
        mutableStateOf(editing?.email.orEmpty())
    }
    var role by rememberSaveable(editing?.id) {
        mutableStateOf(editing?.role ?: "STAFF")
    }

    val isEditing = editing != null
    val emailValid = isEditing || EMAIL_REGEX.matches(email.trim())
    val valid = name.isNotBlank() && username.isNotBlank() && emailValid

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeading(
                title = if (isEditing) "Edit User" else "Add New User",
                caption = if (isEditing) {
                    "Update profile details and assigned access role."
                } else {
                    "An invitation email will be sent so the user can set up their password."
                },
                onBack = onBack,
                backEnabled = !saving
            )
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full name *") },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = FieldShape,
                colors = userFieldColors()
            )
        }

        item {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username *") },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = FieldShape,
                colors = userFieldColors()
            )
        }

        if (!isEditing) {
            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email address *") },
                    enabled = !saving,
                    isError = email.isNotBlank() && !emailValid,
                    supportingText = {
                        if (email.isNotBlank() && !emailValid) {
                            Text("Enter a valid email address.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = FieldShape,
                    colors = userFieldColors()
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "USER ROLE",
                    color = Muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )

                val roleMap = listOf("STAFF" to "Staff", "ADMIN" to "Administrator")
                val selectedLabel = roleMap.firstOrNull { it.first == role }?.second ?: "Staff"
                UserSegmentedTabs(
                    options = roleMap.map { it.second },
                    selected = selectedLabel,
                    onSelect = { label ->
                        if (!saving) {
                            role = roleMap.first { it.second == label }.first
                        }
                    }
                )
            }
        }

        item {
            Button(
                enabled = valid && !saving,
                onClick = {
                    onSave(
                        UserDraft(
                            fullName = name.trim(),
                            username = username.trim(),
                            email = email.trim(),
                            role = role
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = FieldShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Navy,
                    contentColor = Color.White
                )
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isEditing) "Update User" else "Send Invitation",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item {
            TextButton(
                onClick = onBack,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", color = Muted, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ScreenHeading(
    title: String,
    onBack: () -> Unit,
    caption: String? = null,
    backEnabled: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(
            onClick = onBack,
            enabled = backEnabled,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.heightIn(min = 36.dp)
        ) {
            Text(
                text = "‹ Back",
                color = if (backEnabled) Navy else Muted,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = title,
            color = Ink,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        if (!caption.isNullOrBlank()) {
            Text(
                text = caption,
                color = Muted,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun userFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Navy,
    unfocusedBorderColor = CardBorder,
    errorBorderColor = Red,
    focusedLabelColor = Navy,
    unfocusedLabelColor = Muted,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Background,
    cursorColor = Navy
)
