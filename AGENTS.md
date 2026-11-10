# AGENTS.md — JoyMilk Android App

> Adapted from [Ponytail](https://github.com/dietrichgebert/ponytail) lazy senior dev philosophy.
> The best code is the code you never wrote.

---

## Identity

You are working on **JoyMilk** — an internal Android app for a soy milk (susu kedelai) business.
It records orders, manages products, tracks price history, and generates sales reports.
2–5 internal users, cloud-synced via Firebase. UI entirely in **Bahasa Indonesia**.

---

## The Lazy Senior Dev Rules

Solve the whole problem with the least new code. End your reply with one or two lines: what you skipped or did not check, and any risk the user must know.

### Before you write

Read the task and the code it touches. List every place your change must reach: callers, tests, fixtures, config, exports. Check what your change could break: data it would destroy or expose, callers that stop working. That is scope. Extra features are not.

### The smallest complete change

Take the first option that fully works:

1. **Does it need to exist?** Skip features, options, and flexibility nobody asked for. Name them in one line. A vague request ("build me X") gets the smallest version that does the core job.
2. **Already in this codebase?** A helper, component, repository, util — use it the way the surrounding code does.
3. **Kotlin stdlib or Android platform feature?** Use it, unless the project has its own. A house composable beats a raw platform widget.
4. **An installed dependency?** Use it. Never add a dependency for a few lines of code.
5. **Can it be one line a reader gets at a glance?** One line.
6. **Otherwise:** the minimum code that works.

**Be lazy about the solution, never about the change itself**: finish every part the task needs, including callers, navigation routes, and ViewModels your change breaks. No abstraction, wrapper, type conversion, option, config, boilerplate, or "for later" code nobody asked for. Deletion beats addition. Keep the structure the codebase already has.

---

## Tech Stack (do not deviate)

| Layer | Technology |
|-------|-----------|
| Language | Kotlin (no Java, ever) |
| UI | Jetpack Compose + Material 3 |
| Navigation | Compose Navigation (single-activity) |
| Architecture | MVVM — Screen → ViewModel → Repository → Firebase |
| DI | Hilt (`@HiltViewModel`, `@Inject`, `@Module`) |
| Backend | Firebase Firestore + Firebase Auth |
| State | `StateFlow` (never `LiveData`) |
| Build | Gradle Kotlin DSL, version catalog (`libs.versions.toml`) |

---

## Project Structure

```
com.joymilk.app/
├── data/model/         # Data classes (User, Product, Order, OrderItem, PriceHistory, SweetLevel)
├── data/repository/    # Firebase operations (AuthRepo, OrderRepo, ProductRepo, ReportRepo, UserRepo)
├── di/                 # Hilt modules (AppModule)
├── ui/auth/            # Login screen + ViewModel
├── ui/dashboard/       # Dashboard screen + ViewModel
├── ui/order/           # Order screens (List, Form, Detail) + ViewModel
├── ui/product/         # Product screens (List, Form, PriceHistory) + ViewModel
├── ui/report/          # Report screen + ViewModel
├── ui/user/            # User management screens + ViewModel
├── ui/components/      # Reusable composables (TopBar, Cards, Chips, Loading)
├── ui/navigation/      # NavGraph with route definitions
├── ui/theme/           # Color, Theme, Type
└── util/               # DateUtils, CurrencyUtils, Constants
```

---

## Critical Business Rules

### Price Snapshot (NEVER break this)
When creating an order, **snapshot** the product's `currentPrice` into the order item's `priceAtOrder`. The order item also snapshots `productName`. These fields are immutable after creation. Reports use `priceAtOrder`, never `currentPrice`.

### Price History Auto-tracking
When admin updates a product's price, a `priceHistory` subcollection entry is auto-created in a Firestore **transaction**:
1. Update `products/{id}.currentPrice`
2. Create `products/{id}/priceHistory/{newId}` with the new price, timestamp, and who changed it

### Sweet Level
Per order item: `Tanpa Gula | Less Sugar | Normal | Extra Sweet`. Preference only — **does not affect price**.

### Money Values
All money stored as `Long` (IDR, no decimals). Rp 10.000 → `10000`. Format for display: `CurrencyUtils.formatRupiah()`.

### Role-Based Access
- **Admin**: Full CRUD on products, users, orders. Manage master data.
- **Staff**: Create/read/update orders, read products. Cannot manage products or users.
- Check role in both Firestore security rules AND app-level navigation/UI guards.

---

## Coding Conventions

- **Package**: `com.joymilk.app`
- **UI language**: Bahasa Indonesia (semua label, pesan, tombol)
- **Naming**: `camelCase` functions/variables, `PascalCase` classes/composables
- **Compose screens**: Named as `{Feature}Screen` (e.g., `OrderListScreen`, `ProductFormScreen`)
- **ViewModels**: `@HiltViewModel`, expose `StateFlow<UiState>`, use sealed class/interface for `UiState`
- **Repositories**: `@Inject constructor`, suspend functions returning `Result<T>`
- **Comments**: Only the _why_ the code cannot show, in one line
- **No XML layouts** — 100% Compose
- **No LiveData** — only `StateFlow` + `collectAsStateWithLifecycle()`

---

## Firestore Collections

```
/users/{userId}             → name, email, role, isActive, createdAt
/products/{productId}       → name, category, currentPrice, unit, isActive, ...
  └─ /priceHistory/{id}     → price, effectiveDate, changedBy
/orders/{orderId}           → customerName, customerPhone?, deliveryAddress?, paymentMethod, status, totalAmount, createdBy, notes?, ...
  └─ /items/{itemId}        → productId, productName↑, priceAtOrder↑, quantity, subtotal, sweetLevel
                               ↑ = snapshot, immutable
```

---

## Security & Safety — NEVER cut these

- Trust-boundary validation (Firestore security rules)
- Role-based access enforcement
- Data-loss prevention (transactions for multi-doc writes)
- Price snapshot integrity
- Input validation on all forms

---

## Documentation Sync Rule

> **When any code change affects the structure, data model, workflow, or UI of this app, you MUST also update the corresponding markdown documentation in `docs/`.**

Documents to keep in sync:

| Document | Update when... |
|----------|---------------|
| `docs/PRD.md` | Features are added, changed, or removed |
| `docs/TRD.md` | Tech stack, architecture, or dependencies change |
| `docs/DATABASE_SCHEMA.md` | Data models, collections, fields, or security rules change |
| `docs/WORKFLOW.md` | User flows, state transitions, or business logic change |
| `docs/UI_UX_RULES.md` | UI components, colors, layouts, or UX patterns change |
| `docs/IMPLEMENTATION_PLAN.md` | New phases, files, or milestones are added |
| `README.md` | Any major project-level change |

**Process:**
1. Make the code change
2. Identify which docs are affected
3. Update those docs in the same commit/changeset
4. In your reply, list which docs you updated and why

---

## Bug Fix Protocol

Before you edit, trace the affected execution path up to its shared root caller. Fix the bug at the source rather than patching each symptom. For non-trivial logic (branches, loops, parsers, money, security), leave one small runnable test or verification step.

---

## Reply Format

End every reply with:
```
⚠️ Skipped: [what you intentionally did not do]
🔍 Risk: [what the user should watch for]
📝 Docs updated: [list of docs updated, or "none needed"]
```

