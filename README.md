# Glutenfri Oppskrifter

An Android app for managing gluten-free recipes, built with Kotlin and MVVM architecture.

## Features

- **Recipe management** — add, edit, rename and delete recipes
- **Ingredients & steps** — inline editing with drag-to-reorder
- **Categories** — filter recipes by pastry type (Brød, Kaker, Boller, Rundstykker, Scones, etc.)
- **Thickener tabs** — separate views for recipes with (E400–E499) and without thickeners
- **Tips & common mistakes** — freetext tabs per recipe
- **Calendar** — day-by-day notes
- **Export / Import** — share recipes as JSON or PDF
- **Bundled recipes** — pre-loaded recipes restored automatically if deleted

## Tech Stack

- Kotlin
- MVVM (ViewModel + LiveData)
- Room (local database)
- ViewPager2 + TabLayout
- Material Design 3
- KSP (Kotlin Symbol Processing)

## Package Structure

```
no.oslo.torshov.pfb
├── data
│   ├── db          # Room database, DAOs, type converters
│   ├── model       # Entity classes (Recipe, DayNote, RecipeCategory)
│   └── repository  # RecipeRepository, RecipeJsonSerializer, RecipePdfExporter
└── ui
    ├── adapter     # RecyclerView and ViewPager adapters
    ├── fragment    # List, freetext, calendar and day note fragments
    ├── viewmodel   # MainViewModel, RecipeDetailViewModel
    └── (root)      # MainActivity, RecipeDetailActivity
```

## Recipe JSON Format

Recipes can be imported/exported as JSON:

```json
{
  "version": 1,
  "recipes": [
    {
      "name": "Oppskrift navn",
      "category": "Brød",
      "ingredients": ["ingrediens 1", "ingrediens 2"],
      "steps": ["steg 1", "steg 2"],
      "tips": ["tips 1"],
      "commonMistakes": ["vanlig feil 1"]
    }
  ]
}
```

## Bundled Recipes

Recipes in `app/src/main/res/raw/bundled_recipes.json` are automatically loaded on launch. A recipe is only added if no existing recipe has the same name and ingredients, so user edits are never overwritten.

## License

See [LICENSE](LICENSE).
