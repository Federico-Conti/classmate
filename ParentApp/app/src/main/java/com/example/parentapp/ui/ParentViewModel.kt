package com.example.parentapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.parentapp.data.ParentRepository
import com.example.parentapp.data.AttendanceMqttClient
import com.example.parentapp.data.ConfirmedAttendance
import com.example.parentapp.model.Child
import com.example.parentapp.model.Parent
import com.example.parentapp.model.HistoryState
import com.example.parentapp.model.PresenceStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ParentViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ParentRepository(app.applicationContext)
    private val mqtt = AttendanceMqttClient { event ->
        if (_selectedChildId.value == event.childId && _confirmedAttendance.value?.eventId != event.eventId) {
            _confirmedAttendance.value = event
        }
    }
    val realtimeConfigurationError: String? get() = mqtt.configurationError

    // Navigation start destination (Login/Home)
    private val _startDestination = MutableStateFlow("login")
    val startDestination: StateFlow<String> = _startDestination.asStateFlow()

    // Login
    val parentIdInput = MutableStateFlow("")
    private val _loginUiState = MutableStateFlow<UiState<Parent>>(UiState.Idle)
    val loginUiState: StateFlow<UiState<Parent>> = _loginUiState.asStateFlow()

    private val _parent = MutableStateFlow<Parent?>(null)
    val parent: StateFlow<Parent?> = _parent.asStateFlow()

    // Children
    private val _childrenUiState = MutableStateFlow<UiState<List<Child>>>(UiState.Idle)
    val childrenUiState: StateFlow<UiState<List<Child>>> = _childrenUiState.asStateFlow()

    private val _selectedChildId = MutableStateFlow<String?>(null)
    val selectedChildId: StateFlow<String?> = _selectedChildId.asStateFlow()

    val selectedChild: StateFlow<Child?> =
        combine(_childrenUiState, _selectedChildId) { state, id ->
            val list = (state as? UiState.Data)?.value.orEmpty()
            list.firstOrNull { it.id == id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _confirmedAttendance = MutableStateFlow<ConfirmedAttendance?>(null)
    val confirmedAttendance: StateFlow<ConfirmedAttendance?> = _confirmedAttendance.asStateFlow()
    val childPresenceStatus: StateFlow<PresenceStatus> =
        _confirmedAttendance.map { event ->
            when (event?.type) {
                "entry" -> PresenceStatus.AtSchool
                "exit" -> PresenceStatus.NotAtSchool
                else -> PresenceStatus.NoRecords
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, PresenceStatus.NoRecords)

    fun startRealtime() = mqtt.start()
    fun stopRealtime() = mqtt.stop()

    override fun onCleared() {
        mqtt.close()
        super.onCleared()
    }

    // ---- History Paging ----
    private val _historyUiState = MutableStateFlow<HistoryState>(HistoryState())
    val historyUiState: StateFlow<HistoryState> = _historyUiState.asStateFlow()

    private var historyCursor: com.google.firebase.firestore.DocumentSnapshot? = null

    init {
        // If DataStore has parentId => auto login
        viewModelScope.launch {
            repo.parentIdFlow.collect { saved ->
                if (!saved.isNullOrBlank()) {
                    _startDestination.value = "home"
                    parentIdInput.value = saved
                    if (_parent.value == null && _childrenUiState.value !is UiState.Loading) {
                        loadParentAndChildren(saved)
                    }
                } else {
                    _startDestination.value = "login"
                }
            }
        }
    }

    fun onParentIdChange(newValue: String) {
        parentIdInput.value = newValue.trim()
    }

    fun login() {
        val id = parentIdInput.value.trim()
        if (id.isBlank()) {
            _loginUiState.value = UiState.Error("Code is required")
            return
        }
        viewModelScope.launch {
            _loginUiState.value = UiState.Loading
            val parent = runCatching { repo.getParent(id) }.getOrElse { error ->
                _loginUiState.value = UiState.Error(error.message ?: "Connection failed")
                return@launch
            }
            if (parent == null) {
                _loginUiState.value = UiState.Error("Code not found")
                return@launch
            }
            _parent.value = parent
            repo.saveParentId(id)
            _loginUiState.value = UiState.Data(parent)

            loadChildren(id)
            _startDestination.value = "home"
        }
    }

    private fun loadParentAndChildren(parentId: String) {
        viewModelScope.launch {
            val parent = runCatching { repo.getParent(parentId) }.getOrElse { error ->
                _childrenUiState.value = UiState.Error(error.message ?: "Connection failed")
                return@launch
            }
            if (repo.parentIdFlow.first() != parentId) return@launch
            if (parent != null) {
                _parent.value = parent
                loadChildren(parentId)
            } else {
                // If the parent no longer exists, clean
                repo.clearParentId()
                _parent.value = null
                _selectedChildId.value = null
                _confirmedAttendance.value = null
                mqtt.selectChild(null)
                _childrenUiState.value = UiState.Idle
                _startDestination.value = "login"
            }
        }
    }

    private fun loadChildren(code: String) {
        viewModelScope.launch {
            _childrenUiState.value = UiState.Loading
            runCatching { repo.getChildren(code) to repo.selectedChildIdFlow.first() }
                .onSuccess { (list, saved) ->
                    if (_parent.value == null || parentIdInput.value != code) return@onSuccess
                    _childrenUiState.value = UiState.Data(list)
                    list.firstOrNull { it.id == saved }?.id?.let(::selectChild)
                        ?: list.firstOrNull()?.id?.let(::selectChild)
                }
                .onFailure { e ->
                    _childrenUiState.value = UiState.Error(e.message ?: "Failed to load children")
                }
        }
    }

    fun selectChild(childId: String) {
        val children = (_childrenUiState.value as? UiState.Data)?.value.orEmpty()
        if (children.none { it.id == childId } || _selectedChildId.value == childId) return
        _selectedChildId.value = childId
        _confirmedAttendance.value = null
        resetHistory()
        mqtt.selectChild(childId)
        viewModelScope.launch { repo.saveSelectedChildId(childId) }
    }

    fun logout() {
        viewModelScope.launch {
            // FIRST, I reset the selected child
            _selectedChildId.value = null
            _confirmedAttendance.value = null
            mqtt.selectChild(null)

            repo.clearParentId()
            _parent.value = null
            parentIdInput.value = ""
            _loginUiState.value = UiState.Idle
            _childrenUiState.value = UiState.Idle
            resetHistory()
            _startDestination.value = "login"
        }
    }

    // History

    fun refreshHistory(childId: String) {
        viewModelScope.launch {
            _historyUiState.update { it.copy(isRefreshing = true, error = null) }
            historyCursor = null

            runCatching { repo.getAttendancePage(childId, pageSize = 20, startAfterSnapshot = null) }
                .onSuccess { page ->
                    historyCursor = page.nextCursor
                    _historyUiState.value = HistoryState(
                        items = page.records,
                        isRefreshing = false,
                        isLoadingMore = false,
                        endReached = page.records.isEmpty(),
                        error = null
                    )
                }
                .onFailure { e ->
                    _historyUiState.update { it.copy(isRefreshing = false, error = e.message ?: "History error") }
                }
        }
    }

    fun loadMoreHistory(childId: String) {
        val state = _historyUiState.value
        if (state.isLoadingMore || state.endReached || state.isRefreshing) return

        viewModelScope.launch {
            _historyUiState.update { it.copy(isLoadingMore = true, error = null) }

            val cursor = historyCursor
            runCatching { repo.getAttendancePage(childId, pageSize = 20, startAfterSnapshot = cursor) }
                .onSuccess { page ->
                    historyCursor = page.nextCursor
                    _historyUiState.update { oldState ->
                        val existingKeys = oldState.items.map { it.id }.toSet()
                        val filteredNewRecords = page.records.filter { it.id !in existingKeys }
                        val newItems = oldState.items + filteredNewRecords

                        oldState.copy(
                            items = newItems,
                            isLoadingMore = false,
                            endReached = page.records.size < 20,
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    _historyUiState.update { it.copy(isLoadingMore = false, error = e.message ?: "Load more error") }
                }
        }
    }



    private fun resetHistory() {
        historyCursor = null
        _historyUiState.value = HistoryState()
    }
}

