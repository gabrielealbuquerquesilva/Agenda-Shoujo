package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.EventEntity
import com.example.data.network.GeminiClient
import com.example.data.network.GeminiContent
import com.example.data.network.GeminiPart
import com.example.data.network.GeminiRequest
import com.example.data.network.OAuthTokenManager
import com.example.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainViewModel(
    application: Application,
    private val repository: EventRepository,
    private val tokenManager: OAuthTokenManager
) : AndroidViewModel(application) {

    val events: StateFlow<List<EventEntity>> = repository.allEvents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isGoogleAuthorized = MutableStateFlow(tokenManager.isAuthorized())
    val isGoogleAuthorized: StateFlow<Boolean> = _isGoogleAuthorized.asStateFlow()

    private val _googleEmail = MutableStateFlow(tokenManager.getUserEmail())
    val googleEmail: StateFlow<String?> = _googleEmail.asStateFlow()

    private val _mascotGreeting = MutableStateFlow(
        "Olá, linda estrelinha! ✨ Sou Luna-chan, sua gatinha assistente mágica. " +
        "Pronta para afastar as sombras da procrastinação hoje? Nya!"
    )
    val mascotGreeting: StateFlow<String> = _mascotGreeting.asStateFlow()

    private val _isLoadingMascot = MutableStateFlow(false)
    val isLoadingMascot: StateFlow<Boolean> = _isLoadingMascot.asStateFlow()

    private val _syncingState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncingState: StateFlow<SyncState> = _syncingState.asStateFlow()

    sealed interface SyncState {
        object Idle : SyncState
        object Syncing : SyncState
        data class Success(val count: Int) : SyncState
        data class Error(val message: String) : SyncState
    }

    init {
        // Automatically ask the mascot for inspiration on start
        refreshMascotGreeting()
    }

    fun addLocalEvent(title: String, description: String, dateTime: Long, category: String) {
        viewModelScope.launch {
            repository.insertLocalEvent(title, description, dateTime, category)
            // Refresh prediction since task changed
            refreshMascotGreeting()
        }
    }

    fun toggleEventCompleted(event: EventEntity) {
        viewModelScope.launch {
            repository.toggleCompleted(event)
        }
    }

    fun deleteEvent(event: EventEntity) {
        viewModelScope.launch {
            repository.deleteEvent(event)
            refreshMascotGreeting()
        }
    }

    fun saveOAuthCredentials(accessToken: String, email: String?) {
        tokenManager.saveTokens(accessToken, null, email)
        _isGoogleAuthorized.value = true
        _googleEmail.value = email
        syncGoogleCalendar()
    }

    fun disconnectGoogle() {
        tokenManager.clear()
        _isGoogleAuthorized.value = false
        _googleEmail.value = null
        viewModelScope.launch {
            // Option to clear synced events as they are disconnect in Google Calendar
            AppDatabase.getDatabase(getApplication()).eventDao().clearGoogleEvents()
        }
    }

    fun syncGoogleCalendar() {
        if (!tokenManager.isAuthorized()) return
        viewModelScope.launch {
            _syncingState.value = SyncState.Syncing
            val result = repository.syncGoogleCalendar(tokenManager)
            result.onSuccess { list ->
                _syncingState.value = SyncState.Success(list.size)
                refreshMascotGreeting()
            }.onFailure { exception ->
                _syncingState.value = SyncState.Error(exception.message ?: "Erro desconhecido")
            }
        }
    }

    fun refreshMascotGreeting() {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            _mascotGreeting.value = "Pronto para organizar seus dias radiantes? Adicione algumas tarefas para que eu possa inspecioná-las com meus poderes estelares! ✨"
            return
        }

        viewModelScope.launch {
            _isLoadingMascot.value = true
            try {
                val currentEvents = events.value
                val eventsListStr = if (currentEvents.isEmpty()) {
                    "Nenhuma tarefa agendada. A agenda está livre e radiante!"
                } else {
                    currentEvents.take(8).joinToString("\n") { ev ->
                        val timeStr = Instant.ofEpochMilli(ev.dateTime)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
                        "- ${ev.title} [Horário: $timeStr] categoria: ${ev.category} completada: ${if (ev.isCompleted) "Sim" else "Não"}"
                    }
                }

                val systemInstructionText = """
                    Você é Luna-chan, uma gatinha mágica de xale lilás e marcas de lua crescente na testa, vinda de um anime clássico de garotas mágicas dos anos 90 (como Sailor Moon ou Cardcaptor Sakura). Você é uma companheira leal, fofa, otimista e dramática que adora chá de jasmim, doces de morango e ver sua dona organizada!
                    
                    Por favor, fale com a usuária em Português de forma fofa, encorajadora, dramática e mágica! Comente sobre as tarefas atuais que ela possui, faça previsões de sorte ou horóscopo da organização para o dia, dê um conselho alegre (por exemplo, sugerindo tomar um chá ou descansar se houver muitas tarefas, ou dando forças com purificações mágicas para vencer os 'vilões da procrastinação'). Use expressões fofas com toques de anime retro (como 'Nya!', '*suspira dramaticamente*', 'Poder do Prisma do Planejamento!'). Responda em até 3-4 frases curtas e confortantes. Seja bem animada e amorosa!
                """.trimIndent()

                val prompt = """
                    Aqui estão as tarefas agendadas para hoje do usuário:
                    $eventsListStr
                    
                    Fale algo amigável sobre o dia dela!
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                    ),
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstructionText)))
                )

                val response = GeminiClient.api.generateContent(apiKey, request)
                val responseText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!responseText.isNullOrBlank()) {
                    _mascotGreeting.value = responseText
                } else {
                    _mascotGreeting.value = "Uau! Sinto uma forte energia cósmica no seu dia hoje, estrelinha! Mantenha o foco e venceremos qualquer desafio! ✨ Nya!"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _mascotGreeting.value = "Estrelinha, as linhas estelares estão um pouco instáveis hoje, mas lembre-se: seu brilho próprio é infinito! O que temos para conquistar hoje? ✨"
            } finally {
                _isLoadingMascot.value = false
            }
        }
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val repository: EventRepository,
    private val tokenManager: OAuthTokenManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, repository, tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
