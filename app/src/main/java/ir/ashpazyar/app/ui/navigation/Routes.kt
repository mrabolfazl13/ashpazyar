package ir.ashpazyar.app.ui.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val RECIPES = "recipes"
    const val RECIPE = "recipe/{recipeId}"
    const val COOKING = "cooking/{recipeId}"
    const val SEARCH = "search"
    const val CATEGORIES = "categories"
    const val CATEGORY = "category/{categoryId}"
    const val FAVORITES = "favorites"
    const val SMART_KITCHEN = "smart"
    const val PANTRY = "pantry"
    const val PANTRY_ADD = "pantry/add"
    const val PLANNER = "planner"
    const val PLAN_GENERATOR = "planner/generate"
    const val SHOPPING = "shopping"
    const val SHOPPING_HISTORY = "shopping/history"
    const val COST = "cost"
    const val LEFTOVERS = "leftovers"
    const val PREFERENCES = "preferences"
    const val SETTINGS = "settings"
    const val BACKUP = "backup"
    const val TIPS = "tips"
    const val TIP = "tips/{tipId}"
    const val CONVERTER = "converter"
    const val USER_RECIPES = "my-recipes"
    const val RECIPE_EDITOR = "recipe-editor?recipeId={recipeId}"
    const val ABOUT = "about"
    const val TIMERS = "timers"

    fun recipe(id: String) = "recipe/" + id
    fun cooking(id: String) = "cooking/" + id
    fun category(id: String) = "category/" + id
    fun tip(id: String) = "tips/" + id
    fun recipeEditor(id: String? = null) = if (id == null) "recipe-editor" else "recipe-editor?recipeId=" + id

    val bottomTabs = listOf(HOME, RECIPES, SMART_KITCHEN, PLANNER, SHOPPING)

    fun isBottomTab(route: String?): Boolean = route in bottomTabs
}
