package shop.devosify.riya.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import shop.devosify.riya.data.local.dao.ConversationWithMessages
import shop.devosify.riya.repository.ConversationRepository
import javax.inject.Inject

@HiltViewModel
class ConversationHistoryViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val conversations: StateFlow<List<ConversationWithMessages>> =
        conversationRepository.getConversationsWithMessages()
            .catch { e ->
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    init {
        viewModelScope.launch {
            conversations.collect { list ->
                _uiState.value = if (list.isEmpty()) {
                    UiState.Empty
                } else {
                    UiState.Success
                }
            }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        object Empty : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
} 