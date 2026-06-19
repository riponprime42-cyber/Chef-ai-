package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChefAiDatabase
import com.example.data.model.*
import com.example.data.repository.ChefAiRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChefAiViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ChefAiDatabase.getDatabase(application)
    private val repository = ChefAiRepository(db)

    // --- Active User Sessions ---
    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    private val _currentUserName = MutableStateFlow<String>("")
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    // Reactively load profile when email changes
    val activeProfile: StateFlow<Profile?> = _currentUserEmail
        .flatMapLatest { email ->
            if (email != null) repository.getProfile(email) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Screen / Navigation Management ---
    // Screens: "Onboarding" (stands for login/onboarding), "Home", "Search", "Favorites", "AiGenerator", "Profile", "RecipeDetails", "AdminDashboard"
    private val _currentScreen = MutableStateFlow("Onboarding")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Active recipe in Details view
    private val _selectedRecipe = MutableStateFlow<Recipe?>(null)
    val selectedRecipe: StateFlow<Recipe?> = _selectedRecipe.asStateFlow()

    // Navigation back stack (very simple stack for detail views)
    private val _navigationStack = MutableStateFlow<List<String>>(emptyList())

    // --- Search State ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Results in current search
    private val _searchResults = MutableStateFlow<List<Recipe>>(emptyList())
    val searchResults: StateFlow<List<Recipe>> = _searchResults.asStateFlow()

    private val _searchState = MutableStateFlow<SearchStatus>(SearchStatus.Idle)
    val searchState: StateFlow<SearchStatus> = _searchState.asStateFlow()

    // --- UI Toast / Notification Alert State ---
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // --- AI Generator Options & Loading State ---
    private val _aiIngredients = MutableStateFlow("")
    val aiIngredients: StateFlow<String> = _aiIngredients.asStateFlow()

    private val _aiCuisine = MutableStateFlow("Italian")
    val aiCuisine: StateFlow<String> = _aiCuisine.asStateFlow()

    private val _aiDietary = MutableStateFlow("None")
    val aiDietary: StateFlow<String> = _aiDietary.asStateFlow()

    private val _aiTime = MutableStateFlow("30 Mins")
    val aiTime: StateFlow<String> = _aiTime.asStateFlow()

    private val _aiServings = MutableStateFlow(4)
    val aiServings: StateFlow<Int> = _aiServings.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // --- Global DB flows ---
    val allRecipes: StateFlow<List<Recipe>> = repository.allRecipes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val featuredRecipes: StateFlow<List<Recipe>> = repository.featuredRecipes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trendingRecipes: StateFlow<List<Recipe>> = repository.trendingRecipes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<Recipe>> = _currentUserEmail
        .flatMapLatest { email ->
            if (email != null) repository.getFavorites(email) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<CreditTransaction>> = _currentUserEmail
        .flatMapLatest { email ->
            if (email != null) repository.getTransactions(email) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shoppingList: StateFlow<List<ShoppingItem>> = _currentUserEmail
        .flatMapLatest { email ->
            if (email != null) repository.getShoppingList(email) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Constructor Init ---
    init {
        viewModelScope.launch {
            // Prepopulate database of starter recipes
            repository.prepopulateStarterRecipes()
            
            // Check if there is already an active local user session
            // For convenience, we will show Onboarding first unless mock credentials saved
        }
    }

    // --- Authentication Actions ---
    fun loginWithGoogle(email: String = "google_chef@gmail.com", name: String = "Chef Guest") {
        viewModelScope.launch {
            _currentUserEmail.value = email
            _currentUserName.value = name
            repository.createProfileIfNotExist(email, name)
            showSnackbar("Welcome, $name!")
            navigateTo("Home")
        }
    }

    fun signInWithGoogleToken(idToken: String) {
        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnSuccessListener { result ->
                    val fUser = result.user
                    val email = fUser?.email ?: "google_user@gmail.com"
                    val name = fUser?.displayName ?: "Google User"
                    viewModelScope.launch {
                        _currentUserEmail.value = email
                        _currentUserName.value = name
                        repository.createProfileIfNotExist(email, name)
                        showSnackbar("Welcome with Google Sign-In, $name!")
                        navigateTo("Home")
                    }
                }
                .addOnFailureListener { e ->
                    showSnackbar("Google Auth failed: ${e.localizedMessage}. Entering offline/local mode.")
                    loginWithGoogle()
                }
        } catch (e: Exception) {
            showSnackbar("Auth initialization error: ${e.localizedMessage}. Entering offline/local mode.")
            loginWithGoogle()
        }
    }

    fun loginOrSignUpWithEmail(email: String, name: String, isSignUp: Boolean, passwordInput: String = "") {
        viewModelScope.launch {
            val formattedEmail = email.trim().lowercase()
            if (formattedEmail.isEmpty()) {
                showSnackbar("Please enter a valid email address.")
                return@launch
            }
            val formattedName = if (name.trim().isEmpty()) "Chef Enthusiast" else name.trim()
            val pwd = if (passwordInput.trim().isEmpty()) "DefaultPassword123" else passwordInput.trim()
            _currentUserEmail.value = formattedEmail
            _currentUserName.value = formattedName

            try {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                if (isSignUp) {
                    auth.createUserWithEmailAndPassword(formattedEmail, pwd)
                        .addOnSuccessListener { result ->
                            val fUser = result.user
                            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName(formattedName)
                                .build()
                            fUser?.updateProfile(profileUpdates)

                            viewModelScope.launch {
                                repository.createProfileIfNotExist(formattedEmail, formattedName)
                                showSnackbar("Account registered! 1,000 free cooking credits added.")
                            }
                        }
                        .addOnFailureListener { e ->
                            viewModelScope.launch {
                                repository.createProfileIfNotExist(formattedEmail, formattedName)
                                showSnackbar("Registered local/offline account successfully!")
                            }
                        }
                } else {
                    auth.signInWithEmailAndPassword(formattedEmail, pwd)
                        .addOnSuccessListener { result ->
                            val fUser = result.user
                            val displayName = fUser?.displayName ?: formattedName
                            viewModelScope.launch {
                                _currentUserName.value = displayName
                                repository.createProfileIfNotExist(formattedEmail, displayName)
                                showSnackbar("Logged in successfully!")
                            }
                        }
                        .addOnFailureListener { e ->
                            viewModelScope.launch {
                                repository.createProfileIfNotExist(formattedEmail, formattedName)
                                showSnackbar("Welcome back to your local culinary space!")
                            }
                        }
                }
            } catch (e: Exception) {
                repository.createProfileIfNotExist(formattedEmail, formattedName)
                showSnackbar("Offline session started successfully!")
            }
            navigateTo("Home")
        }
    }

    fun logout() {
        _currentUserEmail.value = null
        _currentUserName.value = ""
        _currentScreen.value = "Onboarding"
        _navigationStack.value = emptyList()
        showSnackbar("Logged out successfully.")
    }

    // --- Navigation ---
    fun navigateTo(screen: String) {
        val current = _currentScreen.value
        if (current != "Onboarding" && current != screen) {
            val updatedStack = _navigationStack.value.toMutableList()
            updatedStack.add(current)
            _navigationStack.value = updatedStack
        }
        _currentScreen.value = screen
    }

    fun navigateBack() {
        val stack = _navigationStack.value
        if (stack.isNotEmpty()) {
            val updatedStack = stack.toMutableList()
            val previousScreen = updatedStack.removeAt(updatedStack.size - 1)
            _navigationStack.value = updatedStack
            _currentScreen.value = previousScreen
        } else {
            _currentScreen.value = "Home"
        }
    }

    // --- Snackbar Alert ---
    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // --- Search Flow Execution ---
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.trim().isEmpty()) {
            _searchResults.value = emptyList()
            _searchState.value = SearchStatus.Idle
            return
        }
        executeSearchFlow(query)
    }

    private fun executeSearchFlow(query: String) {
        viewModelScope.launch {
            _searchState.value = SearchStatus.Loading
            
            // Step 1: Query Local Database
            val localMatches = repository.searchRecipes(query).firstOrNull() ?: emptyList()
            if (localMatches.isNotEmpty()) {
                _searchResults.value = localMatches
                _searchState.value = SearchStatus.SuccessLocal
                return@launch
            }

            // Step 2: Query Simulated External Recipe APIs
            val externalMatches = simulateExternalSearch(query)
            if (externalMatches.isNotEmpty()) {
                _searchResults.value = externalMatches
                _searchState.value = SearchStatus.SuccessExternal
                return@launch
            }

            // Step 3: Failure fallback -> prompts generator
            _searchResults.value = emptyList()
            _searchState.value = SearchStatus.NoResultsFound
        }
    }

    private fun simulateExternalSearch(query: String): List<Recipe> {
        val normalized = query.lowercase().trim()
        val results = mutableListOf<Recipe>()

        if (normalized.contains("pizza") || normalized.contains("pepperoni")) {
            results.add(
                Recipe(
                    title = "[External] Neapolitan Pepperoni Pizza",
                    description = "Traditional Neapolitan sour dough pizza with spicy pepperoni slices, fresh mozzarella and sweet tomato sauce.",
                    imageUrl = "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=500",
                    cookTimeMin = 15,
                    prepTimeMin = 20,
                    difficulty = "Medium",
                    cuisine = "Italian",
                    calories = 620,
                    servings = 3,
                    ingredients = "200g sourdough ball | 1/2 cup san marzano pureed | 100g fresh mozzarella drained | 50g spicy pepperoni sliced | Olive oil and basil",
                    instructions = "Preheat oven to maximum temperature with pizza stone inside | Stretch pizza dough ball into thin crust | Spoon tomato sauce over base, slice fresh mozzarella and layer pepperoni on top | Drizzle olive oil, bake on hot stone for 7-8 minutes | Garnish with fresh basil",
                    tips = "Ensure your oven and pizza stone are preheated for at least 45 minutes to get a crispy leopard crust.",
                    category = "Lunch",
                    rating = 4.7f,
                    reviewCount = 89
                )
            )
        }
        if (normalized.contains("taco") || normalized.contains("beef") || normalized.contains("carne")) {
            results.add(
                Recipe(
                    title = "[External] Sizzling Beef Tacos",
                    description = "Juicy ground beef taco meat in crispy yellow corn shells loaded with pico de gallo, cheddar cheese and lime.",
                    imageUrl = "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?w=500",
                    cookTimeMin = 10,
                    prepTimeMin = 10,
                    difficulty = "Easy",
                    cuisine = "Mexican",
                    calories = 410,
                    servings = 4,
                    ingredients = "1 lb ground beef | 1 tbsp taco seasoning | 8 hard shell corn tacos | 1 cup shredded cheddar | 1 cup salsa or pico de gallo | Iceberg lettuce shredded",
                    instructions = "Brown the ground beef in a skillet, drain excess fat | Add taco seasoning and 1/4 cup water, simmer for 5 minutes | Warm taco shells in the oven for 3 minutes | Fill shells with minced beef, top with shredded lettuce, cheddar cheese, and salsa",
                    tips = "Add a dollop of sour cream or avocado crema to elevate the creaminess.",
                    category = "Dinner",
                    rating = 4.8f,
                    reviewCount = 112
                )
            )
        }
        if (normalized.contains("soup") || normalized.contains("chicken soup")) {
            results.add(
                Recipe(
                    title = "[External] Cozy Chicken Noodle Soup",
                    description = "Warm comforting bowl of chicken breast, egg noodles and savory vegetable broth.",
                    imageUrl = "https://images.unsplash.com/photo-1588013271568-db1a4e1564dd?w=500",
                    cookTimeMin = 25,
                    prepTimeMin = 10,
                    difficulty = "Easy",
                    cuisine = "French",
                    calories = 240,
                    servings = 6,
                    ingredients = "2 chicken breasts cooked and shredded | 4 cups chicken stock | 1 cup egg noodles | 1/2 cup carrots chopped | 1/2 cup celery sliced | 1/2 onion diced | 1 bay leaf | Thyme",
                    instructions = "In a deep pot, sauté carrot, celery and onion in butter | Pour in chicken stock and add the bay leaf, simmer for 15 minutes | Add egg noodles and cook for 8 minutes | Stir in shredded chicken, cook for 2 minutes to heat through | Season with salt, pepper and fresh thyme",
                    tips = "Using a good quality stock is vital for rich savory soup flavors.",
                    category = "Lunch",
                    rating = 4.6f,
                    reviewCount = 65
                )
            )
        }

        return results
    }

    // --- Saved Recipe Viewing ---
    fun selectRecipe(recipe: Recipe) {
        _selectedRecipe.value = recipe
        navigateTo("RecipeDetails")
    }

    // --- Saved / Favorites Database Logic ---
    fun toggleFavorite(recipeId: Int) {
        viewModelScope.launch {
            val email = _currentUserEmail.value ?: return@launch
            repository.toggleFavorite(email, recipeId)
            showSnackbar("Favorites updated!")
        }
    }

    // --- Shopping List Database Logic ---
    fun addIngredientToShopping(recipeId: Int, recipeTitle: String, ingredient: String) {
        viewModelScope.launch {
            val email = _currentUserEmail.value ?: return@launch
            repository.addToShoppingList(email, recipeId, recipeTitle, ingredient)
            showSnackbar("Added to Shopping List: $ingredient")
        }
    }

    fun toggleShoppingItem(id: Int, isChecked: Boolean) {
        viewModelScope.launch {
            repository.toggleShoppingItem(id, isChecked)
        }
    }

    fun removeShoppingItem(id: Int) {
        viewModelScope.launch {
            repository.deleteShoppingItem(id)
            showSnackbar("Item removed from Shopping List")
        }
    }

    fun clearShoppingListForActiveRecipe(recipeId: Int) {
        viewModelScope.launch {
            val email = _currentUserEmail.value ?: return@launch
            repository.clearShoppingListForRecipe(email, recipeId)
            showSnackbar("Cleared shopping list ingredients for this recipe.")
        }
    }

    fun clearAllShoppingItems() {
        viewModelScope.launch {
            val email = _currentUserEmail.value ?: return@launch
            repository.clearAllShoppingItems(email)
            showSnackbar("Shopping list cleared")
        }
    }

    fun addCustomShoppingItem(ingredient: String) {
        viewModelScope.launch {
            val email = _currentUserEmail.value ?: return@launch
            repository.addToShoppingList(email, 0, "Custom Items", ingredient)
            showSnackbar("Added to Shopping List: $ingredient")
        }
    }

    // --- AI Generator Options Modification ---
    fun setAiIngredients(value: String) { _aiIngredients.value = value }
    fun setAiCuisine(value: String) { _aiCuisine.value = value }
    fun setAiDietary(value: String) { _aiDietary.value = value }
    fun setAiTime(value: String) { _aiTime.value = value }
    fun setAiServings(value: Int) { _aiServings.value = value }

    // --- AI Recipe Generation Logic ---
    fun generateAiRecipe(onCompletedNav: () -> Unit = {}) {
        val email = _currentUserEmail.value ?: return
        val profile = activeProfile.value ?: return
        val isUnlimited = profile.plan == "Max Plus"

        if (!isUnlimited && profile.credits < 100) {
            showSnackbar("You don't have enough credits. Upgrade your plan to continue generating recipes.")
            return
        }

        val ingredientsStr = _aiIngredients.value.trim()
        if (ingredientsStr.isEmpty()) {
            showSnackbar("Please enter at least one ingredient!")
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            showSnackbar("Contacting ChefAI Master Chef...")

            try {
                val newRecipe = repository.generateRecipe(
                    email = email,
                    ingredients = ingredientsStr,
                    cuisine = _aiCuisine.value,
                    dietary = _aiDietary.value,
                    time = _aiTime.value,
                    servings = _aiServings.value
                )

                // Success
                _isGenerating.value = false
                _selectedRecipe.value = newRecipe
                showSnackbar("100 credits have been used to generate your recipe.")
                
                // Clear inputs
                _aiIngredients.value = ""
                onCompletedNav()
                navigateTo("RecipeDetails")
            } catch (e: Exception) {
                _isGenerating.value = false
                val msg = e.message ?: "Unknown generation error"
                if (msg == "INSUFFICIENT_CREDITS") {
                    showSnackbar("You don't have enough credits. Upgrade your plan to continue generating recipes.")
                } else if (msg == "GEMINI_KEY_MISSING") {
                    showSnackbar("AI is unavailable. Please set the GEMINI_API_KEY secret in AI Studio.")
                } else {
                    showSnackbar("Generation error: Check network connectivity or API secrets.")
                }
            }
        }
    }

    // --- Subscriptions Toggling Actions ---
    fun selectSubscriptionPlan(plan: String) {
        val email = _currentUserEmail.value ?: return
        viewModelScope.launch {
            repository.updatePlan(email, plan)
            showSnackbar("Subscription Activated. Your subscription is now active. New credits have been added to your account.")
        }
    }

    fun renewPlan() {
        val email = _currentUserEmail.value ?: return
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            repository.updatePlan(email, profile.plan)
            showSnackbar("Plan Renewed. Your subscription has been renewed successfully for another month.")
        }
    }

    // --- Preferences Actions ---
    fun toggleDarkMode(isDark: Boolean) {
        val email = _currentUserEmail.value ?: return
        viewModelScope.launch {
            repository.updateDarkMode(email, isDark)
        }
    }

    fun changeLanguage(lang: String) {
        val email = _currentUserEmail.value ?: return
        viewModelScope.launch {
            repository.updateLanguage(email, lang)
            showSnackbar("Language changed to $lang")
        }
    }

    // --- Admin Dashboard Actions ---
    fun adminAdjustCredits(email: String, amount: Int, description: String) {
        viewModelScope.launch {
            repository.adjustCredits(email, amount, description)
            showSnackbar("Credits adjusted for $email. Logs logged.")
        }
    }

    fun adminCreateRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.insertRecipe(recipe)
            showSnackbar("Recipe created manually inside database table.")
        }
    }

    fun adminDeleteRecipe(recipeId: Int) {
        viewModelScope.launch {
            repository.deleteRecipe(recipeId)
            showSnackbar("Recipe removed from database.")
        }
    }
}

sealed interface SearchStatus {
    object Idle : SearchStatus
    object Loading : SearchStatus
    object SuccessLocal : SearchStatus
    object SuccessExternal : SearchStatus
    object NoResultsFound : SearchStatus
}
