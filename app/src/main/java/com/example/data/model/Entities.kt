package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val imageUrl: String,
    val cookTimeMin: Int,
    val prepTimeMin: Int,
    val difficulty: String, // "Easy", "Medium", "Hard"
    val cuisine: String, // "Italian", "Mexican", "Japanese", "Indian", "American", "French", "Chinese"
    val calories: Int,
    val servings: Int,
    val ingredients: String, // Pipe-separated (|) or comma-separated list
    val instructions: String, // Pipe-separated (|) list of directions
    val tips: String = "", // Additional cooking tips
    val category: String, // Breakfast, Lunch, Dinner, Snacks, Desserts, etc.
    val rating: Float = 4.5f,
    val reviewCount: Int = 12,
    val isFeatured: Boolean = false,
    val isTrending: Boolean = false,
    val isAI: Boolean = false,
    val alternativeIngredients: String = "" // PIPE (|) separated
)

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey val email: String,
    val name: String,
    val plan: String = "Free", // "Free", "Pro", "Max", "Max Plus"
    val credits: Int = 1000,
    val profilePicture: String = "", // Asset or placeholder URL
    val paymentMethod: String = "Google Pay (Ending in 4022)",
    val isDarkMode: Boolean = false,
    val language: String = "English"
)

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val recipeId: Int
)

@Entity(tableName = "transactions")
data class CreditTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val description: String,
    val amount: Int, // e.g., -100 or +1000
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val recipeId: Int,
    val recipeTitle: String,
    val ingredient: String,
    val isChecked: Boolean = false
)
