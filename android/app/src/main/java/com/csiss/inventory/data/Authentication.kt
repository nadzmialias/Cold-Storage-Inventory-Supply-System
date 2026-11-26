package com.csiss.inventory.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CsissUser(
    @SerialName("user_id")
    val userId: String,

    val username: String,

    @SerialName("full_name")
    val fullName: String,

    val role: String,

    val status: String
)

class Authentication {

    companion object {
        const val AUTH_CALLBACK_URL = "com.csiss.inventory://auth/callback"
    }

    private val client
        get() = SupabaseClientProvider.client

    // LOGIN
    suspend fun signIn(
        email: String,
        password: String
    ) {
        client.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    // FORGOT PASSWORD
    suspend fun sendPasswordResetEmail(email: String) {
        client.auth.resetPasswordForEmail(
            email = email.trim().lowercase(),
            redirectUrl = AUTH_CALLBACK_URL
        )
    }

    // GET CURRENT USER PROFILE
    suspend fun getCurrentProfile(): CsissUser? {

        val userId = client.auth
            .currentUserOrNull()
            ?.id
            ?: return null

        return client.from("users")
            .select(
                columns = Columns.raw(
                    "user_id,username,full_name,role,status"
                )
            ) {
                filter {
                    eq("user_id", userId)
                }

                limit(1)
            }
            .decodeList<CsissUser>()
            .firstOrNull()
    }

    // LOGOUT
    suspend fun signOut() {
        client.auth.signOut()
    }
}