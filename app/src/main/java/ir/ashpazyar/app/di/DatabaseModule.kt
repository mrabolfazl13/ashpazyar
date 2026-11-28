package ir.ashpazyar.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.ashpazyar.app.data.local.AshpazyarDatabase
import ir.ashpazyar.app.data.local.dao.KitchenDao
import ir.ashpazyar.app.data.local.dao.MealPlanDao
import ir.ashpazyar.app.data.local.dao.TimerDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AshpazyarDatabase {
        return Room.databaseBuilder(context, AshpazyarDatabase::class.java, "ashpazyar.db")
            .addMigrations(MIGRATION_1_2)
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE recipes ADD COLUMN region TEXT")
            db.execSQL("ALTER TABLE recipes ADD COLUMN history TEXT")
            db.execSQL("ALTER TABLE recipes ADD COLUMN resultDescription TEXT")
            db.execSQL("ALTER TABLE recipes ADD COLUMN restMinutes INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE recipes ADD COLUMN equipment TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE recipes ADD COLUMN ovenTemperatureC INTEGER")
            db.execSQL("ALTER TABLE recipes ADD COLUMN prepGuide TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE recipes ADD COLUMN servingGuide TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE recipes ADD COLUMN storageGuide TEXT")
            db.execSQL("ALTER TABLE recipes ADD COLUMN troubleshooting TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE recipes ADD COLUMN sourceName TEXT")
            db.execSQL("ALTER TABLE recipes ADD COLUMN sourceUrl TEXT")
            db.execSQL("ALTER TABLE recipes ADD COLUMN license TEXT")
            db.execSQL("ALTER TABLE recipes ADD COLUMN validationStatus TEXT NOT NULL DEFAULT 'REVIEW_REQUIRED'")
            db.execSQL("ALTER TABLE recipe_steps ADD COLUMN ingredientRefs TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE recipe_steps ADD COLUMN equipment TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE recipe_steps ADD COLUMN temperatureC INTEGER")
            db.execSQL("ALTER TABLE recipe_steps ADD COLUMN heatLevel TEXT")
            db.execSQL("ALTER TABLE recipe_steps ADD COLUMN successIndicators TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE recipe_steps ADD COLUMN commonMistakes TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE recipe_steps ADD COLUMN recoveryInstructions TEXT")
            db.execSQL("ALTER TABLE recipe_steps ADD COLUMN safetyNotes TEXT NOT NULL DEFAULT ''")
        }
    }

    @Provides
    fun provideKitchenDao(database: AshpazyarDatabase): KitchenDao = database.kitchenDao()

    @Provides
    fun provideMealPlanDao(database: AshpazyarDatabase): MealPlanDao = database.mealPlanDao()

    @Provides
    fun provideTimerDao(database: AshpazyarDatabase): TimerDao = database.timerDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { context.preferencesDataStoreFile("ashpazyar_settings") }
        )
    }
}
