package com.csiss.inventory.data

import com.csiss.inventory.BuildConfig
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ManagedUser(
    @SerialName("user_id") val id: String,
    val username: String,
    @SerialName("full_name") val fullName: String,
    val email: String = "",
    val role: String,
    val status: String,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class UserDraft(
    @SerialName("full_name") val fullName: String,
    val username: String,
    val email: String = "",
    val role: String
)

@Serializable
private data class UserRequest(
    val action: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    val username: String? = null,
    val email: String? = null,
    val role: String? = null,
    val status: String? = null
)

@Serializable
private data class UserResponse(
    val users: List<ManagedUser> = emptyList(),
    val message: String? = null
)

class UserManagementRepository {

    companion object {
        // Shared singleton client prevents OkHttp thread/connection leaks per screen visit
        private val sharedHttp by lazy { HttpClient(OkHttp) }
        private val sharedJson = Json { ignoreUnknownKeys = true }
    }

    private suspend fun request(body: UserRequest): UserResponse = withContext(Dispatchers.IO) {
        val token = SupabaseClientProvider.client.auth
            .currentSessionOrNull()
            ?.accessToken
            ?: error("Please sign in again.")

        val response = sharedHttp.post(
            "${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/manage-users"
        ) {
            header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(sharedJson.encodeToString(body))
        }

        val text = response.bodyAsText()
        val result = runCatching {
            sharedJson.decodeFromString<UserResponse>(text)
        }.getOrNull()

        if (response.status.value !in 200..299) {
            error(result?.message ?: "User operation failed.")
        }

        result ?: error("Invalid server response.")
    }

    suspend fun loadUsers(): List<ManagedUser> =
        request(UserRequest(action = "list")).users

    suspend fun inviteUser(user: UserDraft) {
        request(
            UserRequest(
                action = "invite",
                fullName = user.fullName,
                username = user.username,
                email = user.email,
                role = user.role
            )
        )
    }

    suspend fun updateUser(id: String, user: UserDraft) {
        request(
            UserRequest(
                action = "update",
                userId = id,
                fullName = user.fullName,
                username = user.username,
                role = user.role
            )
        )
    }

    suspend fun changeStatus(id: String, status: String) {
        request(
            UserRequest(
                action = "status",
                userId = id,
                status = status
            )
        )
    }
}
