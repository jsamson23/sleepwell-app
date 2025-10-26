# Android App Specification: SleepWell

## 1. Overview

This document outlines the specifications for an Android application named "SleepWell" The app's primary purpose is to help users establish a focused morning routine by combining a standard alarm clock with a temporary, user-configurable lockout for selected applications. It also includes an optional feature to begin this lockout period the night before.

The core idea is that after the morning alarm is dismissed, the user is prevented from accessing distracting apps for a pre-defined duration, encouraging more productive activities.

## 2. Core Features

### 2.1. Morning Alarm & Lockout

-   **Set Alarm Time:** Users must be able to set a specific time for their morning alarm. This should use a standard Android time picker interface.
    
-   **Set Lockout Duration:** Users must be able to specify a duration (e.g., in minutes or hours) for which selected apps will be locked _after_ the alarm time.
    
-   **Alarm Functionality:**
    
    -   The alarm must sound at the designated time, even if the app is not in the foreground or the device is in Doze mode.
        
    -   The user must be able to dismiss or snooze the alarm from the lock screen or a full-screen notification.
        
-   **Lockout Activation:** The app lockout period begins immediately after the alarm time is reached. Dismissing the alarm does not end the lockout early.
    
-   **Master Toggle:** A main switch on the dashboard should allow the user to easily enable or disable the entire alarm and lockout system.
    

### 2.2. App Selection

-   **App List:** The application must be able to fetch and display a list of all user-installed applications on the device. System apps should be excluded by default.
    
-   **Selection UI:** The list should be user-friendly, displaying each app's icon and name. A checkbox or toggle next to each app should allow the user to include it in the lockout list.
    
-   **Persistence:** The user's selection of apps to be locked must be saved and persist across app restarts and device reboots.
    
    

### 2.3. Lock Screen Overlay

-   **Blocking Mechanism:** When a user attempts to open a locked application, a non-intrusive but firm overlay must be displayed, preventing interaction with the underlying app.
    
-   **Overlay Content:** This screen should clearly state:
    
    -   That the app is locked by "SleepWell"
        
    -   The exact time and date when the app will be unlocked.
        
-   **No Bypass:** The overlay should not be easily dismissible by the user (e.g., the back button should not close it). It should only disappear automatically when the lockout period ends.
    

### 2.4. Onboarding and Permissions

-   **Initial Setup:** On first launch, the app should guide the user through a brief onboarding process explaining its purpose.
    
-   **Permission Requests:** The app must clearly explain why it needs specific, sensitive permissions and guide the user on how to grant them. The required permissions include:
    
    -   **Usage Stats:** To detect which app is currently in the foreground.
        
    -   **Display Over Other Apps / System Alert Window:** To show the lock screen overlay.
        
    -   **Schedule Exact Alarms:** To ensure the alarm triggers precisely on time.
        
    -   **Post Notifications:** To display alarm notifications.
        

## 3. User Interface (UI) and User Experience (UX) Flow

### 3.1. Main Screen (Dashboard)

-   **Status Display:** Shows the currently set alarm time and total lockout duration (e.g., "Alarm at 7:00 AM, Unlocked at 7:30 AM").
    
-   **Main Toggle:** A prominent `On/Off` switch to activate or deactivate the service.
    
-   **Navigation:** A clear button or icon to navigate to the `Settings Screen`.
    

### 3.2. Settings Screen

This screen will be the main configuration hub.

-   **Alarm Time:** A clickable element that opens a time picker.
    
-   **Lockout Duration:** A clickable element that opens a dialog to select minutes/hours.
    
-   **App Selection:** A menu item that navigates to the `App Selection Screen`.
    

### 3.3. App Selection Screen

-   **Layout:** A `RecyclerView` or `LazyColumn` displaying a scrollable list of installed applications.
    
-   **Components:** Each list item will contain the App Icon, App Name, and a Checkbox.
    
-   **Functionality:** Tapping the checkbox selects/deselects the app for lockout. A "Save" button will persist the changes.
    

