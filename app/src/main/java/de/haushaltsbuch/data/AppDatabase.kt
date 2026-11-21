package de.haushaltsbuch.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Account::class, Category::class, Booking::class, RecurringRule::class,
        Budget::class, SavingsGoal::class, Rate::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accounts(): AccountDao
    abstract fun categories(): CategoryDao
    abstract fun bookings(): BookingDao
    abstract fun recurring(): RecurringDao
    abstract fun budgets(): BudgetDao
    abstract fun goals(): GoalDao
    abstract fun fx(): RateDao

    companion object {
        /** v1 -> v2: Wechselkurse und Zielbetrag bei Umbuchungen zwischen Währungen. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `rates` (`currency` TEXT NOT NULL, `toBase` REAL NOT NULL, PRIMARY KEY(`currency`))")
                db.execSQL("ALTER TABLE `bookings` ADD COLUMN `toAmountCents` INTEGER")
                db.execSQL("ALTER TABLE `recurring_rules` ADD COLUMN `anchorDay` INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
