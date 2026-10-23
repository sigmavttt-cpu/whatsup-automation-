---
name: kotlin-coroutines-flow-expert
description: Deep expertise in Kotlin Coroutines, StateFlow, SharedFlow, Channels, structured concurrency, CoroutineScope lifecycle, Dispatcher discipline, and preventing memory leaks or CPU spin in Android. Trigger on "coroutines", "flow", "StateFlow", "SharedFlow", "Channel", "Dispatchers", "viewModelScope", "SupervisorJob", "تزامن", "كروتنين", "فلو", "تجميد الواجهة", "تسريب الذاكرة", "معالجة خلفية", "MainThread".
---

# Kotlin Coroutines & Reactive Flow Expert Guidelines

## 1. Overview & Core Mission
Provides strict engineering rules and battle-tested patterns for managing asynchronous operations, reactive data streams, and concurrency across Android Jetpack Compose and background services in WhatsUp Automation.

---

## 2. The 5 Immutable Concurrency Commandments

### I. Explicit Scope Ownership & Structured Concurrency
- **Rule:** Never use `GlobalScope.launch` or unbounded `CoroutineScope(Dispatchers.Default)`.
- **In ViewModels:** Use `viewModelScope`.
- **In Services:** Bind coroutines to the Service lifecycle:
  ```kotlin
  private val serviceJob = SupervisorJob()
  private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
  
  override fun onDestroy() {
      super.onDestroy()
      serviceJob.cancel() // Cancels all child coroutines cleanly
  }
  ```
- **SupervisorJob:** Always use `SupervisorJob()` for parent scopes so that the failure of one task (e.g. processing a single WhatsApp status) does NOT cancel sibling tasks or the entire foreground engine.

### II. Dispatcher Discipline
- **`Dispatchers.Main.immediate`:** UI updates and Compose state mutations. Always prefer `.immediate` to avoid re-dispatching if already on the main thread.
- **`Dispatchers.IO`:** All disk reads/writes, Room DB operations, file encryption, and socket communication.
- **`Dispatchers.Default`:** Heavy CPU calculations, JSON serialization/deserialization, cryptography (AES-GCM), and image/media processing.
- **Rule:** If a repository function performs blocking IO, it MUST switch dispatchers internally via `withContext(Dispatchers.IO)` so callers on the main thread are safe.

### III. StateFlow vs. SharedFlow vs. Channels
| Construct | Use Case | Replay Buffer | Behavior on Multiple Collectors |
| :--- | :--- | :---: | :--- |
| **`StateFlow`** | UI State representation (e.g., `UiState`, connection status) | `1` (Conflated) | Emits latest value to new subscribers immediately. Deduplicates equal consecutive values. |
| **`SharedFlow`** | Broadcast events, broadcast status updates | Configurable (`replay = 0`) | Multiple collectors receive events; non-stateful notifications. |
| **`Channel`** | One-shot transient events (Single-observer, e.g. Show Toast, Navigate, Haptic Feedback) | Buffered (`Channel.BUFFERED`) | Exactly ONE collector consumes each event. Prevents event duplication on screen rotation. |

#### One-Shot Event Pattern in MVI:
```kotlin
// In ViewModel:
private val _eventChannel = Channel<UiEvent>(Channel.BUFFERED)
val events = _eventChannel.receiveAsFlow()

// In Compose Screen:
LaunchedEffect(Unit) {
    viewModel.events.collectLatest { event ->
        when (event) {
            is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            is UiEvent.Navigate -> navController.navigate(event.route)
        }
    }
}
```

### IV. Never Swallow `CancellationException`
- **Fatal Anti-Pattern:**
  ```kotlin
  // ❌ BROKEN: Breaks coroutine cancellation and causes zombie threads
  try {
      delay(5000)
  } catch (e: Exception) {
      Log.e("Tag", "Error", e)
  }
  ```
- **Correct Pattern:**
  ```kotlin
  // ✅ SAFE: Rethrow CancellationException
  try {
      delay(5000)
  } catch (e: CancellationException) {
      throw e
  } catch (e: Exception) {
      Log.e("Tag", "Handled error", e)
  }
  ```

### V. StateIn Optimization in ViewModels
Convert cold repository flows to hot `StateFlow` with proper lifecycle sharing:
```kotlin
val uiState: StateFlow<DashboardUiState> = repository.observeServiceState()
    .map { state -> DashboardUiState.Success(state) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = DashboardUiState.Loading
    )
```
> `stopTimeoutMillis = 5_000` keeps the upstream flow alive for 5 seconds during configuration changes (like screen rotation) to avoid restarting cold flows.

---

## 3. High-Throughput Stream Processing (Node.js Bridge Events)
When consuming continuous events from the Baileys WebSocket bridge:
- Use `.flowOn(Dispatchers.IO)` before downstream UI collection.
- Use `.conflate()` if UI only cares about the newest telemetry status.
- Use `.buffer(capacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)` for fast-producing message streams to prevent memory bloat.
