package com.example.campuslostfound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuslostfound.model.Notification
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {
    private val repository = NotificationRepository()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications

    // Single collector: every screen entry / pull-to-refresh used to start a new
    // permanent realtime subscription on top of the previous ones.
    private var job: Job? = null

    fun fetchNotifications(userId: String) {
        if (userId.isBlank()) return
        job?.cancel()
        job = viewModelScope.launch {
            repository.getNotifications(userId).collectLatest { list ->
                _notifications.value = list
            }
        }
    }

    fun markAsRead(id: String) {
        repository.markAsRead(id)
    }

    fun markAllAsRead(userId: String) {
        repository.markAllAsRead(userId)
    }

    fun markChatMessagesAsRead(userId: String, chatId: String) {
        repository.markMessagesAsRead(userId, chatId)
    }

    /** Drops the subscription and cached rows (used on sign-out). */
    fun clear() {
        job?.cancel()
        job = null
        _notifications.value = emptyList()
    }

    override fun onCleared() {
        job?.cancel()
        // No super.onCleared(): the base implementation is @EmptySuper.
    }
}
