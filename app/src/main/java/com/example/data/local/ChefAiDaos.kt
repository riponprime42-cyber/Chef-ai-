package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY id DESC")
    fun getAllRecipes(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE category = :category")
    fun getRecipesByCategory(category: String): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE isFeatured = 1")
    fun getFeaturedRecipes(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE isTrending = 1")
    fun getTrendingRecipes(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    fun getRecipeById(id: Int): Flow<Recipe?>

    @Query("SELECT * FROM recipes WHERE title LIKE '%' || :query || '%' OR ingredients LIKE '%' || :query || '%' OR cuisine LIKE '%' || :query || '%'")
    fun searchRecipes(query: String): Flow<List<Recipe>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<Recipe>)

    @Delete
    suspend fun deleteRecipe(recipe: Recipe)
    
    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipeById(id: Int)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE email = :email LIMIT 1")
    fun getProfile(email: String): Flow<Profile?>

    @Query("SELECT * FROM profiles WHERE email = :email LIMIT 1")
    suspend fun getProfileSuspend(email: String): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile)

    @Query("UPDATE profiles SET credits = :newCredits WHERE email = :email")
    suspend fun updateCredits(email: String, newCredits: Int)

    @Query("UPDATE profiles SET plan = :newPlan WHERE email = :email")
    suspend fun updatePlan(email: String, newPlan: String)

    @Query("UPDATE profiles SET isDarkMode = :isDark WHERE email = :email")
    suspend fun updateDarkMode(email: String, isDark: Boolean)

    @Query("UPDATE profiles SET language = :lang WHERE email = :email")
    suspend fun updateLanguage(email: String, lang: String)
}

@Dao
interface FavoriteDao {
    @Query("SELECT r.* FROM recipes r INNER JOIN favorites f ON r.id = f.recipeId WHERE f.userEmail = :email")
    fun getFavoritesForUser(email: String): Flow<List<Recipe>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userEmail = :email AND recipeId = :recipeId LIMIT 1)")
    fun isFavorite(email: String, recipeId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE userEmail = :email AND recipeId = :recipeId")
    suspend fun deleteFavorite(email: String, recipeId: Int)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userEmail = :email ORDER BY timestamp DESC")
    fun getTransactionsForUser(email: String): Flow<List<CreditTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CreditTransaction)
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_items WHERE userEmail = :email ORDER BY id DESC")
    fun getShoppingItems(email: String): Flow<List<ShoppingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItem)

    @Query("UPDATE shopping_items SET isChecked = :isChecked WHERE id = :id")
    suspend fun updateShoppingItem(id: Int, isChecked: Boolean)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteShoppingItem(id: Int)

    @Query("DELETE FROM shopping_items WHERE userEmail = :email")
    suspend fun clearAllShoppingItems(email: String)

    @Query("DELETE FROM shopping_items WHERE userEmail = :email AND recipeId = :recipeId")
    suspend fun deleteShoppingItemsForRecipe(email: String, recipeId: Int)
}
