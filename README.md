# Sahm Food POS

Sahm Food POS is a Kotlin Multiplatform point-of-sale application for food cashier workflows. It shares the main product logic, UI, navigation, dependency injection, local persistence, and tests across Android and iOS while keeping platform code limited to native integrations such as Firebase, SQLDelight drivers, Android WorkManager, Android SmartPOS printing, and iOS HTTP access to Firestore.

The current implementation is not only a login shell. It includes cashier authentication, menu browsing, cart building, dine-in/takeaway/delivery order creation, discount validation, cash and card payment flows, receipt printing, order history, refund domain logic, manual and automatic synchronization, and an offline outbox for orders, payments, and refunds.

## Table of contents

- [Tech stack](#tech-stack)
- [Architecture](#architecture)
- [Main features](#main-features)
- [Important code paths](#important-code-paths)
- [Handled edge cases](#handled-edge-cases)
- [Sync and offline strategy](#sync-and-offline-strategy)
- [Persistence model](#persistence-model)
- [Platform behavior](#platform-behavior)
- [Running the project](#running-the-project)
- [Testing](#testing)
- [Project structure](#project-structure)

## Tech stack

- Kotlin Multiplatform for shared Android and iOS code.
- Compose Multiplatform and Material 3 for the shared UI.
- Compose Navigation for screen routing.
- Android adaptive window APIs for phone/tablet layout selection.
- Koin for dependency injection and ViewModel creation.
- Kotlin Coroutines and Flow for async work, UI state, and one-time effects.
- SQLDelight for local users, menu items, discounts, orders, payments, refunds, and sync outbox storage.
- AndroidX DataStore Preferences for current-user and time-sync metadata.
- Firebase Firestore on Android for remote users, menu items, discounts, and outbox uploads.
- Ktor client on iOS for Firestore REST access.
- Android WorkManager for background outbox sync.
- SmartPOS printer AIDL integration on Android.
- kotlin.test and kotlinx-coroutines-test for domain, data, and ViewModel tests.

## Architecture

The project uses clean architecture with an MVI-style UI layer.

```text
composeApp -> data -> domain
composeApp -> domain
```

### `domain`

The domain module is the business core. It contains entities, repository contracts, gateway/printer abstractions, result models, sync policies, and use cases. It has no dependency on Compose, Firebase, SQLDelight, WorkManager, or platform APIs.

Examples:

- `CreateOrderUseCase` validates cart input, verifies the cashier, checks local menu item availability, calculates service/tax/discount totals, creates order lines, and schedules sync.
- `PayOrderByCashUseCase` creates a paid cash payment, marks the order as paid, starts receipt printing in the background, and schedules sync.
- `PayOrderByCardUseCase` validates card fields, writes a processing payment, calls the payment gateway, records success/failure details, updates the order, and schedules printing/sync on success.
- `CreateRefundUseCase`, `RefundByCashUseCase`, and `RefundByCardUseCase` handle refund selection, proration, payment gateway refunding, receipt printing, and aggregate order/payment status updates.
- `SyncRetryPolicy` defines retryable error codes and exponential-style retry delays.

### `data`

The data module implements the domain contracts. It owns local and remote data access, mapping, platform database drivers, image caching, background sync integration, and printer implementations.

Examples:

- `SyncDataRepoImpl` coordinates Firestore data, SQLDelight persistence, cached menu images, server time checks, outbox upload, dependency checks, and aggregate sync timestamps.
- `OrderRepoImpl` persists orders/payments/refunds transactionally and writes idempotent outbox rows for sync.
- `SqlDelightLocalDataSourceImpl` implements local snapshots for users, menu items, discounts, and outbox state transitions.
- `SyncOutboxProcessorImpl` processes pending outbox rows safely, including stale lock recovery, dependency delays, retryable failures, non-retryable failures, and conflicts.

### `composeApp`

The Compose app module owns the shared UI, navigation, MVI contracts, ViewModels, theme, reusable components, and platform entry points.

Examples:

- `LoginViewModel` normalizes phone input, validates credentials, checks sync warnings, and emits navigation effects.
- `HomeViewModel` loads menu items, filters categories/search, manages the cart, applies discounts, creates orders, and drives cash/card payment flows.
- `OrdersViewModel` lists historical orders, filters and sorts them, loads full order details, and supports receipt reprinting.
- `SyncViewModel` exposes sync counts, last sync times, skipped invalid rows, outbox counts, and manual sync actions.
- `SettingsViewModel` loads the current cashier, logs out, and links to sync tools.

## Main features

### Authentication

- Cashiers log in with phone and password.
- Phone input is normalized by removing whitespace and converting Arabic/Persian digits to Latin digits.
- Login works against locally synced users, so authentication does not require a live network request after users are synced.
- Successful login updates `lastLoginAt` and stores the current user in DataStore.
- The login screen checks pending/failed/conflicted outbox rows before login and exposes sync navigation when attention is needed.

### Menu and cart

- Menu items are loaded from the local SQLDelight database.
- Items can be filtered by category and searched by name.
- The cart supports adding items, changing quantities, and removing lines.
- Cart edits invalidate the previously created order id/payment selection so the user cannot accidentally pay an outdated draft.
- Order types include `DINE_IN`, `TAKEAWAY`, and `DELIVERY`.

### Order totals

Amounts are stored in minor units as `Long` values to avoid floating-point money errors in persisted orders.

The domain order calculation:

1. Sum item line subtotals.
2. Add 10% service for dine-in orders.
3. Validate and apply promo code discount when present.
4. Calculate 14% tax on the amount after discount.
5. Store subtotal, service, discount, tax, and final total on the order.
6. Allocate order-level discounts and taxes back to order lines proportionally.

`CreateOrderUseCase` is the source of truth for persisted totals. The UI recalculates a live preview for cashier feedback, but the domain layer revalidates and recomputes everything before saving.

### Discounts

- Discounts are synced from remote data into the local database.
- A promo code can be applied to the current cart before payment.
- Discount configuration is validated before use.
- The order creation step rechecks that the discount exists, has started, has not expired, and satisfies the minimum order value.
- Discount amounts are capped by max value and by the order subtotal so totals cannot go below zero.

### Payments

- Cash payments immediately create a paid payment row and mark the order as paid.
- Card payments first write a `Processing` payment, then call the `PaymentGateway`.
- Successful card payments persist transaction id, gateway reference, authorization code, card brand, and last four digits.
- Failed card payments persist failure reason and keep the order available for retry.
- A failed or processing card payment can be reused on retry instead of creating unlimited duplicate payment rows.

### Receipt printing

- Payment success triggers receipt printing in the background.
- Print status moves through `NotPrinted`, `Printing`, `Printed`, or `Failed`.
- Printing failures do not roll back the successful payment.
- Orders can be reprinted from the order details screen.
- Android uses the SmartPOS printer service through AIDL with connection retry and a mutex so multiple print jobs do not overlap.
- iOS currently uses a fake receipt printer implementation.

### Order history

- Orders are listed newest-first by default.
- Cashiers can search by order id, filter by order type, and switch sort direction.
- Order details include items, payments, refunds, totals, payment status, order status, and print status.

### Refunds

The refund domain is implemented even though the visible UI is currently centered on orders and payment history.

- Only paid or partially refunded orders are refundable.
- Refund selections must contain positive quantities.
- Already pending, processing, or completed refund quantities reduce the remaining refundable quantity.
- Refund amounts are prorated across subtotal, discount, tax, and total amounts.
- Cash refunds complete locally and print a refund receipt.
- Card refunds require an original completed card payment with a transaction id, call the payment gateway, and handle success/failure.
- Aggregate order/payment status becomes `PartiallyRefunded` or `Refunded` based on the refunded amount.

### Sync screens

- Users, items, discounts, orders, and payments each have sync entry points.
- Sync detail screens show counts, last sync time, synced/unsynced aggregate counts, and outbox health.
- Manual outbox sync is supported.
- Remote sync reports success, empty remote data, permission denied, invalid data, duplicate promo codes, local storage errors, request timeout, no internet, and unknown errors.

## Important code paths

### App startup and navigation

`App.kt` starts Koin with `appModules(platformContext)`. `AppNavHost` selects the start destination passed by the app shell, then routes between:

- `Login`
- `Home`
- `Orders`
- `Settings`
- `Sync`
- `SyncUsers`
- `SyncItems`
- `SyncDiscounts`
- `SyncOrders`
- `SyncPayments`

ViewModels expose `StateFlow` for render state and `SharedFlow` for one-time navigation/toast-like effects. Composables render from state and send intents back to the ViewModel.

### Login flow

`LoginViewModel` performs UI-level validation:

- Phone is required.
- Phone must be 12 Latin digits after normalization.
- Phone must start with `20`.
- Password is required.
- Password length must be greater than 6 characters.

`LoginUseCase` performs business-level validation:

- Trim phone/password.
- Load user by phone from `AuthRepo`.
- Compare the saved password.
- Update `lastLoginAt`.
- Save `CurrentUser`.

### Order creation flow

`HomeViewModel` creates an order only when the cashier chooses to make/pay the order. It sends `CreateOrderRequest` to `CreateOrderUseCase`.

`CreateOrderUseCase` handles the critical rules:

- Empty carts are rejected.
- Zero or negative quantities are rejected.
- Missing cashier is rejected.
- Menu item ids are reloaded from local storage to catch stale UI data.
- Missing or inactive menu items are rejected.
- Discount rules are rechecked at creation time.
- Order and order item ids come from `UUIDProvider`.
- Order, order items, and outbox row are written transactionally by `OrderRepoImpl`.

### Payment flow

Cash:

```text
PayOrderByCashUseCase
-> get order details
-> reject non-pending orders
-> insert paid payment
-> update order as paid
-> print receipt in background
-> schedule outbox sync
```

Card:

```text
PayOrderByCardUseCase
-> get order details
-> reject non-pending orders
-> validate card fields
-> insert/update processing payment
-> call payment gateway
-> on success: persist gateway data, mark order paid, print, schedule sync
-> on failure: persist failed payment details and show reason
```

### Sync outbox flow

Local business actions write outbox rows with stable idempotency keys:

```text
CREATE_ORDER:<orderId>
CREATE_PAYMENT:<paymentId>
CREATE_REFUND:<refundId>
```

`SyncOutboxProcessorImpl` processes rows in creation order:

1. Reset stale `IN_PROGRESS` rows after 10 minutes.
2. Select pending or ready retry rows.
3. Check aggregate dependencies.
4. Mark the row `IN_PROGRESS`.
5. Upload to remote.
6. Mark success, retry waiting, failed, or conflict.
7. Return `NeedsRetry` when work remains.

Dependency ordering prevents a payment from uploading before its order and prevents a refund from uploading before both its order and payment.

## Handled edge cases

### Authentication edge cases

- Blank phone and blank password are reported separately.
- Arabic and Persian numerals are accepted and normalized.
- Whitespace in phone numbers is ignored.
- Non-digit phone input is rejected.
- Invalid phone length/prefix is rejected.
- Wrong phone/password returns invalid credentials without exposing which field was wrong.
- Login failures from the repository are converted to a generic failure result.

### Menu/cart/order edge cases

- Empty cart cannot create an order.
- Quantity `0` or negative quantities are rejected.
- Removing an item clears it from the cart.
- Selecting an unknown category falls back to the "all" category.
- Stale menu items are reloaded from the database before order creation.
- Deleted menu items are rejected.
- Inactive menu items are rejected.
- Changing cart contents, order type, or applied discount clears the existing created order id so payment cannot be applied to stale order data.

### Money calculation edge cases

- Persisted money uses integer minor units.
- Percentage calculations use rounded integer math.
- Discount and tax are allocated proportionally to line items.
- The last line receives the remaining allocation amount to avoid rounding drift.
- Discounts cannot reduce taxable amount or totals below zero.
- Dine-in service is only applied when the order type is `DINE_IN`.

### Discount edge cases

- Missing promo code returns `PromoCodeNotFound`/`DiscountNotFound`.
- Structurally invalid discounts are rejected in the preview use case.
- Discounts before `startAt` are rejected.
- Discounts after `endAt` are rejected.
- Discounts below the minimum order value are rejected during final order creation.
- Duplicate promo codes during sync are rejected.
- Remote discount data that cannot map to valid local discounts is reported.
- Local storage failure while replacing discounts is reported.

### Payment edge cases

- Unknown order id fails payment.
- Orders that are not `PendingPayment` cannot be paid again.
- Card number must be non-blank and numeric after removing spaces.
- CVV must be 3 or 4 digits.
- Expiry month must be between 1 and 12.
- Two-digit expiry years are normalized to `20xx`.
- Expiry years before 2026 are rejected.
- Card holder name is required.
- Failed card payment attempts preserve the failure reason and card last four digits when possible.
- Retry after failed/processing card payment reuses the previous payment id.

### Printing edge cases

- Missing order/refund details fail printing cleanly.
- Printer service unavailability is retried.
- Printer exceptions reset the service binding and retry.
- Concurrent print jobs are serialized with a mutex.
- Payment remains successful even if printing fails.
- Print status is updated to `Failed` when a background print throws.

### Refund edge cases

- Empty refund selection is rejected.
- Zero or negative refund quantities are rejected.
- Missing order is rejected.
- Orders that are unpaid or already fully invalid for refund are rejected.
- Item ids not belonging to the order are rejected.
- Quantities greater than remaining refundable quantity are rejected.
- Pending and processing refunds reserve quantity so duplicate refund requests cannot exceed the original order quantity.
- Card refunds require the original card transaction id.
- Failed card refunds mark the refund as failed without marking the order refunded.

### Sync edge cases

- Empty remote users/items data is reported as empty.
- Invalid remote documents are skipped and counted.
- If every remote discount document is invalid, sync returns invalid remote data.
- Network, timeout, permission, local storage, and unknown errors are mapped to explicit results.
- Duplicate outbox uploads are treated as success through idempotency keys.
- Retryable errors are delayed using `SyncRetryPolicy`.
- Non-retryable errors become `FAILED`.
- Server conflicts become `CONFLICT`.
- Stale `IN_PROGRESS` outbox rows are reset.
- Payment/refund uploads wait until required parent aggregates are synced.

## Sync and offline strategy

The app is designed around local-first cashier operations:

- Reference data such as users, menu items, and discounts is synced from remote into SQLDelight.
- Cashier actions are written locally first.
- Orders, payments, and refunds create outbox rows in the same local transaction as the business record.
- Background sync uploads outbox rows later when the network is available.
- Idempotency keys make repeated uploads safe.
- Aggregate tables keep their own `synced_at` timestamps so the UI can show synced/unsynced counts.

On Android, `WorkManagerSyncScheduler` enqueues unique work named `sync_outbox` with connected-network constraints and exponential backoff. On iOS, the scheduler is currently a no-op, but manual processing and the shared repository/processor logic are available.

## Persistence model

SQLDelight tables are organized around:

- `users`: locally synced cashier records.
- `menu_items`: locally synced menu snapshot with active/inactive support.
- `discounts`: locally synced promo code rules.
- `orders`: order header, cashier info, totals, statuses, discount snapshot, and sync timestamp.
- `order_items`: immutable order line snapshot.
- `payments`: cash/card payment attempts and gateway metadata.
- `refunds`: refund header, status, method, amount, and sync timestamp.
- `refund_items`: prorated refund line details.
- `sync_outbox`: upload queue with idempotency key, retry metadata, lock timestamp, and last error.

Snapshot sync intentionally marks missing users/menu items inactive rather than deleting them. That preserves historical order integrity while preventing stale users/items from being used for new activity.

## Platform behavior

### Android

- Uses Firebase Firestore SDK for remote data.
- Uses Android SQLDelight driver.
- Uses AndroidX DataStore Preferences.
- Uses WorkManager for outbox sync.
- Uses SmartPOS AIDL printer service from `data/libs/SmartPos_1.3.6_R201217.jar`.
- Supports Firebase configuration through `composeApp/google-services.json`.

### iOS

- Uses Ktor/Darwin HTTP client for Firestore REST calls.
- Uses SQLDelight native driver.
- Uses shared Compose UI through `ComposeUIViewController`.
- Uses a fake receipt printer.
- Automatic background outbox scheduling is currently not implemented.

## Running the project

### Android

Build the debug Android app:

```bash
./gradlew :composeApp:assembleDebug
```

Or open the project in Android Studio and run the `composeApp` Android target.

### iOS

Open the iOS project in Xcode:

```text
iosApp/iosApp.xcodeproj
```

Run the iOS app target. The shared UI is rendered from `MainViewController`.

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

- Login validation and login use case behavior.
- Current-user checks and DataStore persistence.
- User, menu item, and discount sync.
- Phone/server time validation.
- Discount application rules.
- Order creation, payment, refund, and receipt retry use cases.
- Sync idempotency keys and retry policy.
- Sync outbox processing.
- SQLDelight local data source behavior for users, menu items, discounts, orders, and outbox rows.
- Home, login, orders, and sync ViewModel behavior.
- Android WorkManager sync scheduling.

## Project structure

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

## Notes for reviewers

- The domain module is the best place to review business correctness because it contains the rules without platform noise.
- The data module is the best place to review persistence, sync, Firestore mapping, and printer integration.
- The Compose ViewModels are the best place to review UI state transitions and one-time effects.
- Order/payment/refund actions are intentionally local-first; remote upload is decoupled through the outbox so cashier work can continue during temporary network problems.
