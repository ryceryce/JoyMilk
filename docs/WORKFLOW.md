# Workflow — JoyMilk Application Workflows

## 1. Overview

This document describes all user-facing workflows in the JoyMilk Android application. Each workflow is illustrated with Mermaid diagrams (flowcharts, sequence diagrams, or state diagrams) to provide a clear visual understanding of system behavior.

**Actors:**
| Actor | Role | Permissions |
|-------|------|-------------|
| **Admin** | Business owner / manager | Full CRUD on products, orders, users, reports |
| **Staff** | Operational employee | Create/manage orders, read products, view reports |

**Key Concepts:**
- **Price Snapshot Pattern** — Orders freeze the product price at creation time (`priceAtOrder`), so future price changes never affect historical orders.
- **Sweet Level** — A per-item preference (Tanpa Gula / Less Sugar / Normal / Extra Sweet) with no price impact.
- **All monetary values** are stored as `Long` in IDR (Indonesian Rupiah) — no floating point.

---

## 2. Authentication Flow

The app uses **Firebase Authentication** (email/password) combined with a Firestore `users` document that stores the user's role and active status.

```mermaid
flowchart TD
    A["App Launch"] --> B{"Check Auth State\n(Firebase Auth)"}
    B -->|Authenticated| C{"Fetch User Doc\nfrom Firestore"}
    B -->|Not Authenticated| D["Login Screen"]

    C -->|"isActive = true"| E{"Check Role"}
    C -->|"isActive = false"| F["Show 'Account Disabled'\nError"]
    F --> G["Sign Out"] --> D

    E -->|Admin| H["Admin Dashboard"]
    E -->|Staff| I["Staff Dashboard"]

    D --> J["Enter Email & Password"]
    J --> K["Firebase Auth\nSignInWithEmailAndPassword"]
    K -->|Success| C
    K -->|Failure| L["Show Error Message\n(Invalid credentials, network, etc.)"]
    L --> D

    H --> M["Logout"]
    I --> M
    M --> N["Clear Session\n(Firebase Sign Out)"] --> D
```

### Authentication Rules

| Scenario | Behavior |
|----------|----------|
| Valid credentials, active user | Navigate to role-based Dashboard |
| Valid credentials, inactive user | Show error, sign out immediately |
| Invalid credentials | Show error on Login Screen |
| Token expired (mid-session) | Redirect to Login Screen |
| No network at login | Show network error |

---

## 3. Order Creation Flow

This is the **most critical workflow** in the application. It involves selecting products, setting sweet levels and quantities, and snapshotting prices at order time.

```mermaid
sequenceDiagram
    actor S as Staff
    participant UI as Order Form UI
    participant VM as OrderViewModel
    participant FS as Firestore

    S->>UI: Open "Buat Pesanan"
    UI->>VM: Initialize new order

    Note over S,UI: Step 1 — Customer Info
    S->>UI: Enter customer name
    S->>UI: Enter phone (optional)
    S->>UI: Enter address (optional)

    Note over S,UI: Step 2 — Add Items (repeatable)
    S->>UI: Tap "Tambah Item"
    UI->>VM: Request active products list
    VM->>FS: Query products where isActive=true
    FS-->>VM: Return product list
    VM-->>UI: Display product dropdown

    S->>UI: Select product
    UI-->>S: Show current price from master data

    S->>UI: Select sweet level<br/>(Tanpa Gula / Less Sugar /<br/>Normal / Extra Sweet)
    S->>UI: Set quantity

    UI->>VM: Add item to local order list
    VM-->>UI: Update order summary & running total

    Note over S,UI: Repeat "Tambah Item" for more products

    Note over S,UI: Step 3 — Payment & Notes
    S->>UI: Select payment method<br/>(Cash / Transfer Bank / E-Wallet)
    S->>UI: Add notes (optional)

    Note over S,FS: Step 4 — Save Order
    S->>UI: Tap "Simpan Order"
    UI->>VM: Submit order

    VM->>VM: Validate order<br/>(≥1 item, customer name filled)

    VM->>FS: Create Order document<br/>(status=pending, paymentMethod,<br/>customerName, totalAmount, createdBy)

    loop For each item in order
        VM->>FS: Create OrderItem sub-document<br/>(productId, productName, priceAtOrder,<br/>sweetLevel, quantity, subtotal)
    end

    VM->>VM: Calculate totalAmount =<br/>Σ (priceAtOrder × quantity)

    FS-->>VM: Write confirmation
    VM-->>UI: Show success message
    UI-->>S: Navigate to Order Detail
```

### Price Snapshot Detail

```mermaid
flowchart LR
    A["Product Master Data\ncurrentPrice = 25000"] --> B["Staff adds item\nto order"]
    B --> C["OrderItem created\npriceAtOrder = 25000\n(snapshot frozen)"]
    C --> D["Product price changes\ncurrentPrice = 28000"]
    D --> E["OrderItem still shows\npriceAtOrder = 25000\n(unchanged)"]
```

### Order Data Structure at Save

| Field | Source | Example |
|-------|--------|---------|
| `customerName` | User input | "Bu Sari" |
| `customerPhone` | User input (optional) | "081234567890" |
| `customerAddress` | User input (optional) | "Jl. Merdeka No. 10" |
| `paymentMethod` | User selection | "cash" |
| `status` | Auto-set | "pending" |
| `totalAmount` | Calculated | 75000 |
| `notes` | User input (optional) | "Antar sore" |
| `createdBy` | Auth state | "userId123" |
| `createdAt` | Server timestamp | — |
| **items/** `productName` | Snapshot from product | "Susu Kedelai Original" |
| **items/** `priceAtOrder` | Snapshot from product | 25000 |
| **items/** `sweetLevel` | User selection | "normal" |
| **items/** `quantity` | User input | 3 |
| **items/** `subtotal` | priceAtOrder × quantity | 75000 |

---

## 4. Order Status Update Flow

Orders follow a strict state machine. Certain transitions are role-restricted.

```mermaid
stateDiagram-v2
    [*] --> pending : Order Created

    pending --> processing : Admin / Staff
    pending --> cancelled : Admin / Staff

    processing --> delivered : Admin / Staff
    processing --> cancelled : Admin Only

    delivered --> [*]
    cancelled --> [*]

    note right of delivered : Final state\nNo further changes
    note right of cancelled : Final state\nExcluded from reports
```

### Transition Permissions

| From | To | Admin | Staff |
|------|----|:-----:|:-----:|
| pending | processing | ✅ | ✅ |
| pending | cancelled | ✅ | ✅ |
| processing | delivered | ✅ | ✅ |
| processing | cancelled | ✅ | ❌ |

### Status Update Sequence

```mermaid
sequenceDiagram
    actor U as Admin/Staff
    participant UI as Order Detail UI
    participant VM as OrderViewModel
    participant FS as Firestore

    U->>UI: Tap status action button
    UI->>VM: Request status change (newStatus)
    VM->>VM: Validate transition<br/>(check current status + user role)

    alt Valid transition
        VM->>FS: Update order.status = newStatus<br/>Update order.updatedAt = now
        FS-->>VM: Confirmation
        VM-->>UI: Refresh order detail
        UI-->>U: Show updated status
    else Invalid transition
        VM-->>UI: Show error<br/>("Anda tidak memiliki izin")
        UI-->>U: Display error toast
    end
```

---

## 5. Product Management Flow (Admin Only)

### 5.1 Add Product

```mermaid
flowchart TD
    A["Admin: Product List Screen"] --> B["Tap 'Tambah Produk'"]
    B --> C["Product Form Screen"]
    C --> D["Fill form:\n• Nama Produk\n• Kategori\n• Harga (IDR)\n• Satuan\n• Deskripsi"]
    D --> E["Tap 'Simpan'"]
    E --> F{"Validate Form"}

    F -->|Valid| G["Create Product Document\nin Firestore\n(isActive = true)"]
    F -->|Invalid| H["Show Validation Errors"] --> D

    G --> I["Create initial priceHistory\nsub-document:\nprice, effectiveDate=now,\nchangedBy=adminId"]
    I --> J["Navigate back to\nProduct List"]
    J --> K["Show Success Toast"]
```

### 5.2 Edit Product (with Price Change)

When an admin edits a product, the system checks whether the price has changed. If so, it automatically creates a new `priceHistory` entry — all within a single Firestore transaction.

```mermaid
sequenceDiagram
    actor A as Admin
    participant UI as Product Detail UI
    participant VM as ProductViewModel
    participant FS as Firestore

    A->>UI: Open product detail
    UI->>VM: Load product data
    VM->>FS: Get product document
    FS-->>VM: Product data (name, category, price, etc.)
    VM-->>UI: Display product form (pre-filled)

    A->>UI: Edit fields<br/>(name, category, price, etc.)
    A->>UI: Tap "Simpan"
    UI->>VM: Submit changes

    VM->>VM: Compare new price vs old price

    alt Price Changed
        VM->>FS: BEGIN Transaction
        VM->>FS: 1. Update product document<br/>(name, category, currentPrice, etc.)
        VM->>FS: 2. Create priceHistory entry<br/>(price=newPrice, effectiveDate=now,<br/>changedBy=adminId)
        VM->>FS: COMMIT Transaction
    else Price Not Changed
        VM->>FS: Update product document<br/>(name, category, etc. — no price fields)
    end

    FS-->>VM: Write confirmation
    VM-->>UI: Show success message
    UI-->>A: Navigate back / refresh
```

### 5.3 Deactivate Product

```mermaid
flowchart TD
    A["Admin: Product Detail"] --> B["Toggle 'Aktif' switch OFF"]
    B --> C["Confirmation Dialog:\n'Nonaktifkan produk ini?'"]
    C -->|Cancel| A
    C -->|Confirm| D["Update Firestore:\nproduct.isActive = false"]
    D --> E["Product removed from\nOrder Form dropdown"]
    D --> F["Existing orders with\nthis product: UNCHANGED\n(snapshot preserved)"]
    E --> G["Show success toast"]
```

> [!IMPORTANT]
> Deactivating a product only hides it from the order creation form. All existing orders that reference this product retain their snapshot data (`productName`, `priceAtOrder`) and are completely unaffected.

---

## 6. Report Generation Flow

Reports aggregate order data using the **already-snapshotted prices** in each order — no need to look up historical product prices.

```mermaid
flowchart TD
    A["User: Navigate to Reports"] --> B["Select Report Type"]

    B --> C["Harian\n(Daily)"]
    B --> D["Mingguan\n(Weekly)"]
    B --> E["Bulanan\n(Monthly)"]

    C --> F["Select Date"]
    D --> G["Select Week"]
    E --> H["Select Month"]

    F --> I["Query Orders"]
    G --> I
    H --> I

    I --> J["Firestore Query:\norders where createdAt\nin date range\nAND status ≠ cancelled"]

    J --> K["Aggregate Data"]

    K --> L["Total Orders Count"]
    K --> M["Total Revenue\nΣ totalAmount"]
    K --> N["Orders per Status\n(pending, processing, delivered)"]
    K --> O["Query Order Items\nfor product breakdown"]

    O --> P["Produk Terlaris\n(Best-selling products\nby quantity)"]

    L --> Q["Display Report"]
    M --> Q
    N --> Q
    P --> Q
```

### Report Data Aggregation

```mermaid
sequenceDiagram
    actor U as User
    participant UI as Report Screen
    participant VM as ReportViewModel
    participant FS as Firestore

    U->>UI: Select report type + period
    UI->>VM: Request report (type, dateRange)

    VM->>FS: Query orders<br/>(createdAt in range, status ≠ cancelled)
    FS-->>VM: List of orders

    VM->>VM: Aggregate orders:<br/>• count<br/>• sum totalAmount<br/>• group by status

    VM->>FS: Query order items<br/>for matched orders
    FS-->>VM: List of order items

    VM->>VM: Aggregate items:<br/>• group by productName<br/>• sum quantities<br/>• rank by popularity

    VM-->>UI: ReportUiState with all metrics
    UI-->>U: Display formatted report
```

### Report Metrics Summary

| Metric | Calculation | Source |
|--------|-------------|--------|
| Total Pesanan | `COUNT(orders)` | orders collection |
| Total Pendapatan | `SUM(order.totalAmount)` | orders collection |
| Pesanan per Status | `GROUP BY status` | orders collection |
| Produk Terlaris | `GROUP BY productName, SUM(quantity), ORDER BY DESC` | order items sub-collection |

---

## 7. User Management Flow (Admin Only)

### 7.1 Add User

```mermaid
flowchart TD
    A["Admin: User Management"] --> B["Tap 'Tambah User'"]
    B --> C["User Form:\n• Nama\n• Email\n• Password\n• Role (admin/staff)"]
    C --> D["Tap 'Simpan'"]
    D --> E{"Validate Input"}

    E -->|Invalid| F["Show Validation Errors"] --> C

    E -->|Valid| G["Create Firebase Auth User\n(email + password)"]
    G -->|Success| H["Create Firestore User Doc:\n• uid\n• name\n• email\n• role\n• isActive = true\n• createdAt"]
    G -->|Failure| I["Show Error\n(email already exists, etc.)"] --> C

    H --> J["Navigate to User List"]
    J --> K["Show Success Toast"]
```

### 7.2 Deactivate User

```mermaid
flowchart TD
    A["Admin: User List"] --> B["Select User"]
    B --> C["User Detail Screen"]
    C --> D["Toggle 'Aktif' switch OFF"]
    D --> E["Confirmation Dialog:\n'Nonaktifkan user ini?'"]

    E -->|Cancel| C
    E -->|Confirm| F["Update Firestore:\nuser.isActive = false"]
    F --> G["User's next login attempt\nwill be blocked"]
    G --> H["Show Success Toast"]
```

> [!NOTE]
> Deactivating a user sets `isActive = false` in the Firestore user document. The Firebase Auth account still exists, but the app checks `isActive` during the authentication flow (see [Section 2](#2-authentication-flow)) and blocks login for inactive users.

---

## 8. Data Sync Flow

JoyMilk uses **Firestore real-time listeners** to keep all connected devices in sync. Firestore also provides built-in offline persistence for handling intermittent connectivity.

```mermaid
sequenceDiagram
    participant DA as Device A
    participant FS as Firestore Cloud
    participant DB as Device B

    Note over DA,DB: Normal Sync (Online)
    DA->>FS: Write change<br/>(e.g., new order)
    FS-->>DA: Write confirmation
    FS--)DB: Real-time snapshot event
    DB->>DB: Update local UI

    Note over DA,DB: Offline Scenario
    DA->>DA: Device goes offline
    DA->>DA: Write change<br/>(stored in local cache)
    DA->>DA: UI updates immediately<br/>(optimistic)

    DA->>FS: Device comes back online<br/>Auto-sync pending writes
    FS-->>DA: Write confirmation
    FS--)DB: Real-time snapshot event
    DB->>DB: Update local UI
```

### Sync Behavior Summary

```mermaid
flowchart LR
    A["Local Write"] --> B{"Device Online?"}
    B -->|Yes| C["Write to Firestore\nimmediately"]
    B -->|No| D["Cache locally\n(offline persistence)"]
    D --> E["Device reconnects"]
    E --> C
    C --> F["Firestore distributes\nto all listeners"]
```

| Scenario | Behavior |
|----------|----------|
| Both devices online | Real-time sync within ~1–2 seconds |
| Writer device offline | Changes cached locally, synced on reconnect |
| Reader device offline | Reads from local cache, receives updates on reconnect |
| Concurrent edits | Firestore last-write-wins (see [Section 9](#9-error-handling-flows)) |

---

## 9. Error Handling Flows

### 9.1 Network Error During Order Save

```mermaid
flowchart TD
    A["Staff: Tap 'Simpan Order'"] --> B["ViewModel attempts\nFirestore write"]
    B --> C{"Network\nAvailable?"}

    C -->|Yes| D["Write succeeds"]
    D --> E["Show success message"]

    C -->|No / Timeout| F["Firestore caches\nwrite locally"]
    F --> G["Show info:\n'Pesanan disimpan offline.\nAkan disinkronkan otomatis.'"]
    G --> H{"Device reconnects?"}
    H -->|Yes| I["Auto-sync to Firestore"]
    I --> D
    H -->|Prolonged offline| J["Data persists in\nlocal cache until\nconnection restored"]
```

### 9.2 Authentication Expired

```mermaid
flowchart TD
    A["User performs action"] --> B["API call to Firestore"]
    B --> C{"Auth token\nvalid?"}
    C -->|Valid| D["Proceed normally"]
    C -->|Expired / Revoked| E["Firebase SDK attempts\ntoken refresh"]
    E -->|Refresh success| D
    E -->|Refresh failed| F["Show error:\n'Sesi Anda telah berakhir.\nSilakan login kembali.'"]
    F --> G["Clear local session"]
    G --> H["Redirect to\nLogin Screen"]
```

### 9.3 Concurrent Edit Conflict

```mermaid
sequenceDiagram
    participant A as Device A
    participant FS as Firestore
    participant B as Device B

    Note over A,B: Both devices editing same order

    A->>FS: Update order.status = "processing"<br/>(timestamp T1)
    B->>FS: Update order.status = "cancelled"<br/>(timestamp T2, T2 > T1)

    FS->>FS: Last-write-wins<br/>Final status = "cancelled"

    FS--)A: Snapshot update:<br/>status = "cancelled"
    FS--)B: Snapshot update:<br/>status = "cancelled"

    Note over A,B: Both devices now show<br/>consistent state
```

> [!WARNING]
> Firestore uses a **last-write-wins** strategy for concurrent edits. For the JoyMilk app (2–5 internal users), this is an acceptable trade-off. If stricter conflict resolution is needed in the future, Firestore Transactions can enforce optimistic concurrency control.

### Error Handling Summary

| Error Type | User-Facing Message | System Behavior |
|------------|---------------------|-----------------|
| Network error (write) | "Pesanan disimpan offline" | Firestore caches locally, auto-syncs |
| Network error (read) | "Tidak dapat memuat data" | Show cached data if available |
| Auth token expired | "Sesi berakhir, silakan login kembali" | Redirect to Login |
| Invalid form input | Field-specific validation messages | Prevent submission |
| Firestore permission denied | "Anda tidak memiliki izin" | Log error, show toast |
| Concurrent edit conflict | No explicit message | Last-write-wins, real-time sync resolves |

---

## 10. Workflow Summary Matrix

| Workflow | Primary Actor | Trigger | Key Firestore Operations |
|----------|---------------|---------|--------------------------|
| Authentication | All | App launch / Login | Read `users` doc |
| Order Creation | Staff / Admin | "Buat Pesanan" | Write `orders` + `items` sub-collection |
| Order Status Update | Staff / Admin | Status button tap | Update `orders` doc |
| Add Product | Admin | "Tambah Produk" | Write `products` + `priceHistory` |
| Edit Product | Admin | Product detail edit | Update `products`, conditionally write `priceHistory` |
| Deactivate Product | Admin | Toggle switch | Update `products.isActive` |
| Report Generation | All | Reports screen | Query `orders` + `items` |
| Add User | Admin | "Tambah User" | Firebase Auth + write `users` doc |
| Deactivate User | Admin | Toggle switch | Update `users.isActive` |
| Data Sync | System | Automatic | Real-time listeners + offline cache |
