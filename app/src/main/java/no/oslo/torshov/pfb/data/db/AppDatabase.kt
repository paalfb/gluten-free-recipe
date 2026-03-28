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

@Database(entities = [Recipe::class, DayNote::class], version = 3, exportSchema = false)
@TypeConverters(RecipeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recipeDao(): RecipeDao
    abstract fun dayNoteDao(): DayNoteDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `day_notes` (`date` TEXT NOT NULL, `text` TEXT NOT NULL, PRIMARY KEY(`date`))"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE recipes ADD COLUMN category TEXT NOT NULL DEFAULT '${RecipeCategory.OTHER}'"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "recipe_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { INSTANCE = it }
            }
    }
}
