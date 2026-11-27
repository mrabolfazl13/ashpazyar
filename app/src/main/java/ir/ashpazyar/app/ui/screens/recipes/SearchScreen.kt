package ir.ashpazyar.app.ui.screens.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowForward, "بازگشت", tint = palette.ink)
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text("چه چیزی می‌خواهید بپزید؟", color = palette.muted) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = palette.muted) },
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.surface,
                    unfocusedContainerColor = palette.surface,
                    focusedBorderColor = palette.forest,
                    unfocusedBorderColor = palette.line,
                    focusedTextColor = palette.ink,
                    unfocusedTextColor = palette.ink,
                    cursorColor = palette.terracotta
                )
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Pill("سریع", state.quickOnly, onClick = viewModel::toggleQuick) }
            item { Pill("گیاهی", state.vegetarianOnly, onClick = viewModel::toggleVegetarian) }
            item { Pill("ایرانی", onClick = { viewModel.setQuery("ایرانی") }) }
            item { Pill("شام", onClick = { viewModel.setQuery("شام") }) }
            item { Pill("صبحانه", onClick = { viewModel.setQuery("صبحانه") }) }
        }
        Spacer(Modifier.height(8.dp))
        if (state.query.isBlank() && !state.quickOnly && !state.vegetarianOnly) {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                        Text("پیشنهادهای پرطرفدار", style = MaterialTheme.typography.titleMedium, color = palette.ink)
                        Text("اگر مطمئن نیستید از کجا شروع کنید", style = MaterialTheme.typography.bodySmall, color = palette.muted)
                    }
                }
                items(state.suggestions, key = { it.id }) { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        isFavorite = recipe.id in state.favorites,
                        onClick = { onOpenRecipe(recipe.id) },
                        onFavoriteClick = { viewModel.toggleFavorite(recipe.id) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        } else {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (state.results.isEmpty()) "نتیجه‌ای پیدا نشد" else PersianText.faDigits(state.results.size.toString()) + " نتیجه",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.muted
                )
                Spacer(Modifier.weight(1f))
                if (state.query.isNotBlank()) {
                    Text(
                        "جست‌وجو در " + PersianText.faDigits(state.totalSearched.toString()) + " دستور",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.terracotta
                    )
                }
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                if (state.results.isEmpty()) {
                    item {
                        EmptyState(
                            title = "چیزی پیدا نشد",
                            description = "املای دیگری را امتحان کنید یا به‌جای نام غذا، یکی از مواد اولیه را جست‌وجو کنید.",
                            icon = Icons.Rounded.Search
                        )
                    }
                } else {
                    items(state.results, key = { it.id }) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            isFavorite = recipe.id in state.favorites,
                            onClick = { onOpenRecipe(recipe.id) },
                            onFavoriteClick = { viewModel.toggleFavorite(recipe.id) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
