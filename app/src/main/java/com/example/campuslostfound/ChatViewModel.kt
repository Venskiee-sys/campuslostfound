package com.example.campuslostfound

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuslostfound.model.Chat
import com.example.campuslostfound.model.Message
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val repository = ChatRepository()

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _currentChatId = mutableStateOf<String?>(null)
    val currentChatId: State<String?> = _currentChatId

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    // Exactly one collector per stream: opening the chat list (or another chat)
    // again used to stack a new realtime channel on top of the old one, so two
    // conversations ended up writing into the same _messages flow.
    private var chatsJob: Job? = null
    private var messagesJob: Job? = null

    fun fetchChats(userId: String) {
        chatsJob?.cancel()
        chatsJob = viewModelScope.launch {
            repository.getChats(userId).collectLatest { chatList ->
                _chats.value = chatList
            }
        }
    }

    fun fetchMessages(chatId: String) {
        if (chatId.isBlank()) return
        // Drop the previous thread immediately so the new chat never opens
        // showing the old conversation's messages.
        if (_currentChatId.value != chatId) _messages.value = emptyList()
        _currentChatId.value = chatId
        _errorMessage.value = null
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.getMessages(chatId).collectLatest { messageList ->
                _messages.value = messageList
            }
        }
    }

    fun startChat(myId: String, myName: String, otherId: String, otherName: String, onReady: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val chatId = repository.getOrCreateChat(myId, myName, otherId, otherName)
                _currentChatId.value = chatId
                _errorMessage.value = null
                onReady(chatId)
            } catch (e: Exception) {
                Log.e(TAG, "startChat failed", e)
                _errorMessage.value = "Couldn't start the conversation. Please try again."
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Sends a message and reports whether it actually reached the server, so the
     * caller only clears the draft on success (and can show an error otherwise).
     */
    fun sendMessage(senderId: String, content: String, onResult: (Boolean) -> Unit = {}) {
        val chatId = _currentChatId.value
        if (chatId.isNullOrBlank() || content.isBlank()) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            try {
                repository.sendMessage(chatId, senderId, content)
                _errorMessage.value = null
                onResult(true)
            } catch (e: Exception) {
                // Uncaught here before: any network/RLS failure crashed the app.
                Log.e(TAG, "sendMessage failed", e)
                _errorMessage.value = "Message not sent. Please try again."
                onResult(false)
            }
        }
    }

    /** Surfaces a message to the UI without failing an operation. */
    fun notifyError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /** Wipes everything so a signed-out account's chats can't leak into the next one. */
    fun clear() {
        chatsJob?.cancel()
        messagesJob?.cancel()
        chatsJob = null
        messagesJob = null
        _chats.value = emptyList()
        _messages.value = emptyList()
        _currentChatId.value = null
        _isLoading.value = false
        _errorMessage.value = null
    }

    override fun onCleared() {
        chatsJob?.cancel()
        messagesJob?.cancel()
        // No super.onCleared(): the base implementation is @EmptySuper.
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}
