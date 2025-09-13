package ai.plusonelabs.assistant

import ai.plusonelabs.assistant.models.Assistant
import ai.plusonelabs.assistant.models.AssistantCreationParams
import ai.plusonelabs.network.NetworkClient
import ai.plusonelabs.network.NetworkError
import ai.plusonelabs.network.delete
import ai.plusonelabs.network.get
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantRepository @Inject constructor(
    private val networkClient: NetworkClient,
    private val moshi: Moshi,
) {

    private val _assistants = MutableStateFlow<List<Assistant>>(emptyList())
    val assistants: StateFlow<List<Assistant>> = _assistants.asStateFlow()

    suspend fun refreshAssistants() {
        getAssistants()
            .onSuccess { assistants ->
                _assistants.value = assistants
            }
    }
    suspend fun getAssistants(skip: Int = 0, limit: Int = 20): Result<List<Assistant>> = runCatching {
        val jsonResponse: String = networkClient.get("/assistants?skip=$skip&limit=$limit")

        val type = Types.newParameterizedType(List::class.java, Assistant::class.java)
        val adapter = moshi.adapter<List<Assistant>>(type)

        adapter.fromJson(jsonResponse)
            ?: throw NetworkError.ParseError("Failed to parse assistants")
    }

    suspend fun getAssistant(id: String): Result<Assistant> = runCatching {
        networkClient.get(
            endpoint = "/assistants/$id",
            responseType = Assistant::class.java,
        )
    }

    suspend fun createAssistant(params: AssistantCreationParams): Result<Assistant> = runCatching {
        networkClient.post(
            endpoint = "/assistants",
            body = mapOf(
                "name" to params.name,
                "description" to params.description,
                "instructions" to params.instructions,
                "model" to params.model,
                "tools" to params.tools,
                "metadata" to params.metadata,
            ).filterValues { it != null },
            responseType = Assistant::class.java,
        )
    }

    suspend fun updateAssistant(
        id: String,
        params: AssistantCreationParams,
    ): Result<Assistant> = runCatching {
        networkClient.put(
            endpoint = "/assistants/$id",
            body = mapOf(
                "name" to params.name,
                "description" to params.description,
                "instructions" to params.instructions,
                "model" to params.model,
                "tools" to params.tools,
                "metadata" to params.metadata,
            ).filterValues { it != null },
            responseType = Assistant::class.java,
        )
    }

    suspend fun deleteAssistant(id: String): Result<Unit> = runCatching {
        networkClient.delete(
            endpoint = "/assistants/$id",
        )
    }
}
