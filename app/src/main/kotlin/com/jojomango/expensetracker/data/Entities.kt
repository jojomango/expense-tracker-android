package com.jojomango.expensetracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.domain.Category
import com.jojomango.expensetracker.domain.CategoryType
import com.jojomango.expensetracker.domain.Settings
import com.jojomango.expensetracker.domain.Theme
import com.jojomango.expensetracker.domain.Transaction
import com.jojomango.expensetracker.domain.TransactionType
import com.jojomango.expensetracker.domain.Wallet
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val id: String,
    val name: String,
    val currency: String,
    val budgetMode: String,
    val budgetAmount: Long?,
    val archived: Boolean,
) {
    fun toDomain(): Wallet =
        Wallet(
            id = id,
            name = name,
            currency = currency,
            budgetMode = BudgetMode.valueOf(budgetMode),
            budgetAmount = budgetAmount,
            archived = archived,
        )

    companion object {
        fun fromDomain(wallet: Wallet): WalletEntity =
            WalletEntity(
                id = wallet.id,
                name = wallet.name,
                currency = wallet.currency,
                budgetMode = wallet.budgetMode.name,
                budgetAmount = wallet.budgetAmount,
                archived = wallet.archived,
            )
    }
}

@Entity(
    tableName = "categories",
    indices = [Index("type")],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val icon: String,
    val color: String,
    val isDefault: Boolean,
) {
    fun toDomain(): Category =
        Category(
            id = id,
            name = name,
            type = CategoryType.valueOf(type),
            icon = icon,
            color = color,
            isDefault = isDefault,
        )

    companion object {
        fun fromDomain(category: Category): CategoryEntity =
            CategoryEntity(
                id = category.id,
                name = category.name,
                type = category.type.name,
                icon = category.icon,
                color = category.color,
                isDefault = category.isDefault,
            )
    }
}

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("walletId"), Index("categoryId"), Index("date")],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val walletId: String,
    val type: String,
    val amount: Long,
    val categoryId: String?,
    val date: LocalDate,
    val note: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun toDomain(): Transaction =
        Transaction(
            id = id,
            walletId = walletId,
            type = TransactionType.valueOf(type),
            amount = amount,
            categoryId = categoryId,
            date = date,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    companion object {
        fun fromDomain(transaction: Transaction): TransactionEntity =
            TransactionEntity(
                id = transaction.id,
                walletId = transaction.walletId,
                type = transaction.type.name,
                amount = transaction.amount,
                categoryId = transaction.categoryId,
                date = transaction.date,
                note = transaction.note,
                createdAt = transaction.createdAt,
                updatedAt = transaction.updatedAt,
            )
    }
}

/** 單行設定表（SPEC.md §3.5）。`id` 固定為 0，全域只有一列。 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val weekStartDay: Int,
    val theme: String,
    val defaultWalletId: String?,
) {
    fun toDomain(): Settings =
        Settings(
            weekStartDay = weekStartDay.toDayOfWeek(),
            theme = Theme.valueOf(theme),
            defaultWalletId = defaultWalletId,
        )

    companion object {
        const val SINGLETON_ID = 0

        fun default(): SettingsEntity =
            SettingsEntity(
                weekStartDay = Settings().weekStartDay.toSpecInt(),
                theme = Theme.SYSTEM.name,
                defaultWalletId = null,
            )

        fun fromDomain(settings: Settings): SettingsEntity =
            SettingsEntity(
                weekStartDay = settings.weekStartDay.toSpecInt(),
                theme = settings.theme.name,
                defaultWalletId = settings.defaultWalletId,
            )
    }
}

/** SPEC.md §3.5：`0`(週日) ~ `6`(週六) -> [DayOfWeek]（ISO：MONDAY=1...SUNDAY=7）。 */
private fun Int.toDayOfWeek(): DayOfWeek {
    val isoValue = if (this == 0) 7 else this
    return DayOfWeek.entries.first { it.value == isoValue }
}

private fun DayOfWeek.toSpecInt(): Int = if (this == DayOfWeek.SUNDAY) 0 else this.value
