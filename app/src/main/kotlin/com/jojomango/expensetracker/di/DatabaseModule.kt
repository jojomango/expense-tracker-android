package com.jojomango.expensetracker.di

import android.content.Context
import androidx.room.Room
import com.jojomango.expensetracker.data.AppDatabase
import com.jojomango.expensetracker.data.CategoryDao
import com.jojomango.expensetracker.data.Migrations
import com.jojomango.expensetracker.data.SettingsDao
import com.jojomango.expensetracker.data.TransactionDao
import com.jojomango.expensetracker.data.WalletDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(context, AppDatabase::class.java, "expense-tracker.db")
            .addMigrations(*Migrations.all)
            .build()

    @Provides
    fun provideWalletDao(db: AppDatabase): WalletDao = db.walletDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideSettingsDao(db: AppDatabase): SettingsDao = db.settingsDao()
}
