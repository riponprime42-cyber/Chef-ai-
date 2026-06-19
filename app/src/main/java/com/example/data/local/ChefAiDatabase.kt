package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [Recipe::class, Profile::class, Favorite::class, CreditTransaction::class, ShoppingItem::class],
    version = 1,
    exportSchema = false
)
abstract class ChefAiDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun profileDao(): ProfileDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun transactionDao(): TransactionDao
    abstract fun shoppingDao(): ShoppingDao

    companion object {
        @Volatile
        private var INSTANCE: ChefAiDatabase? = null

        fun getDatabase(context: Context): ChefAiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChefAiDatabase::class.java,
                    "chef_ai_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
