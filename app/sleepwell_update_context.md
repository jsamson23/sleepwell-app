# Project Context: SleepWell / Digital Wellbeing App

## 1. Technical Architecture
* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose (Material 3). No XML layouts.
* **Architecture:** MVVM (Model-View-ViewModel).
    * ViewModels use `StateFlow` to expose state to Composables.
    * `viewModelScope` used for Coroutines.
* **Dependency Injection:** Manual Injection.
    * No Dagger/Hilt.
    * ViewModels are instantiated with factories or directly in the Composition root using `private val repository = SleepWellRepository(application)`.
* **Data Persistence:** Jetpack DataStore (Preferences).
    * Implementation: `PreferencesManager.kt`
    * Stores: `AlarmSettings`, `AppLockState` (lock times, selected apps, etc.).
    * No Room Database.

## 2. Existing Feature Implementation

### Alarm & Time Selection
* **Current State:** Uses an analog clock UI or standard native pickers.
* **Target State:** Needs a custom "Wheel/Tumbler" style picker for Hour, Minute, and AM/PM.

### Lockout Duration
* **Current State:** Defined in `SettingsScreen.kt` using a `LazyColumn` of `RadioButtons`.
    * Current options are hardcoded intervals (15, 30, 45... 240 min).
* **Target State:** Needs to be a scrollable "Wheel Picker" allowing granular selection (Hours and Minutes).
    * Default: 10 minutes.
    * Range: 1 minute to 23 hours 59 minutes.

### Lockout Mechanism
* **Implementation:** `AppBlockOverlayActivity.kt`
* **Type:** `ComponentActivity` with Compose UI.
* **Manifest Config:** `launchMode="singleInstance"`.
* **Behavior:**
    * Flags: `FLAG_SHOW_WHEN_LOCKED`, `FLAG_TURN_SCREEN_ON`.
    * Back Button: Disabled/Overridden.
    * Trigger: `AppMonitoringService` launches this activity when a restricted app is detected.

## 3. New Feature Requirements: Bypass Logic

The app requires a new "Bypass" system to allow users to temporarily unlock an app if they complete a challenge.

### Configuration (Settings)
* User selects bypass type during setup:
    1.  **Math Challenge:** Simple arithmetic (e.g., "14 + 7 = ?").
    2.  **String Match:** Random 8-char alphanumeric string (e.g., "7k9LmP3q").
    3.  **Strict Mode:** No bypass allowed.

### Runtime Behavior (`AppBlockOverlayActivity`)
* The Overlay screen should display a "Unlock / Bypass" button.
* Clicking the button presents the configured challenge (Math or String) in a Dialog or BottomSheet.
* **Validation:**
    * Success: The Activity calls `finish()`, returning the user to the underlying app.
    * Failure: Show error, generate a new problem.