package com.csiss.inventory.ui.supplier

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SupplierForm(
    supplier: Supplier?,
    back: () -> Unit,
    save: (String, String, String, String, String) -> Unit,
    delete: (() -> Unit)? = null
) {
    var name by rememberSaveable(supplier?.id) { mutableStateOf(supplier?.name.orEmpty()) }
    var contactPerson by rememberSaveable(supplier?.id) { mutableStateOf(supplier?.contactPerson.orEmpty()) }
    var phone by rememberSaveable(supplier?.id) { mutableStateOf(supplier?.phone.orEmpty()) }
    var email by rememberSaveable(supplier?.id) { mutableStateOf(supplier?.email.orEmpty()) }
    var address by rememberSaveable(supplier?.id) { mutableStateOf(supplier?.address.orEmpty()) }

    var nameError by rememberSaveable { mutableStateOf("") }
    var phoneError by rememberSaveable { mutableStateOf("") }
    var emailError by rememberSaveable { mutableStateOf("") }

    val isEdit = supplier != null

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Title
        item {
            SupplierScreenTitle(
                title = if (isEdit) "Edit supplier" else "Add supplier",
                caption = "Complete the required information.",
                back = back
            )
        }

        // Supplier Company Name *
        item {
            SupplierField(
                value = name,
                onChange = {
                    name = it
                    nameError = ""
                },
                label = "Supplier company name *",
                isError = nameError.isNotEmpty(),
                errorMessage = nameError
            )
        }

        // Contact Person
        item {
            SupplierField(
                value = contactPerson,
                onChange = { contactPerson = it },
                label = "Contact person"
            )
        }

        // Phone Number *
        item {
            SupplierField(
                value = phone,
                onChange = {
                    phone = it
                    phoneError = ""
                },
                label = "Phone number * (e.g. 012-3456789)",
                keyboard = KeyboardType.Phone,
                isError = phoneError.isNotEmpty(),
                errorMessage = phoneError
            )
        }

        // Email Address *
        item {
            SupplierField(
                value = email,
                onChange = {
                    email = it
                    emailError = ""
                },
                label = "Email address * (must have @)",
                keyboard = KeyboardType.Email,
                isError = emailError.isNotEmpty(),
                errorMessage = emailError
            )
        }

        // Address
        item {
            SupplierField(
                value = address,
                onChange = { address = it },
                label = "Address",
                singleLine = false,
                minLines = 3
            )
        }

        // Save / Update Button
        item {
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                label = if (isEdit) "Update" else "Save",
                onClick = {
                    var isValid = true

                    // 1. Name validation
                    if (name.trim().isBlank()) {
                        nameError = "Supplier company name is required"
                        isValid = false
                    }

                    // 2. Phone validation
                    val trimmedPhone = phone.trim()
                    val digits = trimmedPhone.filter { it.isDigit() }
                    if (trimmedPhone.isBlank()) {
                        phoneError = "Phone number is required"
                        isValid = false
                    } else if (!trimmedPhone.startsWith("01") && !digits.startsWith("01") && !digits.startsWith("601")) {
                        phoneError = "Phone number must start with 01 (e.g. 012-3456789)"
                        isValid = false
                    } else if (digits.length < 9 || digits.length > 12) {
                        phoneError = "Please enter a valid phone number (e.g. 012-3456789)"
                        isValid = false
                    }

                    // 3. Email validation
                    val trimmedEmail = email.trim()
                    if (trimmedEmail.isBlank()) {
                        emailError = "Email address is required"
                        isValid = false
                    } else if (!trimmedEmail.contains("@")) {
                        emailError = "Email address must contain '@' (e.g. name@example.com)"
                        isValid = false
                    } else if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                        emailError = "Please enter a valid email address (e.g. name@example.com)"
                        isValid = false
                    }

                    if (isValid) {
                        save(name.trim(), contactPerson.trim(), trimmedPhone, trimmedEmail, address.trim())
                    }
                }
            )
        }

        // Delete button if in edit mode (optional)
        if (isEdit && delete != null) {
            item {
                OutlinedDeleteButton(
                    label = "Delete",
                    onClick = delete
                )
            }
        }

        // Cancel Button
        item {
            TextButton(
                onClick = back,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cancel",
                    color = Muted,
                    fontSize = 14.sp
                )
            }
        }
    }
}
