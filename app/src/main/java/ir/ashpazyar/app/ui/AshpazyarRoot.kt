package ir.ashpazyar.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ir.ashpazyar.app.di.StartupViewModel
import ir.ashpazyar.app.ui.components.PrimaryButton
import ir.ashpazyar.app.ui.navigation.AshpaBottomBar
import ir.ashpazyar.app.ui.navigation.Routes
import ir.ashpazyar.app.ui.screens.about.AboutScreen
import ir.ashpazyar.app.ui.screens.backup.BackupScreen
import ir.ashpazyar.app.ui.screens.categories.CategoriesScreen
import ir.ashpazyar.app.ui.screens.favorites.FavoritesScreen
import ir.ashpazyar.app.ui.screens.converter.ConverterScreen
import ir.ashpazyar.app.ui.screens.cooking.CookingModeScreen
import ir.ashpazyar.app.ui.screens.cost.CostScreen
import ir.ashpazyar.app.ui.screens.home.HomeScreen
import ir.ashpazyar.app.ui.screens.leftovers.LeftoversScreen
import ir.ashpazyar.app.ui.screens.onboarding.OnboardingScreen
import ir.ashpazyar.app.ui.screens.pantry.AddPantryScreen
import ir.ashpazyar.app.ui.screens.pantry.PantryScreen
import ir.ashpazyar.app.ui.screens.planner.PlanGeneratorScreen
import ir.ashpazyar.app.ui.screens.planner.PlannerScreen
import ir.ashpazyar.app.ui.screens.preferences.PreferencesScreen
import ir.ashpazyar.app.ui.screens.recipeeditor.RecipeEditorScreen
import ir.ashpazyar.app.ui.screens.recipedetail.RecipeDetailScreen
import ir.ashpazyar.app.ui.screens.recipes.RecipesScreen
import ir.ashpazyar.app.ui.screens.recipes.SearchScreen
import ir.ashpazyar.app.ui.screens.settings.SettingsScreen
import ir.ashpazyar.app.ui.screens.shopping.ShoppingHistoryScreen
import ir.ashpazyar.app.ui.screens.shopping.ShoppingScreen
import ir.ashpazyar.app.ui.screens.smart.SmartKitchenScreen
import ir.ashpazyar.app.ui.screens.timers.TimersScreen
import ir.ashpazyar.app.ui.screens.tips.TipDetailScreen
import ir.ashpazyar.app.ui.screens.tips.TipsScreen
import ir.ashpazyar.app.ui.screens.update.UpdateOverlay
import ir.ashpazyar.app.ui.screens.update.UpdatePhase
import ir.ashpazyar.app.ui.screens.update.UpdateViewModel
import ir.ashpazyar.app.ui.screens.userrecipes.UserRecipesScreen
import ir.ashpazyar.app.ui.theme.AshpaTheme
import ir.ashpazyar.app.ui.theme.AshpazyarTheme

@Composable
fun AshpazyarRoot(startup: StartupViewModel = hiltViewModel()) {
    val seeded by startup.seeded.collectAsStateWithLifecycle()
    val error by startup.error.collectAsStateWithLifecycle()
    val settings by startup.settings.collectAsStateWithLifecycle()
    val dark = when (settings?.themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    AshpazyarTheme(darkTheme = dark) {
        when {
            !seeded -> StartupGate(error = error, onRetry = startup::seed)
            settings == null -> StartupGate(error = null, onRetry = {})
            !settings!!.onboardingCompleted -> OnboardingScreen(onFinished = {})
            else -> MainNavigation()
        }
    }
}

@Composable
private fun StartupGate(error: String?, onRetry: () -> Unit) {
    val palette = AshpaTheme.palette
    Box(
        Modifier.fillMaxSize().background(palette.background).padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(104.dp).clip(RoundedCornerShape(30.dp)).background(palette.forest),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Restaurant, null, tint = Color(0xFFE6B88C), modifier = Modifier.size(50.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text("چاشیکا", style = MaterialTheme.typography.displaySmall, color = palette.ink)
            Spacer(Modifier.height(7.dp))
            Text("آشپزخانه‌تان را آماده می‌کنیم…", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
            Spacer(Modifier.height(20.dp))
            if (error != null) {
                Text(error, style = MaterialTheme.typography.bodySmall, color = palette.danger, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                PrimaryButton("تلاش دوباره", onClick = onRetry)
            } else {
                CircularProgressIndicator(color = palette.terracotta, strokeWidth = 3.dp)
            }
        }
    }
}

@Composable
private fun MainNavigation(updateViewModel: UpdateViewModel = hiltViewModel()) {
    val updateState by updateViewModel.state.collectAsStateWithLifecycle()
    if (updateState.phase == UpdatePhase.FORCED) {
        UpdateOverlay(
            state = updateState,
            onRetry = { updateViewModel.check(manual = true) },
            onDismiss = {},
            onOpenBazaar = updateViewModel::openBazaar,
            onCloseMessage = updateViewModel::closeMessage
        )
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val goTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val navigate: (String) -> Unit = { navController.navigate(it) }

    Scaffold(
        containerColor = AshpaTheme.palette.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (Routes.isBottomTab(currentRoute)) {
                AshpaBottomBar(currentRoute = currentRoute, onNavigate = goTab)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenRecipe = { navigate(Routes.recipe(it)) },
                    onOpenRecipes = { goTab(Routes.RECIPES) },
                    onOpenSearch = { navigate(Routes.SEARCH) },
                    onOpenCategories = { navigate(Routes.CATEGORIES) },
                    onOpenCategory = { navigate(Routes.category(it)) },
                    onOpenSmart = { goTab(Routes.SMART_KITCHEN) },
                    onOpenSettings = { navigate(Routes.SETTINGS) },
                    onOpenPlanner = { goTab(Routes.PLANNER) },
                    onOpenShopping = { goTab(Routes.SHOPPING) },
                    onOpenTimers = { navigate(Routes.TIMERS) },
                    onOpenFavorites = { navigate(Routes.FAVORITES) }
                )
            }
            composable(Routes.RECIPES) {
                RecipesScreen(onOpenRecipe = { navigate(Routes.recipe(it)) })
            }
            composable(
                route = Routes.RECIPE,
                arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
            ) {
                RecipeDetailScreen(
                    onBack = { navController.popBackStack() },
                    onStartCooking = { navigate(Routes.cooking(it)) },
                    onEditRecipe = { navigate(Routes.recipeEditor(it)) },
                    onDeleted = { navController.popBackStack() },
                    onOpenCategory = { navigate(Routes.category(it)) }
                )
            }
            composable(
                route = Routes.COOKING,
                arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
            ) {
                CookingModeScreen(
                    onExit = { navController.popBackStack() },
                    onOpenTimers = { navigate(Routes.TIMERS) }
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(onBack = { navController.popBackStack() }, onOpenRecipe = { navigate(Routes.recipe(it)) })
            }
            composable(Routes.CATEGORIES) {
                CategoriesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCategory = { navigate(Routes.category(it)) },
                    onSearch = { navigate(Routes.SEARCH) }
                )
            }
            composable(
                route = Routes.CATEGORY,
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
            ) { entry ->
                val categoryId = entry.arguments?.getString("categoryId")
                RecipesScreen(
                    onOpenRecipe = { navigate(Routes.recipe(it)) },
                    onBack = { navController.popBackStack() },
                    initialCategoryId = categoryId,
                    categoryTitle = null
                )
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(onBack = { navController.popBackStack() }, onOpenRecipe = { navigate(Routes.recipe(it)) })
            }
            composable(Routes.SMART_KITCHEN) {
                SmartKitchenScreen(
                    onOpenRecipe = { navigate(Routes.recipe(it)) },
                    onOpenPantry = { navigate(Routes.PANTRY) },
                    onOpenShopping = { goTab(Routes.SHOPPING) },
                    onOpenTimers = { navigate(Routes.TIMERS) }
                )
            }
            composable(Routes.PANTRY) {
                PantryScreen(
                    onBack = { navController.popBackStack() },
                    onAdd = { navigate(Routes.PANTRY_ADD) },
                    onOpenShopping = { goTab(Routes.SHOPPING) },
                    onOpenSmart = { goTab(Routes.SMART_KITCHEN) }
                )
            }
            composable(Routes.PANTRY_ADD) {
                AddPantryScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.PLANNER) {
                PlannerScreen(
                    onOpenRecipe = { navigate(Routes.recipe(it)) },
                    onOpenGenerator = { navigate(Routes.PLAN_GENERATOR) },
                    onOpenShopping = { goTab(Routes.SHOPPING) }
                )
            }
            composable(Routes.PLAN_GENERATOR) {
                PlanGeneratorScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(Routes.SHOPPING) {
                ShoppingScreen(onOpenHistory = { navigate(Routes.SHOPPING_HISTORY) })
            }
            composable(Routes.SHOPPING_HISTORY) {
                ShoppingHistoryScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.COST) {
                CostScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.LEFTOVERS) {
                LeftoversScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.PREFERENCES) {
                PreferencesScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPreferences = { navigate(Routes.PREFERENCES) },
                    onOpenBackup = { navigate(Routes.BACKUP) },
                    onOpenTimers = { navigate(Routes.TIMERS) },
                    onOpenTips = { navigate(Routes.TIPS) },
                    onOpenConverter = { navigate(Routes.CONVERTER) },
                    onOpenAbout = { navigate(Routes.ABOUT) },
                    onOpenUserRecipes = { navigate(Routes.USER_RECIPES) },
                    onOpenCost = { navigate(Routes.COST) },
                    onOpenLeftovers = { navigate(Routes.LEFTOVERS) },
                    onCheckUpdate = { updateViewModel.check(manual = true) }
                )
            }
            composable(Routes.BACKUP) {
                BackupScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.TIPS) {
                TipsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTip = { navigate(Routes.tip(it)) }
                )
            }
            composable(
                route = Routes.TIP,
                arguments = listOf(navArgument("tipId") { type = NavType.StringType })
            ) {
                TipDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.CONVERTER) {
                ConverterScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.USER_RECIPES) {
                UserRecipesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRecipe = { navigate(Routes.recipe(it)) },
                    onEditRecipe = { navigate(Routes.recipeEditor(it)) },
                    onCreate = { navigate(Routes.recipeEditor()) }
                )
            }
            composable(
                route = Routes.RECIPE_EDITOR,
                arguments = listOf(
                    navArgument("recipeId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                RecipeEditorScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(Routes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.TIMERS) {
                TimersScreen(onBack = { navController.popBackStack() })
            }
        }
    }

    UpdateOverlay(
        state = updateState,
        onRetry = { updateViewModel.check(manual = true) },
        onDismiss = updateViewModel::dismissOptional,
        onOpenBazaar = updateViewModel::openBazaar,
        onCloseMessage = updateViewModel::closeMessage
    )
}
