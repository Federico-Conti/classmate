package com.example.parentapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.parentapp.data.ParentRepository
import com.example.parentapp.model.AttendanceRecord
import com.example.parentapp.model.Child
import com.example.parentapp.model.Parent
import com.example.parentapp.model.HistoryState
import com.example.parentapp.model.PresenceStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ParentViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ParentRepository(app.applicationContext)

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

    @OptIn(ExperimentalCoroutinesApi::class)
    val last5Records: StateFlow<UiState<List<AttendanceRecord>>> =
        _selectedChildId
            //flatMapLatest ensures listening to the last selected ID on _selectedChildId.
            .flatMapLatest { childId ->
                if (childId == null) {
                    // If no child is selected (logout), to Idle.
                    flowOf(UiState.Idle) //return a Flow type
                } else {
                    repo.listenLastAttendance(childId, limit = 5) // return a Flow type
                        .map<List<AttendanceRecord>, UiState<List<AttendanceRecord>>> {
                            UiState.Data(it)
                        }
                        .onStart { emit(UiState.Loading) } //is issued loading state before query the database
                        .catch { e -> emit(UiState.Error(e.message ?: "Realtime listener error")) }
                }
            }
            .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = UiState.Idle
            )
    val childPresenceStatus: StateFlow<PresenceStatus> =
        last5Records.map { state ->
            val records = (state as? UiState.Data)?.value
            val last = records?.firstOrNull()
            when {
                last == null -> PresenceStatus.NoRecords
                last.isEntry -> PresenceStatus.AtSchool
                else -> PresenceStatus.NotAtSchool
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PresenceStatus.NoRecords)

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
                    if (_parent.value == null) {
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
            _loginUiState.value = UiState.Error("username is required")
            return
        }
        viewModelScope.launch {
            _loginUiState.value = UiState.Loading
            val parent = repo.getParent(id)
            if (parent == null) {
                _loginUiState.value = UiState.Error("username not found")
                return@launch
            }
            repo.saveParentId(id)
            _parent.value = parent
            _loginUiState.value = UiState.Data(parent)

            loadChildren(parent.childrens)
            _startDestination.value = "home"
        }
    }

    private fun loadParentAndChildren(parentId: String) {
        viewModelScope.launch {
            val parent = repo.getParent(parentId)
            if (parent != null) {
                _parent.value = parent
                loadChildren(parent.childrens)
            } else {
                // If the parent no longer exists, clean
                repo.clearParentId()
                _parent.value = null
                _selectedChildId.value = null
                _childrenUiState.value = UiState.Idle
                _startDestination.value = "login"
            }
        }
    }

    private fun loadChildren(childIds: List<String>) {
        viewModelScope.launch {
            _childrenUiState.value = UiState.Loading
            runCatching { repo.getChildren(childIds) }
                .onSuccess { list ->
                    _childrenUiState.value = UiState.Data(list)
                    if (list.isNotEmpty() && _selectedChildId.value == null) {
                        _selectedChildId.value = list.first().id
                    }
                }
                .onFailure { e ->
                    _childrenUiState.value = UiState.Error(e.message ?: "Failed to load children")
                }
        }
    }

    fun selectChild(childId: String) {
        _selectedChildId.value = childId
        // Reset history quando cambio child
        resetHistory()
    }

    fun logout() {
        viewModelScope.launch {
            // FIRST, I reset the selected child
            _selectedChildId.value = null

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

