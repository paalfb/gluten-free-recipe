# Gluten-Free Recipe

An Android app for managing gluten-free recipes, built with Kotlin and MVVM architecture.

## Features

- **Recipe management** — add, edit, rename and delete recipes
- **Emoji icons** — assign up to 2 emojis per recipe via a visual picker; ignored when sorting alphabetically
- **Ingredients & steps** — inline editing with tap-to-reveal edit/delete buttons and long-press drag-to-reorder; persistent "+ Add" footer row at the bottom of each list
- **Categories** — filter recipes by type (Bread, Flatbread, Cakes, Cookies, Buns, Rolls, Scones, Muffins, Waffles, Pancakes, Pizza, Other); localised based on device language; only categories with matching recipes are shown
- **Stabiliser tabs** — three equal-width tabs labelled under "Konsistensmidler / Stabilisers": ⊕ with E400–E499 thickeners, ⊖ without, and ★ Favourites
- **Favourites** — star any recipe from the recipe detail toolbar; dedicated tab shows only starred recipes; starred recipes show a circle-star icon in the recipe list
- **Erfaringer (Experiences)** — log notes per bake with date; accessible from recipe menu or recipe list icon
- **Calendar** — browse experiences by date; tap a day to see all experiences registered that day; accessible directly from the main toolbar
- **Tips & common mistakes** — freetext tabs per recipe
- **Share (Del…)** — export a filtered selection of recipes as JSON (all, favourites, + thickeners, − thickeners) or as PDF; filenames include date and time (`recipes_share_yyyyMMddTHHmm.json`, `recipes_yyyyMMddTHHmm.pdf`)
- **Sync (Synkroniser…)** — export a full sync JSON including experiences and favourites; importing merges recipes and experiences without duplication; filename includes date and time (`recipes_sync_yyyyMMddTHHmm.json`)
- **Delete all** — clear all recipes at once (with confirmation)
- **Localisation** — Norwegian (Bokmål) and English, based on device language
- **Dark/light mode** — all icons adapt to the system theme via `colorOnSurface`
- **Bundled recipes** — 23 pre-loaded recipes with emojis, sorted by category, restored automatically if deleted

## Tech Stack

- Kotlin
- MVVM (ViewModel + LiveData)
- Room (local database, v8)
- ViewPager2 + TabLayout
- Material Design 3
- KSP (Kotlin Symbol Processing)
- kizitonwose/calendar-view 2.5.0

## Building & Development

### Debug Builds
Debug builds require no additional setup:
```bash
./gradlew installDebug
# or
./gradlew assembleDebug
```

The app uses Android's built-in debug keystore automatically.

### Release Builds (Local Testing)
You can build release APKs locally without signing credentials:
```bash
./gradlew assembleRelease
```

Without a keystore, release builds will be signed with the debug key, making them unsuitable for distribution but fine for local testing.

### Release Builds with Production Credentials
When you're ready to sign with production credentials:

1. Generate or obtain a keystore file (see [Android documentation](https://developer.android.com/studio/publish/app-signing))
2. Create `keystore.properties` in the project root:
   ```properties
   storeFile=/path/to/your/app.keystore
   storePassword=your_keystore_password
   keyAlias=your_key_alias
   keyPassword=your_key_password
   ```
   > **Important**: Never commit `keystore.properties` to git. It's in `.gitignore` for security.

3. Build the release APK (now with production signing):
   ```bash
   ./gradlew assembleRelease
   ```

The gradle configuration automatically detects the keystore file and applies production signing when present.

## Package Structure

```
no.oslo.torshov.pfb
├── data
│   ├── db          # Room database, DAOs, migrations, type converters
│   ├── model       # Entity classes (Recipe, DayNote, RecipeCategory, RecipeExperience)
│   └── repository  # RecipeRepository, RecipeJsonSerializer, RecipePdfExporter
└── ui
    ├── adapter     # RecyclerView and ViewPager adapters
    ├── fragment    # List, freetext, calendar fragments
    ├── viewmodel   # MainViewModel, RecipeDetailViewModel
    └── (root)      # MainActivity, RecipeDetailActivity, ExperiencesActivity,
                    # DateExperiencesActivity, CalendarActivity
```

## JSON Formats

### Share format

Exported by **Del…** — recipes only, no experiences:

```json
{
  "version": 1,
  "recipes": [
    {
      "name": "Recipe name",
      "emoji": "🍞",
      "category": "bread",
      "ingredients": ["ingredient 1", "ingredient 2"],
      "steps": ["step 1", "step 2"],
      "tips": ["tip 1"],
      "commonMistakes": ["common mistake 1"]
    }
  ]
}
```

### Sync format

Exported by **Synkroniser…** — full data including experiences:

```json
{
  "version": 1,
  "sync": true,
  "recipes": [
    {
      "name": "Recipe name",
      "emoji": "🍞",
      "category": "bread",
      "favourite": false,
      "ingredients": ["ingredient 1"],
      "steps": ["step 1"],
      "tips": [],
      "commonMistakes": [],
      "experiences": [
        { "date": "2024-01-15", "note": "Turned out great" }
      ]
    }
  ]
}
```

> **Note:** `category` is stored as a stable English key (e.g. `bread`, `cakes`, `pizza`). Display names are resolved from string resources based on device locale. The `"sync": true` flag distinguishes sync files from regular share files on import.

## Bundled Recipes

Recipes in `app/src/main/res/raw/bundled_recipes.json` are automatically loaded on launch. A recipe is only added if no existing recipe has the same name and ingredients, so user edits are never overwritten.

## License

See [LICENSE](LICENSE).
