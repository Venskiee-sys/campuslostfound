package com.example.campuslostfound

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuslostfound.model.Item
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ItemViewModel : ViewModel() {
    private val repository = ItemRepository()
    private val imageRepo = ImageRepository()

    private val _lostItems = MutableStateFlow<List<Item>>(emptyList())
    val lostItems: StateFlow<List<Item>> = _lostItems

    private val _foundItems = MutableStateFlow<List<Item>>(emptyList())
    val foundItems: StateFlow<List<Item>> = _foundItems

    private val _myReports = MutableStateFlow<List<Item>>(emptyList())
    val myReports: StateFlow<List<Item>> = _myReports

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    /**
     * True once the feeds have produced their first result. Screens need it to
     * tell "still loading" apart from "there is genuinely nothing here" — the
     * lists start empty, so an empty list on its own is ambiguous.
     */
    private val _itemsLoaded = mutableStateOf(false)
    val itemsLoaded: State<Boolean> = _itemsLoaded

    // One collector per feed: every refresh cancels the previous collector
    // instead of stacking another realtime channel on top of it.
    private var lostJob: Job? = null
    private var foundJob: Job? = null
    private var myReportsJob: Job? = null

    init {
        refreshItems()
    }

    fun refreshItems() {
        fetchLostItems()
        fetchFoundItems()
    }

    private fun fetchLostItems() {
        lostJob?.cancel()
        lostJob = viewModelScope.launch {
            repository.getItems("Lost").collectLatest { items ->
                _lostItems.value = items
                _itemsLoaded.value = true
            }
        }
    }

    private fun fetchFoundItems() {
        foundJob?.cancel()
        foundJob = viewModelScope.launch {
            repository.getItems("Found").collectLatest { items ->
                _foundItems.value = items
                _itemsLoaded.value = true
            }
        }
    }

    fun fetchMyReports(userId: String) {
        myReportsJob?.cancel()
        myReportsJob = viewModelScope.launch {
            repository.getMyReports(userId).collectLatest { items ->
                _myReports.value = items
            }
        }
    }

    fun reportItem(
        name: String,
        category: String,
        location: String,
        date: String,
        description: String,
        type: String,
        userId: String,
        userName: String,
        imageUri: Uri? = null,
        context: Context? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            var finalImageUrl: String? = null

            if (imageUri != null && context != null) {
                finalImageUrl = imageRepo.uploadImage(context, imageUri)
                if (finalImageUrl == null) {
                    // Report the failure instead of navigating away: the warning
                    // would otherwise be set and immediately thrown away by
                    // onSuccess(), leaving the user with a photo-less report and
                    // no idea why. They can drop the photo or retry.
                    _errorMessage.value =
                        "Couldn't upload the photo. Remove it or try again."
                    _isLoading.value = false
                    return@launch
                }
            }
            
            val item = Item(
                name = name,
                category = category,
                location = location,
                date = date,
                description = description,
                type = type,
                reportedBy = userId,
                reporterName = userName,
                imageUrl = finalImageUrl,
                timestamp = System.currentTimeMillis()
            )

            val result = repository.reportItem(item)
            _isLoading.value = false
            
            if (result.isSuccess) {
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to report item"
            }
        }
    }

    fun getItemById(itemId: String): Item? {
        return _lostItems.value.find { it.id == itemId }
            ?: _foundItems.value.find { it.id == itemId }
            ?: _myReports.value.find { it.id == itemId }
    }

    private val _selectedItem = MutableStateFlow<Item?>(null)
    /**
     * The item loaded on demand for the detail / claim screens. The three feed
     * lists above only ever contain *open* items the user happened to load, so a
     * deep link (or a resolved item) can be missing from them entirely.
     */
    val selectedItem: StateFlow<Item?> = _selectedItem

    /**
     * True while [fetchItem] is in flight. Without it a screen cannot tell
     * "the fetch has not come back yet" apart from "this item does not exist"
     * and would flash a not-found state at the user.
     */
    private val _itemLoading = mutableStateOf(false)
    val itemLoading: State<Boolean> = _itemLoading

    private var selectedItemJob: Job? = null

    fun fetchItem(itemId: String) {
        if (itemId.isBlank()) return
        if (selectedItemJob?.isActive == true && _selectedItem.value?.id == itemId) return
        selectedItemJob?.cancel()
        selectedItemJob = viewModelScope.launch {
            if (_selectedItem.value?.id != itemId) _selectedItem.value = null
            _itemLoading.value = true
            try {
                // Serve the cached copy immediately so the screen is never blank
                // while the network request runs.
                val cached = getItemById(itemId)
                if (cached != null) _selectedItem.value = cached
                repository.getItem(itemId)
                    .onSuccess { fresh -> _selectedItem.value = fresh ?: cached ?: _selectedItem.value }
                    .onFailure { Log.e(TAG, "Failed to load item $itemId", it) }
            } finally {
                _itemLoading.value = false
            }
        }
    }

    fun resolveItem(itemId: String, onSuccess: () -> Unit, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.resolveItem(itemId)
            if (result.isSuccess) {
                onSuccess()
            } else {
                val message = result.exceptionOrNull()?.message ?: "Failed to resolve the item"
                Log.e(TAG, "resolveItem($itemId) failed", result.exceptionOrNull())
                _errorMessage.value = message
                onError(message)
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Cancels every feed subscription and empties the caches. Called on
     * sign-out so the next account can never see the previous account's items
     * (the collectors close over the old user id), then re-subscribes to the
     * public feeds for the guest/new session.
     */
    fun clear() {
        lostJob?.cancel()
        foundJob?.cancel()
        myReportsJob?.cancel()
        lostJob = null
        foundJob = null
        myReportsJob = null
        _lostItems.value = emptyList()
        _foundItems.value = emptyList()
        _myReports.value = emptyList()
        _selectedItem.value = null
        selectedItemJob?.cancel()
        selectedItemJob = null
        _itemLoading.value = false
        _itemsLoaded.value = false
        _isLoading.value = false
        _errorMessage.value = null
        refreshItems()
    }

    companion object {
        private const val TAG = "ItemViewModel"
    }
}
