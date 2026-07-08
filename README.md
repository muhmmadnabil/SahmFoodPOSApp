# Restaurant POS

Restaurant POS is a Kotlin Multiplatform point-of-sale app for restaurant cashier workflows. The project focuses on a practical offline-first flow: cashiers can log in, browse a synced menu, create orders, take payments, print receipts, and keep working even when the network is temporarily unavailable.

The same business logic, UI, navigation, local database, dependency injection, and tests are shared across Android and iOS. Platform-specific code is used only where it is needed, such as Firebase, SQLDelight drivers, WorkManager, SmartPOS printing, and iOS Firestore REST calls.

## What The App Does

- Cashier login using locally synced users.
- Menu browsing from local SQLDelight storage.
- Category filtering and menu search.
- Cart building with quantity changes and item removal.
- Dine-in, takeaway, and delivery orders.
- Promo code validation and discount calculation.
- Cash and card payment flows.
- Receipt printing on Android SmartPOS devices.
- Order history with filters, sorting, and order details.
- Refund domain logic for cash and card refunds.
- Manual and automatic sync for users, menu items, discounts, orders, payments, and refunds.
- Offline outbox for actions that must be uploaded later.

## Why Offline-First Matters Here

In a restaurant, the cashier should not stop taking orders just because the internet drops for a few minutes. This project handles that by making local storage the source of truth for cashier actions.

The basic idea is:

1. The app downloads reference data such as users, menu items, and discounts.
2. The cashier creates orders and payments locally.
3. Every important local action writes an outbox row in the same database transaction.
4. The app uploads those outbox rows when the network is available.
5. If upload fails, the row stays in the outbox and is retried later.
6. The UI can show what is synced, pending, failed, or conflicted.

This means the cashier flow stays fast and reliable, while remote sync happens in the background.

## Tech Stack

- Kotlin Multiplatform for shared Android and iOS code.
- Compose Multiplatform and Material 3 for shared UI.
- Compose Navigation for screen routing.
- Koin for dependency injection.
- Kotlin Coroutines and Flow for async work and UI state.
- SQLDelight for local users, menu items, discounts, orders, payments, refunds, and sync outbox storage.
- AndroidX DataStore Preferences for current-user and time-sync metadata.
- Firebase Firestore on Android.
- Ktor client on iOS for Firestore REST access.
- Android WorkManager for background outbox sync.
- SmartPOS printer AIDL integration on Android.
- kotlin.test and kotlinx-coroutines-test for tests.

## Architecture

The project uses clean architecture with an MVI-style UI layer.

```text
composeApp -> domain
composeApp -> data -> domain
```

### Domain Module

The domain module contains the business rules. It does not depend on Compose, Firebase, SQLDelight, WorkManager, or platform APIs.

Important examples:

- `CreateOrderUseCase` validates the cart, reloads menu items from local storage, calculates totals, applies discounts, and creates the order.
- `PayOrderByCashUseCase` creates a paid cash payment, marks the order as paid, starts receipt printing, and schedules sync.
- `PayOrderByCardUseCase` stores a processing payment, calls the payment gateway, saves success or failure, and schedules sync only when payment succeeds.
- `CreateRefundUseCase`, `RefundByCashUseCase`, and `RefundByCardUseCase` handle refund rules and refund status updates.
- `SyncRetryPolicy` defines which sync errors should be retried and how long to wait before retrying.

### Data Module

The data module connects the business rules to local storage, remote APIs, sync, and printing.

Important examples:

- `OrderRepoImpl` saves orders, payments, refunds, and outbox rows transactionally.
- `SyncDataRepoImpl` reads remote reference data, uploads outbox rows, marks aggregates as synced, and checks upload dependencies.
- `SqlDelightLocalDataSourceImpl` owns SQLDelight queries and sync state transitions.
- `SyncOutboxProcessorImpl` processes pending outbox rows safely.
- `FirebaseRemoteDataSource` uploads Android outbox rows to Firestore.

### Compose App Module

The Compose app module contains screens, ViewModels, navigation, theme, and reusable UI components.

Important examples:

- `LoginViewModel` validates login input and checks if sync needs attention.
- `HomeViewModel` loads menu items, manages the cart, applies discounts, creates orders, and starts payment flows.
- `OrdersViewModel` shows order history and order details.
- `SyncViewModel` shows sync counts, last sync times, and manual sync actions.
- `SettingsViewModel` loads the current cashier and handles logout.

## Core Flows

### Login

Cashiers log in with phone and password. Users are synced into the local database, so login can work without a live network request after the first sync.

Phone numbers are normalized by removing spaces and converting Arabic/Persian digits to Latin digits.

### Menu And Cart

Menu items are loaded from SQLDelight. The cashier can filter by category, search by name, add items to the cart, change quantities, and choose the order type.

When the cart changes, the existing created order id is cleared. This prevents the app from paying an old order after the cashier changes the cart.

### Order Creation

Order creation is local-first.

`CreateOrderUseCase` revalidates the cart before saving:

- The cart cannot be empty.
- Quantities must be positive.
- The cashier must exist.
- Menu items are reloaded from local storage.
- Missing or inactive menu items are rejected.
- Promo code rules are checked again.
- Totals are calculated in the domain layer.

`OrderRepoImpl` then saves the order, order items, and `CREATE_ORDER` outbox row in one transaction.

### Payments

Cash payment is completed locally and can be uploaded later.

Card payment stores a processing payment first, then calls the payment gateway:

- On success, payment details are saved, the order becomes paid, receipt printing starts, and sync is scheduled.
- On failure, the failure reason is saved and the order stays available for retry.
- A failed or processing card payment can be reused on retry instead of creating duplicate payment rows.

### Receipt Printing

Receipt printing is separate from payment success. If the printer fails, the payment is still saved as successful and the print status becomes `Failed`. The cashier can retry printing from order details.

Android uses the SmartPOS printer service through AIDL. iOS currently uses a fake receipt printer.

### Refunds

Refund logic is implemented in the domain layer:

- Only paid or partially refunded orders are refundable.
- Refund quantities cannot exceed the remaining refundable quantity.
- Pending and processing refunds reserve quantity so the same items are not refunded twice.
- Cash refunds complete locally.
- Card refunds require the original card transaction id.
- Failed card refunds are saved without marking the order as refunded.

## Offline Sync Flow

Local actions create outbox rows with stable idempotency keys:

```text
CREATE_ORDER:<orderId>
CREATE_PAYMENT:<paymentId>
CREATE_REFUND:<refundId>
```

The processor handles the queue like this:

1. Reset stale `IN_PROGRESS` rows after 10 minutes.
2. Select `PENDING` rows and `RETRY_WAITING` rows whose retry time has arrived.
3. Process rows in creation order.
4. Check dependencies before upload.
5. Mark the row `IN_PROGRESS`.
6. Upload to remote.
7. Mark the row as `SUCCEEDED`, `RETRY_WAITING`, `FAILED`, or `CONFLICT`.

Dependency checks are important:

- Orders can upload directly.
- Payments wait until their order has synced.
- Refunds wait until their order and payment have synced.

This keeps remote data in a valid order even if the cashier created everything offline.

## What Happens When Upload Fails

The app treats failures differently depending on the reason.

### Retryable Failure

Examples:

- No internet.
- Timeout.
- Temporary server error.
- Rate limit.

The row becomes `RETRY_WAITING`, keeps the error details, increases its retry count, and gets a `next_attempt_at` time. When that time arrives, WorkManager or manual sync can upload it again.

### Non-Retryable Failure

Examples:

- Unauthorized cashier.
- Invalid payload rejected by remote.

The row becomes `FAILED`. This is not retried automatically because sending the same payload again will probably fail again until the data or permissions are fixed.

### Conflict

Conflicts become `CONFLICT`. This keeps the row visible for manual handling instead of hiding a data problem.

### Duplicate Upload

If the remote already has the same idempotency key, the app treats it as success. This protects the system from duplicate orders/payments when the app retries after a timeout.

## Persistence Model

SQLDelight stores:

- `users`: synced cashier records.
- `menu_items`: synced menu snapshot.
- `discounts`: synced promo code rules.
- `orders`: order header, totals, statuses, cashier snapshot, and sync timestamp.
- `order_items`: immutable order item snapshots.
- `payments`: cash/card payment attempts and gateway metadata.
- `refunds`: refund header, method, amount, status, and sync timestamp.
- `refund_items`: refund item details.
- `sync_outbox`: upload queue with idempotency key, retry state, lock time, and last error.

Users and menu items that disappear from the remote snapshot are marked inactive instead of deleted. This keeps old orders readable while preventing inactive records from being used for new actions.

## Platform Behavior

### Android

- Uses Firebase Firestore SDK.
- Uses SQLDelight Android driver.
- Uses AndroidX DataStore Preferences.
- Uses WorkManager for background outbox upload.
- Uses SmartPOS AIDL printer service from `data/libs/SmartPos_1.3.6_R201217.jar`.
- Uses `composeApp/google-services.json` for Firebase configuration.

### iOS

- Uses Ktor/Darwin HTTP client for Firestore REST calls.
- Uses SQLDelight native driver.
- Uses shared Compose UI through `ComposeUIViewController`.
- Uses a fake receipt printer.
- Automatic background scheduling is not implemented yet, but manual/shared sync logic is available.

## Running The Project

Build the Android debug app:

```bash
./gradlew :composeApp:assembleDebug
```

Or open the project in Android Studio and run the `composeApp` Android target.

For iOS, open:

```text
iosApp/iosApp.xcodeproj
```

Then run the iOS app target from Xcode.

## Testing

Run all checks:

```bash
./gradlew check
```

Run focused module tests:

```bash
./gradlew :domain:allTests
./gradlew :data:allTests
./gradlew :composeApp:allTests
```

Current tests cover:

- Login validation and current-user behavior.
- User, menu item, and discount sync.
- Phone/server time validation.
- Discount rules.
- Order creation, payment, refund, and receipt retry use cases.
- Sync idempotency keys and retry policy.
- Sync outbox processing.
- SQLDelight local behavior for users, menu items, discounts, orders, and outbox rows.
- Home, login, orders, and sync ViewModel behavior.
- Android WorkManager sync scheduling.

## Project Structure

```text
.
├── composeApp
│   └── src
│       ├── commonMain/kotlin/com/sahm/pos
│       │   ├── App.kt
│       │   ├── navigation
│       │   ├── screens
│       │   ├── components
│       │   ├── di
│       │   ├── theme
│       │   └── utils
│       ├── androidMain
│       └── iosMain
├── domain
│   └── src/commonMain/kotlin/com/sahm/pos/domain
│       ├── entity
│       ├── repository
│       ├── results
│       ├── sync
│       └── usecase
├── data
│   └── src
│       ├── commonMain/kotlin/com/sahm/pos/data
│       │   ├── local
│       │   ├── mapper
│       │   ├── model
│       │   ├── printing
│       │   ├── remote
│       │   ├── repo
│       │   └── sync
│       ├── commonMain/sqldelight
│       ├── androidMain
│       └── iosMain
└── iosApp
```
