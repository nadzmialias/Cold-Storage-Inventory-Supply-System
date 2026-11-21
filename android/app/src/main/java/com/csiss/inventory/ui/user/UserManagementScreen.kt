
package com.csiss.inventory.ui.user

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csiss.inventory.data.ManagedUser
import com.csiss.inventory.data.UserDraft
import com.csiss.inventory.data.UserManagementRepository
import com.csiss.inventory.ui.components.OutlineRefreshButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextAlign

private val Navy = Color(0xFF132945)
private val Background = Color(0xFFF1F6FB)
private val Ink = Color(0xFF182B48)
private val Muted = Color(0xFF718198)
private val Red = Color(0xFFE45465)
private val Green = Color(0xFF059669)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    currentUserId: String
) {
    val repository = remember { UserManagementRepository() }
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<ManagedUser>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadedOnce by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var page by rememberSaveable { mutableStateOf("list") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var confirmStatus by remember { mutableStateOf(false) }
    val selected = users.firstOrNull { it.id == selectedId }
    val back: () -> Unit = {
        page = when (page) {
            "edit" -> "details"
            else -> "list"
        }
        error = null
    }

    BackHandler(enabled = page != "list", onBack = back)

    LaunchedEffect(reload) {
        loading = true
        error = null

        try {
            users = repository.loadUsers()
            loadedOnce = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Unable to load users."
        } finally {
            loading = false
        }
    }

    val refresh: () -> Unit = {
        if (!loading && !saving) reload++
    }

    val save: (UserDraft) -> Unit = { draft ->
        if (!saving) {
            scope.launch {
                saving = true
                error = null

                try {
                    if (page == "add") {
                        repository.inviteUser(draft)
                        page = "list"
                    } else {
                        repository.updateUser(
                            selectedId ?: throw IllegalStateException("No user selected."),
                            draft
                        )
                        page = "details"
                    }
                    reload++
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    error = e.message ?: "Unable to save user."
                } finally {
                    saving = false
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (saving) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Navy
                )
            }
            when (page) {
                "list" -> {
                    PullToRefreshBox(
                        isRefreshing = loading && loadedOnce,
                        onRefresh = refresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val filtered = users.filter { user ->
                            val matchesSearch =
                                user.fullName.contains(search, true) ||
                                        user.username.contains(search, true) ||
                                        user.email.contains(search, true)

                            val matchesStatus =
                                filter == "All" ||
                                        user.status.equals(filter, true)

                            matchesSearch && matchesStatus
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = 12.dp,
                                bottom = 20.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "ADMINISTRATION",
                                            color = Muted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 1.sp
                                        )

                                        Text(
                                            "User Management",
                                            color = Ink,
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    OutlineRefreshButton(
                                        onClick = refresh,
                                        isRefreshing = loading,
                                        enabled = !loading
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
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }

                            item {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("All", "Active", "Inactive")
                                        .forEach { option ->
                                            FilterChip(
                                                selected = filter == option,
                                                onClick = { filter = option },
                                                label = { Text(option) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Navy,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                }
                            }

                            item {
                                Button(
                                    onClick = {
                                        selectedId = null
                                        error = null
                                        page = "add"
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Navy
                                    )
                                ) {
                                    Text("+ Add New User")
                                }
                            }

                            item {
                                Text(
                                    "Users (${filtered.size})",
                                    color = Ink,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (loading && !loadedOnce) {
                                item {
                                    CircularProgressIndicator(color = Navy)
                                }
                            } else if (filtered.isEmpty()) {
                                item {
                                    Text(
                                        if (!loadedOnce) "No user data available."
                                        else "No users found.",
                                        color = Muted
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
                            onBack = back,
                            onEdit = {
                                error = null
                                page = "edit"
                            },
                            onChangeStatus = {
                                confirmStatus = true
                            }
                        )
                    }
                }

                "add", "edit" -> {
                    UserForm(
                        editing = if (page == "edit") selected else null,
                        saving = saving,
                        onBack = back,
                        onSave = save
                    )
                }
            }
        }
    }

    error?.let { message ->
        AlertDialog(
            onDismissRequest = {
                error = null
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = Color(0xFFFEE7E9),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "!",
                        color = Color(0xFFB42332),
                        fontSize = 26.sp,
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
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = message,
                    color = Muted,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        error = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Navy,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Got it",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 4.dp
        )
    }

    if (confirmStatus && selected != null) {
        val target = selected
        val newStatus =
            if (target.status == "ACTIVE") "INACTIVE" else "ACTIVE"

        AlertDialog(
            onDismissRequest = { confirmStatus = false },
            title = { Text("Change account status?") },
            text = {
                Text("Set ${target.fullName} to $newStatus?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmStatus = false
                        scope.launch {
                            saving = true
                            error = null
                            try {
                                repository.changeStatus(
                                    target.id,
                                    newStatus
                                )
                                reload++
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                error = e.message
                            } finally {
                                saving = false
                            }
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmStatus = false }) {
                    Text("Cancel")
                }
            }
        )



    }
}



@Composable
private fun UserCard(
    user: ManagedUser,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(18.dp)
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
                        .size(46.dp)
                        .background(
                            Color(0xFFE7EFF9),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        user.fullName.take(1).uppercase(),
                        color = Navy,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        user.fullName,
                        color = Ink,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        user.email.ifBlank { "@${user.username}" },
                        color = Muted,
                        fontSize = 12.sp
                    )
                }

                Text("›", color = Muted, fontSize = 24.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (user.role == "ADMIN") "Administrator" else "Staff",
                    color = Muted,
                    fontSize = 12.sp
                )

                Text(
                    if (user.status == "ACTIVE") "● Active"
                    else "● Inactive",
                    color = if (user.status == "ACTIVE") Green else Muted,
                    fontSize = 12.sp
                )
            }
        }
    }
}


@Composable
private fun UserDetails(
    user: ManagedUser,
    isCurrentUser: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onChangeStatus: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeading(
                "User Details",
                onBack
            )
        }

        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        user.fullName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )

                    Text("@${user.username}", color = Muted)

                    Text(
                        "${user.role} · ${user.status}",
                        color = if (user.status == "ACTIVE") Green else Muted
                    )
                }
            }
        }

        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailField("FULL NAME", user.fullName)
                    HorizontalDivider()
                    DetailField("EMAIL", user.email)
                    HorizontalDivider()
                    DetailField("USERNAME", user.username)
                    HorizontalDivider()
                    DetailField("ROLE", user.role)
                    HorizontalDivider()
                    DetailField("STATUS", user.status)
                    HorizontalDivider()
                    DetailField(
                        "CREATED",
                        user.createdAt.take(10)
                    )
                }
            }
        }

        if (!isCurrentUser) {
            item {
                Button(
                    onClick = onEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Navy
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Edit User")
                }
            }

            item {
                OutlinedButton(
                    onClick = onChangeStatus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        if (user.status == "ACTIVE")
                            "Deactivate User"
                        else
                            "Activate User",
                        color = if (user.status == "ACTIVE") Red else Green
                    )
                }
            }
        } else {
            item {
                Text(
                    "You cannot change your own account's role or status here.",
                    color = Muted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            label,
            color = Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            value.ifBlank { "—" },
            color = Ink,
            fontSize = 13.sp
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

    val valid = name.isNotBlank() &&
            username.isNotBlank() &&
            (isEditing || email.contains("@"))

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeading(
                if (isEditing) "Edit User" else "Add New User",
                onBack
            )
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        if (!isEditing) {
            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email address *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    singleLine = true
                )
            }
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "USER ROLE",
                    color = Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("STAFF", "ADMIN").forEach { option ->
                        FilterChip(
                            selected = role == option,
                            onClick = { role = option },
                            label = {
                                Text(
                                    if (option == "ADMIN")
                                        "Administrator"
                                    else
                                        "Staff"
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Navy,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        if (!isEditing) {
            item {
                Text(
                    "An invitation will be sent to the user's email to set up their password.",
                    color = Muted,
                    fontSize = 12.sp
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
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Navy
                )
            ) {
                Text(
                    if (isEditing) "Update User"
                    else "Send Invitation"
                )
            }
        }

        item {
            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", color = Muted)
            }
        }
    }
}

@Composable
private fun ScreenHeading(
    title: String,
    onBack: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("‹ Back", color = Navy)
        }
        Text(
            title,
            color = Ink,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

