# Copilot Instructions

## Build Commands

```bash
./gradlew assembleDebug        # Build debug APK
./gradlew assembleRelease      # Build release APK
./gradlew installDebug         # Build and install on connected device
```

Release signing reads from `keystore.properties` (gitignored); falls back to debug keystore if absent.

There are no automated tests. The test directory exists but is empty.

## Architecture

Single-module Android app (MVVM + Repository pattern) with a View-based UI (no Jetpack Compose).

```
UI (Activities / Fragments)
  → ViewModel (LiveData + viewModelScope)
    → RecipeRepository
      → Room DAOs
        → SQLite (Room v2.6.1, schema version 8)
```

**No DI framework.** `AppDatabase` is a manually-managed singleton:
```kotlin
AppDatabase.getInstance(application).recipeDao()
```
ViewModels extend `AndroidViewModel` and instantiate the repository directly.

**Navigation** is Intent-based — no Navigation Component. Activities communicate via `Intent.putExtra()`.

**State** is exposed as `LiveData` from ViewModels and observed in Activities/Fragments. All database operations are `suspend` functions called via `viewModelScope.launch {}`.

The main recipe list uses a `MediatorLiveData<List<Recipe>>` (`filteredRecipes`) that combines three independent filter states — `thickenerFilter: MutableLiveData<ThickenerFilter>`, `categoryFilter: MutableLiveData<String?>`, and `favouritesOnly: MutableLiveData<Boolean>` — with the base `_recipes` list. Filters are set directly on the ViewModel by `MainRecipeListFragment`; the MediatorLiveData recomputes automatically when any source changes.

## Key Conventions

**Package structure:**
```
no.oslo.torshov.pfb
├── data/
│   ├── db/          # Room entities, DAOs, AppDatabase singleton, migrations
│   ├── model/       # Data classes (Recipe, RecipeCategory, DayNote, …)
│   └── repository/  # RecipeRepository, RecipeJsonSerializer, RecipePdfExporter
└── ui/
    ├── adapter/     # RecyclerView / ViewPager adapters
    ├── fragment/    # Fragments and BottomSheets
    ├── viewmodel/   # ViewModels
    └── (activities) # Activity classes at ui/ root
```

**LiveData naming:** private `MutableLiveData` is prefixed with `_`; the public read-only `LiveData` drops the prefix.

**Database migrations** must be added to `AppDatabase` whenever columns or tables change. All 7 existing migrations are preserved in code — follow that pattern.

**Thickener detection** uses the regex `[Ee]4\d\d` (E400–E499 food additives). Recipes are split into "with thickeners" / "without thickeners" tabs in `MainViewModel` using `.map {}` on a single LiveData.

**JSON serialization** uses `org.json` (not Gson or kotlinx.serialization). Two formats exist:
- *Share* — recipes only
- *Sync* — recipes + experiences, identified by `"sync": true` in the root object

Import deduplication checks `name + ingredients.sorted()`.

**Localization:** English (`values/` and `values-en/`) and Norwegian Bokmål (`values-nb/`). Always use string resources — no hardcoded UI strings. Category display names are resolved via `RecipeCategory.displayName(context, key)` to support legacy Norwegian keys stored before migration 5→6.

**UI components:** Material Design 3 throughout. Use `MaterialAlertDialogBuilder` for dialogs, `BottomSheetDialogFragment` for quick-edit sheets, and `TabLayoutMediator` when binding tabs to a `ViewPager2`.

**Bundled recipes** are loaded from `res/raw/bundled_recipes.json` on first launch via `MainViewModel.loadBundledRecipes()`. The merge logic skips recipes that already exist by name + ingredients — preserve this deduplication when modifying import logic.

## Tech Stack Highlights

- Kotlin 1.9.23 / AGP 8.3.2 / Java 17
- minSdk 26 / compileSdk + targetSdk 35
- Room 2.6.1 (KSP for code generation)
- Lifecycle / LiveData / ViewModel 2.7.0
- Coroutines 1.7.3
- Material Design 3 (1.11.0)
- ViewPager2, RecyclerView, ConstraintLayout
- kizitonwose/calendar-view 2.5.0
