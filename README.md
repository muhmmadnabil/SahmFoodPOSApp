# Sahm Food POS

Sahm Food POS is a Kotlin Multiplatform point-of-sale application for cashier workflows. The current app foundation focuses on authentication, local user persistence, shared Android/iOS UI, and a clean architecture split that keeps business rules independent from platform and UI code.

The app is built so Android and iOS share the same Compose UI, navigation, dependency injection, domain use cases, repository contracts, and local database logic. Platform-specific code is kept only where the project needs native services such as Android Firebase, SQLDelight drivers, DataStore file creation, and iOS entry points.

## Technologies

- Kotlin Multiplatform for shared business, data, and UI code across Android and iOS.
- Compose Multiplatform and Material 3 for the shared UI.
- Compose Navigation for screen routing.
- Android adaptive window APIs for phone/tablet layout selection.
- Koin for dependency injection and ViewModel creation.
- Kotlin Coroutines and Flow for async work, state, and one-time UI effects.
- SQLDelight for the local users database.
- AndroidX DataStore Preferences for storing the current logged-in user.
- Firebase Firestore on Android for remote user sync.
- Ktor client dependencies for shared networking support.
- Coil for image loading support.
- Gradle version catalog for centralized dependency versions.
- kotlin.test and kotlinx-coroutines-test for unit tests.

## Architecture

The project follows a clean, modular architecture with MVI in the UI layer.

### MVI UI flow

The login feature is structured around three MVI pieces:

- State: `LoginUiState` contains all data needed to render the screen, including phone, password, loading state, field errors, password visibility, and general errors.
- Intent: `LoginIntent` describes user actions such as changing the phone, changing the password, toggling password visibility, and submitting login.
- Effect: `LoginEffect` represents one-time actions such as navigating to the POS home screen or showing a message.

`LoginScreen` renders only from state and sends user actions as intents. `LoginViewModel` receives those intents, validates input, updates the state, calls the domain layer through `LoginUseCase`, and emits effects for navigation or messages. This keeps UI rendering predictable and keeps business decisions out of composables.

### Clean architecture layers

The app is split into data, domain, and UI modules:

- `domain`: Contains business models, repository contracts, and use cases. It does not depend on Compose, Firebase, SQLDelight, or platform APIs.
- `data`: Implements repository contracts and owns data sources. It connects Firestore, SQLDelight, DataStore, mappers, and platform-specific database/storage setup.
- `composeApp`: Owns shared UI, navigation, ViewModels, dependency graph assembly, Android entry point, and iOS Compose entry point.

The dependency direction is:

```text
composeApp -> domain
composeApp -> data -> domain
```

The domain module defines what the app needs. The data module decides how those needs are fulfilled. The UI module consumes use cases and displays state.

## Modules

### `composeApp`

The shared application module contains:

- `App.kt`: Starts Koin and loads the app navigation host.
- `navigation`: Defines `AppRoute` and `AppNavHost`.
- `screens/login`: Login screen, contract, ViewModel, and screen components.
- `screens/home`: Current POS home placeholder.
- `components`: Reusable Compose components such as `PrimaryTextField`.
- `theme`: Shared colors and UI styling values.
- `utils`: Shared UI helpers such as `ScreenType`.
- `androidMain`: Android `MainActivity`, edge-to-edge setup, adaptive screen type detection, and Firebase configuration.
- `iosMain`: iOS `MainViewController` for rendering the shared Compose app.

### `domain`

The domain module contains:

- Entities: `User` and `CurrentUser`.
- Repository contract: `AuthRepo`.
- Login result model: `LoginResult`.
- Use cases:
  - `LoginUseCase`: Validates credentials, updates last login time, and saves the current user.
  - `SyncUsersUseCase`: Syncs remote users into local storage.
  - `HasUsersUseCase`: Checks whether local users exist.
  - `HasCurrentUserUseCase`: Checks whether a user is already logged in.
  - `CurrentTimestampProvider`: Provides testable timestamps for login updates.

### `data`

The data module contains:

- `AuthRepoImpl`: Coordinates remote users, local SQLDelight users, and current-user preferences.
- `RemoteDataSource`: Shared remote contract.
- `FirebaseRemoteDataSource`: Android Firestore implementation for loading users.
- `IosRemoteDataSource`: iOS placeholder until remote sync is configured on iOS.
- `LocalDataSource`: Shared local users contract.
- `SqlDelightLocalDataSource`: SQLDelight-backed local users implementation.
- `CurrentUserLocalDataSource`: Contract for current-session persistence.
- `DataStorePref`: DataStore implementation for saving and reading the current user.
- `DataModule`: Koin bindings for database, data sources, repository, and use cases.
- SQLDelight schema: `User.sq` defines the local users table and queries.

## App process and navigation

1. Android starts from `MainActivity`; iOS starts from `MainViewController`.
2. Each platform creates a `PlatformContext` and calls the shared `App` composable.
3. `App` starts `KoinApplication` and installs `appModules(platformContext)`.
4. `AppNavHost` checks `HasCurrentUserUseCase`.
5. If a current user exists, navigation starts at `Home`.
6. If no current user exists, navigation starts at `Login`.
7. On the login screen, the cashier enters a phone number and password.
8. The ViewModel normalizes phone digits, supports Arabic/Persian numerals, validates the phone/password, and shows field errors through state.
9. `LoginUseCase` checks the local user by phone, compares the password, updates `lastLoginAt`, and stores the current user in DataStore.
10. On success, `LoginEffect.NavigateToPos` is emitted.
11. `AppNavHost` navigates to `Home` and removes `Login` from the back stack.
12. `HomeScreen` currently shows the POS placeholder and is ready for the next cashier workflow.

Remote users are loaded through `SyncUsersUseCase`, which reads users from Firestore on Android and writes them to SQLDelight. Login then works against the local SQLDelight users table, giving the app a clear path for offline-friendly authentication.

## Running the app

### Android

```bash
./gradlew :composeApp:assembleDebug
```

Open the project in Android Studio and run the `composeApp` Android target on an emulator or device.

### iOS

Open `iosApp/iosApp.xcodeproj` in Xcode and run the iOS app target. The shared UI is rendered through `ComposeUIViewController`.

## Testing

Run all available checks:

```bash
./gradlew check
```

Run focused module tests:

```bash
./gradlew :domain:allTests
./gradlew :data:allTests
./gradlew :composeApp:allTests
```

Current test coverage includes login business rules, current-user checks, repository behavior, DataStore persistence, and LoginViewModel state/effect behavior.
