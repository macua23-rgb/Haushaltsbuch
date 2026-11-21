package de.haushaltsbuch.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Kontostand (in Kontowährung) = Startsaldo + Einnahmen - Ausgaben - ausgehende Umbuchungen
 * + eingehende Umbuchungen (bei Fremdwährung: toAmountCents).
 */
private const val BALANCE_EXPR = """a.startBalanceCents
 + COALESCE((SELECT SUM(CASE WHEN b.type = 'INCOME' THEN b.amountCents ELSE -b.amountCents END)
             FROM bookings b WHERE b.accountId = a.id), 0)
 + COALESCE((SELECT SUM(COALESCE(b.toAmountCents, b.amountCents)) FROM bookings b
             WHERE b.type = 'TRANSFER' AND b.toAccountId = a.id), 0)"""

@Dao
interface AccountDao {
    @Query("SELECT a.*, ($BALANCE_EXPR) AS balanceCents FROM accounts a ORDER BY a.id")
    fun observeWithBalance(): Flow<List<AccountWithBalance>>

    @Query("SELECT ($BALANCE_EXPR) FROM accounts a WHERE a.id = :id")
    suspend fun balanceOf(id: Long): Long

    @Query("SELECT * FROM accounts") suspend fun all(): List<Account>
    @Query("SELECT COUNT(*) FROM accounts") suspend fun count(): Int
    @Query("UPDATE accounts SET settlementAccountId = NULL WHERE settlementAccountId = :id")
    suspend fun clearSettlementRef(id: Long)
    @Insert suspend fun insert(a: Account): Long
    @Update suspend fun update(a: Account)
    @Delete suspend fun delete(a: Account)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY id") fun observeAll(): Flow<List<Category>>
    @Query("SELECT * FROM categories WHERE id = :id") suspend fun get(id: Long): Category?
    @Insert suspend fun insertAll(c: List<Category>): List<Long>
    @Insert suspend fun insert(c: Category): Long
    @Update suspend fun update(c: Category)
    @Delete suspend fun delete(c: Category)
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<Booking>>
    @Query("SELECT * FROM bookings WHERE type = 'EXPENSE' AND categoryId = :cat AND date BETWEEN :from AND :to")
    suspend fun expenses(cat: Long, from: LocalDate, to: LocalDate): List<Booking>
    @Query("SELECT COUNT(*) FROM bookings WHERE accountId = :id OR toAccountId = :id")
    suspend fun countForAccount(id: Long): Int
    @Query("UPDATE bookings SET categoryId = NULL WHERE categoryId = :id")
    suspend fun clearCategory(id: Long)
    @Insert suspend fun insert(b: Booking): Long
    @Update suspend fun update(b: Booking)
    @Delete suspend fun delete(b: Booking)
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_rules ORDER BY nextDue") fun observeAll(): Flow<List<RecurringRule>>
    @Query("SELECT * FROM recurring_rules WHERE active = 1 AND nextDue <= :today")
    suspend fun due(today: LocalDate): List<RecurringRule>
    @Query("SELECT COUNT(*) FROM recurring_rules WHERE accountId = :id") suspend fun countForAccount(id: Long): Int
    @Query("UPDATE recurring_rules SET categoryId = NULL WHERE categoryId = :id") suspend fun clearCategory(id: Long)
    @Upsert suspend fun upsert(r: RecurringRule)
    @Delete suspend fun delete(r: RecurringRule)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets") fun observeAll(): Flow<List<Budget>>
    @Query("SELECT * FROM budgets WHERE categoryId = :id") suspend fun get(id: Long): Budget?
    @Query("DELETE FROM budgets WHERE categoryId = :id") suspend fun deleteFor(id: Long)
    @Upsert suspend fun upsert(b: Budget)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY id") fun observeAll(): Flow<List<SavingsGoal>>
    @Upsert suspend fun upsert(g: SavingsGoal)
    @Delete suspend fun delete(g: SavingsGoal)
}

@Dao
interface RateDao {
    @Query("SELECT * FROM rates") fun observeAll(): Flow<List<Rate>>
    @Query("SELECT * FROM rates") suspend fun all(): List<Rate>
    @Query("SELECT COUNT(*) FROM rates") suspend fun count(): Int
    @Query("DELETE FROM rates WHERE currency = :c") suspend fun delete(c: String)
    @Upsert suspend fun upsert(r: Rate)
}
