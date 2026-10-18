package com.jojomango.expensetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets ORDER BY archived ASC, name ASC")
    fun observeAll(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets")
    suspend fun getAllOnce(): List<WalletEntity>

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getById(id: String): WalletEntity?

    @Query("SELECT COUNT(*) FROM wallets")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM wallets WHERE archived = 0")
    suspend fun countActive(): Int

    // 一定要用 @Upsert，不能用 @Insert(onConflict = REPLACE)：SQLite 的 REPLACE 是「先刪除
    // 舊列再插入」，這一下刪除會觸發 transactions 的外鍵動作——錢包是 ON DELETE CASCADE
    // （交易被刪光）、分類是 ON DELETE SET NULL（交易全變成未分類）。Phase 8 才發現
    // 「編輯錢包/分類」與「合併匯入」一直都會這樣弄丟資料（見 PersistenceTest 的回歸測試）。
    @Upsert
    suspend fun upsert(wallet: WalletEntity)

    @Upsert
    suspend fun upsertAll(wallets: List<WalletEntity>)

    @Query("DELETE FROM wallets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM wallets")
    suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAllOnce(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    // 一定要用 @Upsert，不能用 @Insert(onConflict = REPLACE)：SQLite 的 REPLACE 是「先刪除
    // 舊列再插入」，這一下刪除會觸發 transactions 的外鍵動作——錢包是 ON DELETE CASCADE
    // （交易被刪光）、分類是 ON DELETE SET NULL（交易全變成未分類）。Phase 8 才發現
    // 「編輯錢包/分類」與「合併匯入」一直都會這樣弄丟資料（見 PersistenceTest 的回歸測試）。
    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()

    /** 分類刪除時，該分類名下的交易轉移到「未分類」——SPEC.md §3.3。 */
    @Query("UPDATE transactions SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun reassignTransactionsToUncategorized(categoryId: String)

    @Transaction
    suspend fun deleteAndReassign(categoryId: String) {
        reassignTransactionsToUncategorized(categoryId)
        deleteById(categoryId)
    }
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE walletId = :walletId ORDER BY date DESC")
    fun observeByWallet(walletId: String): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE walletId = :walletId")
    suspend fun countByWallet(walletId: String): Int

    @Query("SELECT * FROM transactions")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions")
    suspend fun getAllOnce(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = :id")
    fun observe(id: Int = SettingsEntity.SINGLETON_ID): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = :id")
    suspend fun get(id: Int = SettingsEntity.SINGLETON_ID): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: SettingsEntity)
}
