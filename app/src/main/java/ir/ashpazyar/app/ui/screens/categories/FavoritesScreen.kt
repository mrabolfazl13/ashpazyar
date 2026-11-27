package ir.ashpazyar.app.ui.screens.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AshpaTopBar(title = "محبوب‌ها", onBack = onBack)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("دستورهایی که دوست داشتید", style = MaterialTheme.typography.displaySmall, color = palette.ink)
            Spacer(Modifier.height(5.dp))
            Text(
                PersianText.faDigits(state.recipes.size.toString()) + " دستور نشان‌شده؛ همیشه یک قدم نزدیک‌تر به سفره.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted
            )
        }
        Spacer(Modifier.height(16.dp))
        if (state.recipes.isEmpty() && !state.loading) {
            EmptyState(
                title = "هنوز دستوری نشان نکرده‌اید",
                description = "قلب کوچک کنار هر دستور، آن را برای روزهای بعد در همین‌جا نگه می‌دارد.",
                icon = Icons.Rounded.FavoriteBorder
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(state.recipes, key = { it.id }) { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        isFavorite = true,
                        onClick = { onOpenRecipe(recipe.id) },
                        onFavoriteClick = { viewModel.removeFavorite(recipe.id) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
