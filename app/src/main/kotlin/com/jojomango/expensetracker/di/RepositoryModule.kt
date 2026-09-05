package com.jojomango.expensetracker.di

import com.jojomango.expensetracker.data.RoomBackupRepository
import com.jojomango.expensetracker.data.RoomCategoryRepository
import com.jojomango.expensetracker.data.RoomSettingsRepository
import com.jojomango.expensetracker.data.RoomTransactionRepository
import com.jojomango.expensetracker.data.RoomWalletRepository
import com.jojomango.expensetracker.domain.BackupRepository
import com.jojomango.expensetracker.domain.CategoryRepository
import com.jojomango.expensetracker.domain.SettingsRepository
import com.jojomango.expensetracker.domain.TransactionRepository
import com.jojomango.expensetracker.domain.WalletRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindWalletRepository(impl: RoomWalletRepository): WalletRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: RoomTransactionRepository): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: RoomCategoryRepository): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: RoomSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: RoomBackupRepository): BackupRepository
}
