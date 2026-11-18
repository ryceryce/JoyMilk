# CLAUDE.md — JoyMilk Android App

> Claude-specific coding instructions. Also read `AGENTS.md` for full project rules.
> Adapted from [Ponytail](https://github.com/dietrichgebert/ponytail) lazy senior dev philosophy.

---

## Context

**JoyMilk** is an internal Android app for a soy milk (susu kedelai) business.
It handles order recording, product management with price history, and sales reporting.
2–5 internal users. The app uses Supabase Postgres through Vercel and keeps Firebase Auth. Deployment status is tracked in the implementation plan. UI entirely in **Bahasa Indonesia**.

---

## The Lazy Senior Dev Rules

The best code is the code you never wrote. Solve the whole problem with the least new code.

### Before you write

Read the task and the code it touches. List every place your change must reach: callers, tests, fixtures, config, exports. Check what your change could break: data it would destroy or expose, callers that stop working. That is scope. Extra features are not.

### The smallest complete change

Take the first option that fully works:

1. **Does it need to exist?** Skip features, options, and flexibility nobody asked for. Name them in one line.
2. **Already in this codebase?** A helper, component, repository, util — use it the way the surrounding code does.
3. **Kotlin stdlib or Android platform feature?** Use it, unless the project has its own.
4. **An installed dependency?** Use it. Never add a dependency for a few lines of code.
5. **Can it be one line a reader gets at a glance?** One line.
6. **Otherwise:** the minimum code that works.

Be lazy about the solution, never about the change itself. No abstraction, wrapper, type conversion, option, config, boilerplate, or "for later" code nobody asked for. Deletion beats addition. Keep the structure the codebase already has.

---

## Key Technical Decisions

1. **Price snapshot pattern**: Orders freeze `productName` and `priceAtOrder` at creation time. These are immutable. Reports always use `priceAtOrder`, never `currentPrice`.
2. **Price history auto-tracking**: When admin updates a product price, a Postgres transaction updates `current_price` and inserts its `price_history` row.
3. **Sweet level**: Per order item preference — `Tanpa Gula | Less Sugar | Normal | Extra Sweet`. Does NOT affect price.
4. **Role-based access**: `admin` (full CRUD) vs `staff` (orders + read products). Enforce active profiles and roles at the API, with app-level UI guards.
5. **Money values**: All stored as `Long` (IDR, no decimals). Rp 10.000 → `10000`.
6. **UI language**: Bahasa Indonesia — all labels, messages, buttons.
7. **State management**: `StateFlow` only, never `LiveData`. Use `collectAsStateWithLifecycle()`.

---

## Tech Stack (do not deviate)

| Layer | Technology |
|-------|-----------|
| Language | Kotlin for Android, TypeScript for the Vercel backend |
| UI | Jetpack Compose + Material 3 |
| Navigation | Compose Navigation (single-activity) |
| Architecture | MVVM — Screen → ViewModel → Repository → Vercel API |
| DI | Hilt (`@HiltViewModel`, `@Inject`, `@Module`) |
| Backend | Vercel (Next.js on Node.js), Supabase Postgres and Realtime, Firebase Auth |
| State | `StateFlow` + sealed class `UiState` |
| Build | Gradle Kotlin DSL, version catalog |

---

## When Modifying This Codebase

- Always use Jetpack Compose for UI (no XML layouts)
- Follow MVVM: `Screen` → `ViewModel` → `Repository` → Vercel API
- Use Hilt `@Inject` for all dependencies
- `StateFlow` for reactive state, never `LiveData`
- Handle loading/error/success states in every screen via sealed `UiState`
- Repositories return `Result<T>` from suspend functions
- Currency formatting: use `CurrencyUtils.formatRupiah()`
- Date formatting: use `DateUtils` helpers

---

## Common Patterns

### Adding a new screen
1. Create Screen composable in `ui/{feature}/{Feature}Screen.kt`
2. Create ViewModel with `@HiltViewModel` in `ui/{feature}/{Feature}ViewModel.kt`
3. Add route to `ui/navigation/NavGraph.kt`
4. Add role check if admin-only

### Adding a data operation

Define the API contract and authorize it in the backend. Use a versioned SQL migration when the schema changes. Wire the Android repository through Hilt and return Result<T>.

### ViewModel UiState pattern
```kotlin
sealed interface OrderUiState {
    data object Loading : OrderUiState
    data class Success(val orders: List<Order>) : OrderUiState
    data class Error(val message: String) : OrderUiState
}
```

---

## Business Data

See [database schema](docs/DATABASE_SCHEMA.md) and [API contracts](docs/TRD.md). Reuse the order service across Android, Telegram, and MCP. Follow [the implementation plan](docs/IMPLEMENTATION_PLAN.md) for cutover.

---

## Security & Safety — NEVER cut these

- Validate input and authorization at the API. Restrict business-table grants to server roles.
- Role-based access enforcement (both server and client)
- Data-loss prevention (Postgres transactions for related writes)
- Price snapshot integrity (never mutate priceAtOrder)
- Input validation on all forms

---

## Documentation Sync Rule

> **When any code change affects the structure, data model, workflow, or UI of this app, you MUST also update the corresponding markdown documentation in `docs/`.**

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

