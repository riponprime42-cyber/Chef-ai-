package com.example.data.repository

import com.example.data.api.GeminiRequest
import com.example.data.api.Content
import com.example.data.api.Part
import com.example.data.api.GenerationConfig
import com.example.data.api.RetrofitClient
import com.example.data.api.GeneratedRecipeJson
import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import com.example.BuildConfig

class ChefAiRepository(private val database: ChefAiDatabase) {

    private val recipeDao = database.recipeDao()
    private val profileDao = database.profileDao()
    private val favoriteDao = database.favoriteDao()
    private val transactionDao = database.transactionDao()
    private val shoppingDao = database.shoppingDao()

    // --- Recipes FLOW ---
    val allRecipes: Flow<List<Recipe>> = recipeDao.getAllRecipes()
    val featuredRecipes: Flow<List<Recipe>> = recipeDao.getFeaturedRecipes()
    val trendingRecipes: Flow<List<Recipe>> = recipeDao.getTrendingRecipes()

    fun getRecipesByCategory(category: String): Flow<List<Recipe>> =
        recipeDao.getRecipesByCategory(category)

    fun getRecipeById(id: Int): Flow<Recipe?> =
        recipeDao.getRecipeById(id)

    fun searchRecipes(query: String): Flow<List<Recipe>> =
        recipeDao.searchRecipes(query)

    suspend fun insertRecipe(recipe: Recipe): Long = withContext(Dispatchers.IO) {
        recipeDao.insertRecipe(recipe)
    }

    suspend fun deleteRecipe(id: Int) = withContext(Dispatchers.IO) {
        recipeDao.deleteRecipeById(id)
    }

    // --- Profile Flows ---
    fun getProfile(email: String): Flow<Profile?> =
        profileDao.getProfile(email)

    suspend fun createProfileIfNotExist(email: String, name: String) = withContext(Dispatchers.IO) {
        val existing = profileDao.getProfileSuspend(email)
        if (existing == null) {
            val newProfile = Profile(
                email = email,
                name = name,
                plan = "Free",
                credits = 1000,
                profilePicture = ""
            )
            profileDao.insertProfile(newProfile)

            // Log initial credit grant transaction
            transactionDao.insertTransaction(
                CreditTransaction(
                    userEmail = email,
                    description = "Welcome Reward: 1,000 free credits",
                    amount = 1000
                )
            )
        }
    }

    suspend fun insertProfileDirectly(profile: Profile) = withContext(Dispatchers.IO) {
        profileDao.insertProfile(profile)
    }

    suspend fun adjustCredits(email: String, amount: Int, description: String) = withContext(Dispatchers.IO) {
        val currentProfile = profileDao.getProfileSuspend(email)
        if (currentProfile != null) {
            val updatedCredits = (currentProfile.credits + amount).coerceAtLeast(0)
            profileDao.updateCredits(email, updatedCredits)
            transactionDao.insertTransaction(
                CreditTransaction(
                    userEmail = email,
                    description = description,
                    amount = amount
                )
            )
        }
    }

    suspend fun updatePlan(email: String, plan: String) = withContext(Dispatchers.IO) {
        profileDao.updatePlan(email, plan)
        // Adjust credits based on plan
        val creditBonus = when (plan) {
            "Pro" -> 10000
            "Max" -> 50000
            "Max Plus" -> 999999 // Unlimited simulation representation
            else -> 1000
        }
        val currentProfile = profileDao.getProfileSuspend(email)
        val oldCredits = currentProfile?.credits ?: 0
        profileDao.updateCredits(email, creditBonus)

        transactionDao.insertTransaction(
            CreditTransaction(
                userEmail = email,
                description = "Subscription Plan changed to $plan. Credits adjusted to $creditBonus.",
                amount = creditBonus - oldCredits
            )
        )
    }

    suspend fun updateDarkMode(email: String, isDark: Boolean) = withContext(Dispatchers.IO) {
        profileDao.updateDarkMode(email, isDark)
    }

    suspend fun updateLanguage(email: String, lang: String) = withContext(Dispatchers.IO) {
        profileDao.updateLanguage(email, lang)
    }


    // --- Favorites ---
    fun getFavorites(email: String): Flow<List<Recipe>> =
        favoriteDao.getFavoritesForUser(email)

    fun isFavorite(email: String, recipeId: Int): Flow<Boolean> =
        favoriteDao.isFavorite(email, recipeId)

    suspend fun toggleFavorite(email: String, recipeId: Int) = withContext(Dispatchers.IO) {
        val isFav = favoriteDao.isFavorite(email, recipeId).firstOrNull() ?: false
        if (isFav) {
            favoriteDao.deleteFavorite(email, recipeId)
        } else {
            favoriteDao.insertFavorite(Favorite(userEmail = email, recipeId = recipeId))
        }
    }

    // --- Transactions ---
    fun getTransactions(email: String): Flow<List<CreditTransaction>> =
        transactionDao.getTransactionsForUser(email)


    // --- Shopping List ---
    fun getShoppingList(email: String): Flow<List<ShoppingItem>> =
        shoppingDao.getShoppingItems(email)

    suspend fun addToShoppingList(email: String, recipeId: Int, recipeTitle: String, ingredient: String) = withContext(Dispatchers.IO) {
        shoppingDao.insertShoppingItem(
            ShoppingItem(userEmail = email, recipeId = recipeId, recipeTitle = recipeTitle, ingredient = ingredient)
        )
    }

    suspend fun toggleShoppingItem(id: Int, isChecked: Boolean) = withContext(Dispatchers.IO) {
        shoppingDao.updateShoppingItem(id, isChecked)
    }

    suspend fun deleteShoppingItem(id: Int) = withContext(Dispatchers.IO) {
        shoppingDao.deleteShoppingItem(id)
    }

    suspend fun clearShoppingListForRecipe(email: String, recipeId: Int) = withContext(Dispatchers.IO) {
        shoppingDao.deleteShoppingItemsForRecipe(email, recipeId)
    }

    suspend fun clearAllShoppingItems(email: String) = withContext(Dispatchers.IO) {
        shoppingDao.clearAllShoppingItems(email)
    }


    // --- Gemini AI Recipe Generation ---
    suspend fun generateRecipe(
        email: String,
        ingredients: String,
        cuisine: String,
        dietary: String,
        time: String,
        servings: Int
    ): Recipe = withContext(Dispatchers.IO) {
        // 1. Verify credits beforehand
        val currentProfile = profileDao.getProfileSuspend(email) ?: throw Exception("Profile not found")
        val isUnlimited = currentProfile.plan == "Max Plus"
        if (!isUnlimited && currentProfile.credits < 100) {
            throw Exception("INSUFFICIENT_CREDITS")
        }

        // 2. Query Gemini
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw Exception("GEMINI_KEY_MISSING")
        }

        val prompt = """
            You are ChefAI, an award-winning master culinary creation chef.
            Generate a custom cooking recipe matching the parameters:
            - Available Ingredients: $ingredients
            - Cuisine Style: $cuisine
            - Dietary Constraints: $dietary
            - Ready In Under: $time
            - Serving Size Target: $servings people

            Provide a single complete culinary recipe in strict JSON format. 
            Do NOT include markdown block characters like ```json or ```, only provide valid raw JSON text.
            Your output JSON keys MUST match this exact schema:
            {
              "title": "Title of Recipe",
              "description": "Engaging description of the culinary flavors",
              "cookTimeMin": 15,
              "prepTimeMin": 10,
              "difficulty": "Easy" or "Medium" or "Hard",
              "cuisine": "Cuisine style name",
              "calories": 300,
              "servings": 4,
              "ingredients": "2 chicken breasts | 1 cup washed rice | 1 onion diced | 2 cloves minced garlic | 1 can crushed tomatoes",
              "instructions": "Place pan on medium heat and add oil | Sauté onions and garlic for 3 minutes | Add sliced chicken and cook until brown | Pour in rice and crushed tomatoes | Simmer for 15 minutes",
              "tips": "Serve with warm garlic bread and fresh basil leaves.",
              "alternativeIngredients": "chicken -> tofu | washed rice -> cauliflower rice | crushed tomatoes -> tomato sauce"
            }
            Ensure the instructions are logical and the ingredient measurements are practical.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.7f)
        )

        val apiResponse = RetrofitClient.service.generateContent(apiKey, request)
        val rawText = apiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("Empty response from AI")

        // Clean any markdown wrapper
        val cleanedText = cleanJson(rawText)

        // Parse JSON
        val moshi = RetrofitClient.moshiInstance
        val adapter = moshi.adapter(GeneratedRecipeJson::class.java)
        val generated = adapter.fromJson(cleanedText) ?: throw Exception("Failed to parse recipe")

        // 3. Deduct Credits & Record transaction
        if (!isUnlimited) {
            val updatedCredits = (currentProfile.credits - 100).coerceAtLeast(0)
            profileDao.updateCredits(email, updatedCredits)
            transactionDao.insertTransaction(
                CreditTransaction(
                    userEmail = email,
                    description = "AI generation: ${generated.title}",
                    amount = -100
                )
            )
        } else {
            transactionDao.insertTransaction(
                CreditTransaction(
                    userEmail = email,
                    description = "AI generation (Unlimited Plan): ${generated.title}",
                    amount = 0
                )
            )
        }

        // 4. Save generated recipe into DB so user can search & view it!
        val randomImage = getCategoryPlaceholderImage(generated.cuisine)
        val mappedRecipe = Recipe(
            title = generated.title,
            description = generated.description,
            imageUrl = randomImage,
            cookTimeMin = generated.cookTimeMin,
            prepTimeMin = generated.prepTimeMin,
            difficulty = generated.difficulty,
            cuisine = generated.cuisine,
            calories = generated.calories,
            servings = generated.servings,
            ingredients = generated.ingredients,
            instructions = generated.instructions,
            tips = generated.tips,
            category = "Dinner", // default generated category
            rating = 4.8f,
            reviewCount = 1,
            isAI = true,
            alternativeIngredients = generated.alternativeIngredients
        )

        val insertedId = recipeDao.insertRecipe(mappedRecipe)
        mappedRecipe.copy(id = insertedId.toInt())
    }

    private fun cleanJson(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substringAfter("```json")
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substringAfter("```")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substringBeforeLast("```")
        }
        return cleaned.trim()
    }

    private fun getCategoryPlaceholderImage(cuisine: String): String {
        return "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600"
    }

    suspend fun prepopulateStarterRecipes() = withContext(Dispatchers.IO) {
        // Insert 10 amazing categories starter recipes
        val currentList = recipeDao.getAllRecipes().firstOrNull() ?: emptyList()
        if (currentList.isEmpty()) {
            val starters = listOf(
                Recipe(
                    title = "Fluffy Blueberry Pancakes",
                    description = "Stack of soft, pillowy buttermilk pancakes studded with juicy fresh blueberries, topped with real maple syrup.",
                    imageUrl = "https://images.unsplash.com/photo-1528207776546-365bb710ee93?w=500",
                    cookTimeMin = 15,
                    prepTimeMin = 10,
                    difficulty = "Easy",
                    cuisine = "American",
                    calories = 380,
                    servings = 3,
                    ingredients = "2 cups flour | 2 tsp baking powder | 1/2 tsp salt | 2 tbsp sugar | 2 eggs | 1.5 cups buttermilk | 1/2 cup butter melted | 1 cup fresh blueberries | Maple syrup for serving",
                    instructions = "Whisk dry ingredients together in a large mixing bowl | Whisk wet ingredients in another bowl, then fold into dry mixture until just combined | Fold in half the fresh blueberries gently | Heat a non-stick griddle over medium heat and grease with butter | Pour 1/4 cup batter for each pancake, decorate with extra blueberries | Flip when bubbles rise to the surface, cook until golden brown | Serve hot with maple syrup and extra butter",
                    tips = "Do not overmix the pancake batter or they will become dense instead of light and fluffy.",
                    category = "Breakfast",
                    rating = 4.9f,
                    reviewCount = 312,
                    isFeatured = true,
                    isTrending = false
                ),
                Recipe(
                    title = "Avocado Chicken Salad",
                    description = "Fresh light chicken salad tossed in a creamy dynamic cilantro lime dressing with tender chopped chicken, sweet corn, and ripe avocados.",
                    imageUrl = "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=500",
                    cookTimeMin = 10,
                    prepTimeMin = 15,
                    difficulty = "Easy",
                    cuisine = "Mexican",
                    calories = 420,
                    servings = 4,
                    ingredients = "2 cups cooked chicken breast shredded | 2 ripe avocados cubed | 1 cup canned sweet corn rinsed | 1/2 cup red onion minced | 1/4 cup chopped cilantro | 1 tbsp lime juice | 2 tbsp Greek yogurt | Salt and pepper to taste",
                    instructions = "Combine shredded chicken breast, corn, and minced red onion in a large bowl | Toss in diced avocados gently to avoid smashing them | In a cup, whisk the Greek yogurt, lime juice, cilantro, salt, and pepper | Pour dressing over the salad | Stir gently until salad is fully coated | Chill in the refrigerator for 10 minutes before serving",
                    tips = "Use rotisserie chicken for an incredibly speedy meal prep.",
                    category = "Lunch",
                    rating = 4.7f,
                    reviewCount = 145,
                    isFeatured = false,
                    isTrending = true
                ),
                Recipe(
                    title = "Garlic Butter Lemon Salmon",
                    description = "Perfectly pan-seared salmon fillets bathed in a rich golden garlic butter reduction with a bright squeeze of lemons.",
                    imageUrl = "https://images.unsplash.com/photo-1467003909585-2f8a72700288?w=500",
                    cookTimeMin = 12,
                    prepTimeMin = 8,
                    difficulty = "Medium",
                    cuisine = "French",
                    calories = 490,
                    servings = 2,
                    ingredients = "2 fresh salmon fillets | 3 tbsp quality butter | 4 cloves garlic minced | 3 tbsp fresh lemon juice | 1 tsp lemon zest | 1/2 tsp dried oregano | 1 tbsp chopped fresh parsley | Sea salt and black pepper to taste",
                    instructions = "Pat salmon fillets completely dry with a paper towel and season generously with sea salt and black pepper | Melt 1 tbsp butter in a large skillet over medium-high heat | Place salmon skin-side up and sear for 4-5 minutes until a beautiful golden crust forms | Flip fillets and add the remaining butter, minced garlic, oregano, and lemon zest | Spoon melted garlic butter over the salmon as it cooks for another 4 minutes | Stir in fresh lemon juice and baste salmon once more | Garnish with fresh parsley and lemon slices",
                    tips = "Searing salmon requires a hot skillet. Patting the salmon dry is essential for a crispy crust.",
                    category = "Dinner",
                    rating = 4.8f,
                    reviewCount = 520,
                    isFeatured = true,
                    isTrending = true
                ),
                Recipe(
                    title = "Crispy Baked Garlic Fries",
                    description = "Oven-roasted golden potato wedges tossed with a rich coating of fresh garlic cloves, parsed olive oil, and sharp parmesan cheese.",
                    imageUrl = "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=500",
                    cookTimeMin = 30,
                    prepTimeMin = 10,
                    difficulty = "Easy",
                    cuisine = "American",
                    calories = 290,
                    servings = 4,
                    ingredients = "4 large Russet potatoes sliced into fries | 3 tbsp extra virgin olive oil | 4 cloves fresh garlic minced | 1/2 cup grated Parmesan cheese | 1 tsp garlic powder | 1 tsp salt | 1/2 tsp smoked paprika | Fresh parsley chopped",
                    instructions = "Soak potato wedges in cold water for 15 minutes to draw out excess starch, then pat completely dry | Preheat oven to 425°F (218°C) | Place potatoes in a bowl, toss with olive oil, garlic powder, salt, and smoked paprika | Spread in a single layer on a greased baking sheet | Roast for 20 minutes, flip, and roast for another 10 minutes until golden and crispy | Toss hot fries with fresh minced garlic, parmesan, and fresh chopped parsley",
                    tips = "Soaking the sliced potatoes beforehand is the secret to restaurant-style crispy baked fries.",
                    category = "Snacks",
                    rating = 4.6f,
                    reviewCount = 98,
                    isFeatured = false,
                    isTrending = false
                ),
                Recipe(
                    title = "Easiest Chocolate Lava Cake",
                    description = "Warm, rich chocolate cakes with an irresistible flowy molten fudge center, made in individual ramekins.",
                    imageUrl = "https://images.unsplash.com/photo-1606313564200-e75d5e30476c?w=500",
                    cookTimeMin = 13,
                    prepTimeMin = 12,
                    difficulty = "Medium",
                    cuisine = "French",
                    calories = 540,
                    servings = 2,
                    ingredients = "4 oz high quality dark chocolate | 1/2 cup unsalted butter | 2 whole eggs | 2 egg yolks | 1/4 cup sugar | 2 tbsp flour | Pinch of salt | Powdered sugar and vanilla ice cream for serving",
                    instructions = "Preheat oven to 425°F (218°C) and grease two individual ramekins with butter and dust with cocoa powder | Melt dark chocolate and butter together in a heatproof bowl in the microwave in 30-second bursts | Whisk whole eggs, egg yolks, sugar, and salt in a bowl until pale and thick | Fold chocolate mixture and flour into the eggs gently until completely smooth | Divide batter between prepared ramekins | Bake for 12-14 minutes until the edges are firm but center is soft | Let stand of 1 minute, invert onto plates, garnish with powdered sugar",
                    tips = "Baking timing is crucial. Underbaking will make the cake collapse, overbaking solidifies the center.",
                    category = "Desserts",
                    rating = 4.9f,
                    reviewCount = 280,
                    isFeatured = true,
                    isTrending = true
                ),
                Recipe(
                    title = "Creamy Tomato Basil Penne",
                    description = "Wholesome penne pasta tossed in a robust crushed tomato marinara sauce finished with silk heavy cream and fresh garden sweet basil.",
                    imageUrl = "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=500",
                    cookTimeMin = 15,
                    prepTimeMin = 10,
                    difficulty = "Easy",
                    cuisine = "Italian",
                    calories = 460,
                    servings = 3,
                    ingredients = "12 oz penne pasta | 1 can (14 oz) crushed tomatoes | 3 cloves garlic minced | 1 tbsp olive oil | 1/2 cup heavy cream | 1/4 cup fresh shredded Parmigiano | 1/2 cup fresh basil leaves chopped | Salt and red pepper flakes",
                    instructions = "Bring a large pot of salted water to a boil and cook penne pasta according to package instructions until al dente | Heat olive oil in a skillet over medium heat, sauté minced garlic for 1 minute | Pour in crushed tomatoes and simmer on low for 8 minutes | Stir in heavy cream, half of parmesan, salt, and red pepper flakes, simmer for 2 minutes | Drain pasta and toss directly into skillet with sauce | Add chopped basil, stir well, serve hot topped with rest of parmesan",
                    tips = "Save 1/2 cup of pasta water before draining to thin out the sauce if it becomes too thick.",
                    category = "Vegetarian",
                    rating = 4.8f,
                    reviewCount = 410,
                    isFeatured = false,
                    isTrending = false
                ),
                Recipe(
                    title = "Healthy Chickpea Curry",
                    description = "A warm, protein-packed plant-based curry featuring hearty chickpeas cooked with creamy coconut milk, turmeric, and baby spinach.",
                    imageUrl = "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=500",
                    cookTimeMin = 20,
                    prepTimeMin = 10,
                    difficulty = "Easy",
                    cuisine = "Indian",
                    calories = 310,
                    servings = 4,
                    ingredients = "2 cans chickpeas drained and rinsed | 1 can light coconut milk | 1 cup diced onions | 2 cloves garlic minced | 1 tbsp grated ginger | 1 tbsp yellow curry powder | 1/2 tsp turmeric | 2 cups baby spinach | 1 tbsp coconut oil",
                    instructions = "Heat coconut oil in a deep pot, sauté onions, garlic, and ginger until fragrant and soft | Add curry powder and turmeric, stirring constantly for 30 seconds to roast the spices | Pour in chickpeas and coconut milk, stir well and bring to a simmer | Simmer on medium-low for 15 minutes to allow flavors to meld together | Fold in fresh baby spinach until wilted, salt to taste | Serve over steamed basmati rice with lime wedges",
                    tips = "Gently mash a handful of chickpeas with a wooden spoon in the pot to make the curry extra creamy.",
                    category = "Vegan",
                    rating = 4.7f,
                    reviewCount = 203,
                    isFeatured = true,
                    isTrending = false
                ),
                Recipe(
                    title = "Spicy Garlic Shrimp",
                    description = "Sizzling garlic shrimp sautéed with smoked sweet paprika, butter, and chili oil, served with crusty warm baguettes.",
                    imageUrl = "https://images.unsplash.com/photo-1559742811-82410b451b94?w=500",
                    cookTimeMin = 8,
                    prepTimeMin = 10,
                    difficulty = "Easy",
                    cuisine = "Spanish",
                    calories = 340,
                    servings = 2,
                    ingredients = "1 lb raw large shrimp peeled and deveined | 4 tbsp butter | 6 cloves garlic sliced thin | 1 tsp crushed red pepper flakes | 1/2 tsp smoked paprika | 2 tbsp white wine / broth | Lemon juice and chopped cilantro",
                    instructions = "Season cleaned shrimp with smoked paprika and salt | Melt butter in a large skillet over medium-high heat | Add sliced garlic and red pepper flakes, cooking for 1 minute until garlic is golden | Add shrimp to pan in a single layer, cook for 2 minutes without moving them | Flip shrimp, pour in white wine / broth, simmer for 2 minutes until shrimp are pink and opaque | Squeeze fresh lemon juice on top, toss with chopped cilantro, serve warm with crusty bread",
                    tips = "Shrimp cook incredibly fast. Do not cook them past 4-5 minutes total or they will become rubbery.",
                    category = "Seafood",
                    rating = 4.8f,
                    reviewCount = 190,
                    isFeatured = false,
                    isTrending = true
                ),
                Recipe(
                    title = "Roasted Vegetable Bowl",
                    description = "A colorful healthy bowl of warm quinoa loaded with oven-roasted sweet sweet potatoes, broccoli heads, and creamy tahini lemon glaze.",
                    imageUrl = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=500",
                    cookTimeMin = 25,
                    prepTimeMin = 15,
                    difficulty = "Easy",
                    cuisine = "Mediterranean",
                    calories = 350,
                    servings = 2,
                    ingredients = "1 cup cooked quinoa | 1 large sweet potato diced | 1 cup broccoli florets | 1 red bell pepper sliced | 2 tbsp olive oil | 1/4 cup tahini | 2 tbsp warm water | 1 tbsp maple syrup | Salad greens and cherry tomatoes",
                    instructions = "Preheat oven to 400°F (204°C) | Toss sweet potato, broccoli, and bell pepper with olive oil, salt, and pepper | Bake on a cookie sheet for 20-25 minutes until tender and caramelized | Make dressing by whisking tahini, warm water, lemon juice, maple syrup, and a pinch of salt until smooth | Dynamic assemble bowl: pile quinoa, salad greens, roasted vegetables, and cherry tomatoes | Drizzle rich tahini glaze generously over top before serving",
                    tips = "Ensure all vegetables are diced roughly the same size so they cook evenly in the oven.",
                    category = "Healthy",
                    rating = 4.7f,
                    reviewCount = 112,
                    isFeatured = false,
                    isTrending = false
                ),
                Recipe(
                    title = "10-Minute Egg Fried Rice",
                    description = "Classic homestyle fried rice made on hot high heat with scrambled eggs, sweet peas, carrots, and rich premium light soy sauce.",
                    imageUrl = "https://images.unsplash.com/photo-1603133872878-68550a5e134b?w=500",
                    cookTimeMin = 5,
                    prepTimeMin = 5,
                    difficulty = "Easy",
                    cuisine = "Chinese",
                    calories = 330,
                    servings = 2,
                    ingredients = "2 cups day-old cooked white rice | 2 large eggs whisked | 1/2 cup peas and carrots frozen | 2 green onions sliced | 2 tbsp sesame oil | 2 tbsp soy sauce | 1 clove garlic minced | Salt and pepper to taste",
                    instructions = "Heat 1 tbsp sesame oil in a wok or large skillet over high heat | Add whisked eggs, scramble quickly until soft, then remove from pan immediately | Add remaining sesame oil, cook minced garlic, green onion whites, peas, and carrots for 2 minutes | Add chilled rice, breaking any clumps with a spatula, sauté for 2 minutes | Pour soy sauce over rice, stir-frying on high heat until rice grains are slightly toasted | Stir in scrambled eggs and green onion greens, toss for 30 seconds, serve immediately",
                    tips = "Day-old cold rice is mandatory. Freshly cooked rice has too much moisture and will make the fried rice mushy.",
                    category = "Quick Meals",
                    rating = 4.6f,
                    reviewCount = 422,
                    isFeatured = false,
                    isTrending = false
                )
            )
            recipeDao.insertRecipes(starters)
        }
    }
}
