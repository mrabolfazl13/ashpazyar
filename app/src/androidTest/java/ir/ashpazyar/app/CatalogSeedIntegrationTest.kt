package ir.ashpazyar.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import ir.ashpazyar.app.data.local.AshpazyarDatabase
import ir.ashpazyar.app.data.seed.SeedLoader
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class CatalogSeedIntegrationTest {
    private lateinit var context: Context
    private lateinit var database: AshpazyarDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AshpazyarDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun fullCatalogSeedsAlongsideEditorialRecipes() = runBlocking {
        SeedLoader(context, database).seedIfNeeded(force = true)

        assertEquals(7_790, database.recipeDao().count())
        val sample = database.recipeDao().getWithDetails("rcp-fd98deb11b09e4")
        assertNotNull(sample)
        assertTrue(sample!!.ingredients.isNotEmpty())
        assertTrue(sample.steps.isEmpty())
        assertEquals("Persian_Cooking (train.csv)", sample.recipe.sourceName)
        assertEquals("REVIEW_REQUIRED", sample.recipe.validationStatus)
    }
}
