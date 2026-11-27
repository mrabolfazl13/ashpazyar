package ir.ashpazyar.app.ui.screens.userrecipes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@Composable
fun UserRecipesScreen(
    onBack: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    onEditRecipe: (String) -> Unit,
    onCreate: () -> Unit,
    viewModel: UserRecipesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AshpaTopBar(
            title = "دستورهای من",
            onBack = onBack,
            action = {
                AshpaIconButton(onClick = onCreate) {
                    Icon(Icons.Rounded.Add, "دستور تازه", tint = palette.ink)
                }
            }
        )
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("آشپزخانه شخصی شما", style = MaterialTheme.typography.displaySmall, color = palette.ink)
            Spacer(Modifier.height(5.dp))
            Text(
                "دستورهایی که خودتان نوشته یا از دستورهای آشپزیار کپی کرده‌اید.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted
            )
            Spacer(Modifier.height(14.dp))
            PrimaryButton(
                text = "نوشتن دستور تازه",
                onClick = onCreate,
                icon = Icons.Rounded.Add,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
        }
        if (state.recipes.isEmpty() && !state.loading) {
            EmptyState(
                title = "اولین دستور خودتان را بنویسید",
                description = "طعم خانگی خودتان را ثبت کنید؛ مواد، مراحل و نکته‌های پخت را کنار هم داشته باشید.",
                actionLabel = "ساخت دستور",
                onAction = onCreate,
                icon = Icons.Rounded.MenuBook
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 26.dp)) {
                items(state.recipes, key = { it.id }) { recipe ->
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
                        RecipeCard(
                            recipe = recipe,
                            isFavorite = recipe.id in state.favorites,
                            onClick = { onOpenRecipe(recipe.id) },
                            onFavoriteClick = { viewModel.toggleFavorite(recipe.id) }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, end = 6.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "ویرایش",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.muted,
                                modifier = Modifier
                                    .clickable { onEditRecipe(recipe.id) }
                                    .padding(8.dp)
                            )
                            Text(
                                "حذف",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.danger,
                                modifier = Modifier
                                    .clickable { pendingDelete = recipe.id }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { id ->
        ConfirmDialog(
            title = "حذف دستور",
            message = "این دستور برای همیشه حذف می‌شود. سابقه مصرف مواد در آشپزخانه دست‌نخورده می‌ماند.",
            confirmLabel = "حذف کن",
            destructive = true,
            onConfirm = {
                viewModel.delete(id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}
