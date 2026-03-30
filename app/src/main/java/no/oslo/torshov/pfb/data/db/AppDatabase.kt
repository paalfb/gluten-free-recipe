package no.oslo.torshov.pfb.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import no.oslo.torshov.pfb.data.model.DayNote
import no.oslo.torshov.pfb.data.model.Recipe
import no.oslo.torshov.pfb.data.model.RecipeCategory
import no.oslo.torshov.pfb.data.model.RecipeExperience

@Database(entities = [Recipe::class, DayNote::class, RecipeExperience::class], version = 8, exportSchema = false)
@TypeConverters(RecipeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recipeDao(): RecipeDao
    abstract fun dayNoteDao(): DayNoteDao
    abstract fun recipeExperienceDao(): RecipeExperienceDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `day_notes` (`date` TEXT NOT NULL, `text` TEXT NOT NULL, PRIMARY KEY(`date`))"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE recipes ADD COLUMN category TEXT NOT NULL DEFAULT '${RecipeCategory.OTHER}'"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE recipes ADD COLUMN tested INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `recipe_experiences` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `recipeId` INTEGER NOT NULL,
                        `date` TEXT NOT NULL,
                        `note` TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) ON DELETE CASCADE
                    )"""
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_recipe_experiences_recipeId` ON `recipe_experiences` (`recipeId`)"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE recipes SET category = 'bread'     WHERE category = 'Brød'")
                db.execSQL("UPDATE recipes SET category = 'flatbread' WHERE category = 'Flatbrød'")
                db.execSQL("UPDATE recipes SET category = 'cakes'     WHERE category = 'Kaker'")
                db.execSQL("UPDATE recipes SET category = 'cookies'   WHERE category = 'Kjeks'")
                db.execSQL("UPDATE recipes SET category = 'buns'      WHERE category = 'Boller'")
                db.execSQL("UPDATE recipes SET category = 'rolls'     WHERE category = 'Rundstykker'")
                db.execSQL("UPDATE recipes SET category = 'scones'    WHERE category = 'Scones'")
                db.execSQL("UPDATE recipes SET category = 'muffins'   WHERE category = 'Muffins'")
                db.execSQL("UPDATE recipes SET category = 'waffles'   WHERE category = 'Vafler'")
                db.execSQL("UPDATE recipes SET category = 'pancakes'  WHERE category = 'Pannekaker'")
                db.execSQL("UPDATE recipes SET category = 'other'     WHERE category = 'Annet'")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipes ADD COLUMN favourite INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipes ADD COLUMN emoji TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "recipe_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .build().also { INSTANCE = it }
            }
    }
}
