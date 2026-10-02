package com.dismal.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProductEntity::class,
        CustomerEntity::class,
        SaleEntity::class,
        LicenseEntity::class,
        SalesTargetEntity::class,
        SyncOutboxEntity::class,
    ],
    version = 13,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    abstract fun customerDao(): CustomerDao

    abstract fun saleDao(): SaleDao

    abstract fun licenseDao(): LicenseDao

    abstract fun salesTargetDao(): SalesTargetDao

    abstract fun syncOutboxDao(): SyncOutboxDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    addColumnIfMissing(db, "products", "licensesJson", "TEXT NOT NULL DEFAULT '[]'")
                    if (columnExists(db, "products", "licenses")) {
                        db.execSQL("UPDATE `products` SET `licensesJson` = `licenses`")
                    }
                    addColumnIfMissing(db, "products", "isSynced", "INTEGER NOT NULL DEFAULT 0")
                    addColumnIfMissing(db, "customers", "isSynced", "INTEGER NOT NULL DEFAULT 0")
                    addColumnIfMissing(db, "sales", "isSynced", "INTEGER NOT NULL DEFAULT 0")
                }
            }

        internal val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    addColumnIfMissing(db, "customers", "saldoActual", "REAL")
                    addColumnIfMissing(db, "customers", "diasAtraso", "INTEGER NOT NULL DEFAULT 0")
                    addColumnIfMissing(db, "customers", "estado", "TEXT")
                }
            }

        internal val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    addColumnIfMissing(db, "sales", "saleType", "TEXT NOT NULL DEFAULT 'CASH'")
                    addColumnIfMissing(db, "sales", "status", "TEXT NOT NULL DEFAULT 'DRAFT'")
                    addColumnIfMissing(db, "sales", "total", "REAL NOT NULL DEFAULT 0")
                    addColumnIfMissing(db, "sales", "paid", "REAL NOT NULL DEFAULT 0")
                    addColumnIfMissing(db, "sales", "balance", "REAL NOT NULL DEFAULT 0")
                    addColumnIfMissing(db, "sales", "createdAt", "TEXT NOT NULL DEFAULT ''")
                    addColumnIfMissing(db, "sales", "updatedAt", "TEXT NOT NULL DEFAULT ''")
                    addColumnIfMissing(db, "sales", "itemsJson", "TEXT NOT NULL DEFAULT '[]'")
                    if (columnExists(db, "sales", "totalPrice")) {
                        db.execSQL("UPDATE `sales` SET `total` = `totalPrice`")
                    }
                    if (columnExists(db, "sales", "saleDate")) {
                        db.execSQL("UPDATE `sales` SET `createdAt` = CAST(`saleDate` AS TEXT)")
                        db.execSQL("UPDATE `sales` SET `updatedAt` = CAST(`saleDate` AS TEXT)")
                    }
                }
            }

        internal val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `sales_new` (
                          `id` TEXT NOT NULL,
                          `customerId` TEXT NOT NULL,
                          `saleType` TEXT NOT NULL,
                          `status` TEXT NOT NULL,
                          `total` REAL NOT NULL,
                          `paid` REAL NOT NULL,
                          `balance` REAL NOT NULL,
                          `createdAt` TEXT NOT NULL,
                          `updatedAt` TEXT NOT NULL,
                          `itemsJson` TEXT NOT NULL,
                          `isSynced` INTEGER NOT NULL,
                          PRIMARY KEY(`id`)
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        INSERT INTO `sales_new` (
                          `id`,
                          `customerId`,
                          `saleType`,
                          `status`,
                          `total`,
                          `paid`,
                          `balance`,
                          `createdAt`,
                          `updatedAt`,
                          `itemsJson`,
                          `isSynced`
                        )
                        SELECT
                          `id`,
                          `customerId`,
                          COALESCE(`saleType`, 'CASH'),
                          COALESCE(`status`, 'DRAFT'),
                          COALESCE(`total`, 0),
                          COALESCE(`paid`, 0),
                          COALESCE(`balance`, 0),
                          COALESCE(`createdAt`, ''),
                          COALESCE(`updatedAt`, ''),
                          COALESCE(`itemsJson`, '[]'),
                          COALESCE(`isSynced`, 0)
                        FROM `sales`
                        """.trimIndent(),
                    )
                    db.execSQL("DROP TABLE `sales`")
                    db.execSQL("ALTER TABLE `sales_new` RENAME TO `sales`")
                }
            }

        internal val MIGRATION_5_6 =
            object : Migration(5, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `sales_new` (
                          `id` TEXT NOT NULL,
                          `customerId` TEXT NOT NULL,
                          `saleType` TEXT NOT NULL,
                          `status` TEXT NOT NULL,
                          `total` REAL NOT NULL,
                          `paid` REAL NOT NULL,
                          `balance` REAL NOT NULL,
                          `createdAt` TEXT NOT NULL,
                          `updatedAt` TEXT NOT NULL,
                          `itemsJson` TEXT NOT NULL,
                          `isSynced` INTEGER NOT NULL,
                          PRIMARY KEY(`id`)
                        )
                        """.trimIndent(),
                    )

                    val idExpr = if (columnExists(db, "sales", "id")) "`id`" else "lower(hex(randomblob(16)))"
                    val customerIdExpr = if (columnExists(db, "sales", "customerId")) "`customerId`" else "''"
                    val saleTypeExpr =
                        if (columnExists(db, "sales", "saleType")) {
                            "COALESCE(`saleType`, 'CASH')"
                        } else {
                            "'CASH'"
                        }
                    val statusExpr =
                        if (columnExists(db, "sales", "status")) {
                            "COALESCE(`status`, 'DRAFT')"
                        } else {
                            "'DRAFT'"
                        }
                    val totalExpr =
                        when {
                            columnExists(db, "sales", "total") -> "COALESCE(`total`, 0)"
                            columnExists(db, "sales", "totalPrice") -> "COALESCE(`totalPrice`, 0)"
                            else -> "0"
                        }
                    val paidExpr = if (columnExists(db, "sales", "paid")) "COALESCE(`paid`, 0)" else "0"
                    val balanceExpr = if (columnExists(db, "sales", "balance")) "COALESCE(`balance`, 0)" else "0"
                    val createdAtExpr =
                        when {
                            columnExists(db, "sales", "createdAt") -> "COALESCE(`createdAt`, '')"
                            columnExists(db, "sales", "saleDate") -> "COALESCE(CAST(`saleDate` AS TEXT), '')"
                            else -> "''"
                        }
                    val updatedAtExpr =
                        when {
                            columnExists(db, "sales", "updatedAt") -> "COALESCE(`updatedAt`, '')"
                            columnExists(db, "sales", "saleDate") -> "COALESCE(CAST(`saleDate` AS TEXT), '')"
                            else -> "''"
                        }
                    val itemsJsonExpr =
                        if (columnExists(db, "sales", "itemsJson")) {
                            "COALESCE(`itemsJson`, '[]')"
                        } else {
                            "'[]'"
                        }
                    val isSyncedExpr =
                        if (columnExists(db, "sales", "isSynced")) {
                            "COALESCE(`isSynced`, 0)"
                        } else {
                            "0"
                        }

                    db.execSQL(
                        """
                        INSERT INTO `sales_new` (
                          `id`,
                          `customerId`,
                          `saleType`,
                          `status`,
                          `total`,
                          `paid`,
                          `balance`,
                          `createdAt`,
                          `updatedAt`,
                          `itemsJson`,
                          `isSynced`
                        )
                        SELECT
                          $idExpr,
                          $customerIdExpr,
                          $saleTypeExpr,
                          $statusExpr,
                          $totalExpr,
                          $paidExpr,
                          $balanceExpr,
                          $createdAtExpr,
                          $updatedAtExpr,
                          $itemsJsonExpr,
                          $isSyncedExpr
                        FROM `sales`
                        """.trimIndent(),
                    )
                    db.execSQL("DROP TABLE `sales`")
                    db.execSQL("ALTER TABLE `sales_new` RENAME TO `sales`")
                }
            }

        internal val MIGRATION_6_7 =
            object : Migration(6, 7) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `licenses` (
                          `id` TEXT NOT NULL,
                          `licenseKey` TEXT NOT NULL,
                          `status` TEXT NOT NULL,
                          `available` INTEGER NOT NULL,
                          `usedActivations` INTEGER NOT NULL,
                          `maxActivations` INTEGER NOT NULL,
                          `purchasePrice` REAL NOT NULL,
                          `softwareId` TEXT,
                          `softwareName` TEXT,
                          `ownerId` TEXT,
                          `ownerEmail` TEXT,
                          PRIMARY KEY(`id`)
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `sales_targets` (
                          `id` TEXT NOT NULL,
                          `softwareId` TEXT NOT NULL,
                          `softwareName` TEXT,
                          `metaUnits` INTEGER NOT NULL,
                          `salePrice` REAL,
                          `variableCost` REAL,
                          `fixedCostProduct` REAL,
                          `unitsSoldCurrent` INTEGER,
                          `deadline` TEXT NOT NULL,
                          `notes` TEXT,
                          `marginUnit` REAL,
                          `targetRevenue` REAL,
                          `variableCostTotal` REAL,
                          `contributionTotal` REAL,
                          `breakEvenUnits` INTEGER,
                          `breakEvenRevenue` REAL,
                          `expectedProfit` REAL,
                          `profitable` INTEGER,
                          `targetAchieved` INTEGER,
                          `fixedCostApplied` REAL,
                          `createdAt` TEXT,
                          `updatedAt` TEXT,
                          PRIMARY KEY(`id`)
                        )
                        """.trimIndent(),
                    )
                    addColumnIfMissing(db, "sales", "syncStatus", "TEXT NOT NULL DEFAULT 'SYNCED'")
                    addColumnIfMissing(db, "sales", "syncErrorMessage", "TEXT")
                    db.execSQL(
                        """
                    UPDATE `sales`
                    SET `syncStatus` = CASE WHEN `isSynced` = 1 THEN 'SYNCED' ELSE 'PENDING' END
                    """,
                    )
                }
            }

        internal val MIGRATION_7_8 =
            object : Migration(7, 8) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `sync_outbox` (
                          `id` TEXT NOT NULL,
                          `entityType` TEXT NOT NULL,
                          `entityId` TEXT NOT NULL,
                          `action` TEXT NOT NULL,
                          `status` TEXT NOT NULL,
                          `errorMessage` TEXT,
                          `createdAt` INTEGER NOT NULL,
                          `updatedAt` INTEGER NOT NULL,
                          PRIMARY KEY(`id`)
                        )
                        """.trimIndent(),
                    )
                    addColumnIfMissing(db, "customers", "syncStatus", "TEXT NOT NULL DEFAULT 'SYNCED'")
                    addColumnIfMissing(db, "customers", "syncErrorMessage", "TEXT")
                    db.execSQL(
                        """
                    UPDATE `customers`
                    SET `syncStatus` = CASE WHEN `isSynced` = 1 THEN 'SYNCED' ELSE 'PENDING' END
                    """,
                    )
                }
            }

        internal val MIGRATION_8_9 =
            object : Migration(8, 9) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `sales_new` (
                          `id` TEXT NOT NULL,
                          `customerId` TEXT NOT NULL,
                          `saleType` TEXT NOT NULL DEFAULT 'CASH',
                          `status` TEXT NOT NULL DEFAULT 'DRAFT',
                          `total` REAL NOT NULL DEFAULT 0,
                          `paid` REAL NOT NULL DEFAULT 0,
                          `balance` REAL NOT NULL DEFAULT 0,
                          `createdAt` TEXT NOT NULL DEFAULT '',
                          `updatedAt` TEXT NOT NULL DEFAULT '',
                          `itemsJson` TEXT NOT NULL DEFAULT '[]',
                          `isSynced` INTEGER NOT NULL DEFAULT 0,
                          `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED',
                          `syncErrorMessage` TEXT,
                          PRIMARY KEY(`id`)
                        )
                        """.trimIndent(),
                    )

                    val idExpr = if (columnExists(db, "sales", "id")) "`id`" else "lower(hex(randomblob(16)))"
                    val customerIdExpr = if (columnExists(db, "sales", "customerId")) "`customerId`" else "''"
                    val saleTypeExpr = if (columnExists(db, "sales", "saleType")) "COALESCE(`saleType`, 'CASH')" else "'CASH'"
                    val statusExpr = if (columnExists(db, "sales", "status")) "COALESCE(`status`, 'DRAFT')" else "'DRAFT'"
                    val totalExpr =
                        when {
                            columnExists(db, "sales", "total") -> "COALESCE(`total`, 0)"
                            columnExists(db, "sales", "totalPrice") -> "COALESCE(`totalPrice`, 0)"
                            else -> "0"
                        }
                    val paidExpr = if (columnExists(db, "sales", "paid")) "COALESCE(`paid`, 0)" else "0"
                    val balanceExpr = if (columnExists(db, "sales", "balance")) "COALESCE(`balance`, 0)" else "0"
                    val createdAtExpr =
                        when {
                            columnExists(db, "sales", "createdAt") -> "COALESCE(`createdAt`, '')"
                            columnExists(db, "sales", "saleDate") -> "COALESCE(CAST(`saleDate` AS TEXT), '')"
                            else -> "''"
                        }
                    val updatedAtExpr =
                        when {
                            columnExists(db, "sales", "updatedAt") -> "COALESCE(`updatedAt`, '')"
                            columnExists(db, "sales", "saleDate") -> "COALESCE(CAST(`saleDate` AS TEXT), '')"
                            else -> "''"
                        }
                    val itemsJsonExpr = if (columnExists(db, "sales", "itemsJson")) "COALESCE(`itemsJson`, '[]')" else "'[]'"
                    val isSyncedExpr = if (columnExists(db, "sales", "isSynced")) "COALESCE(`isSynced`, 0)" else "0"
                    val syncStatusExpr =
                        if (columnExists(db, "sales", "syncStatus")) {
                            "COALESCE(`syncStatus`, CASE WHEN `isSynced` = 1 THEN 'SYNCED' ELSE 'PENDING' END)"
                        } else {
                            "CASE WHEN $isSyncedExpr = 1 THEN 'SYNCED' ELSE 'PENDING' END"
                        }
                    val syncErrorExpr = if (columnExists(db, "sales", "syncErrorMessage")) "`syncErrorMessage`" else "NULL"

                    db.execSQL(
                        """
                        INSERT INTO `sales_new` (
                          `id`,
                          `customerId`,
                          `saleType`,
                          `status`,
                          `total`,
                          `paid`,
                          `balance`,
                          `createdAt`,
                          `updatedAt`,
                          `itemsJson`,
                          `isSynced`,
                          `syncStatus`,
                          `syncErrorMessage`
                        )
                        SELECT
                          $idExpr,
                          $customerIdExpr,
                          $saleTypeExpr,
                          $statusExpr,
                          $totalExpr,
                          $paidExpr,
                          $balanceExpr,
                          $createdAtExpr,
                          $updatedAtExpr,
                          $itemsJsonExpr,
                          $isSyncedExpr,
                          $syncStatusExpr,
                          $syncErrorExpr
                        FROM `sales`
                        """.trimIndent(),
                    )
                    db.execSQL("DROP TABLE `sales`")
                    db.execSQL("ALTER TABLE `sales_new` RENAME TO `sales`")
                }
            }

        internal val MIGRATION_9_10 =
            object : Migration(9, 10) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    addColumnIfMissing(db, "customers", "updatedAt", "TEXT NOT NULL DEFAULT ''")
                }
            }

        internal val MIGRATION_10_11 =
            object : Migration(10, 11) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `products_new` (
                          `id` TEXT NOT NULL,
                          `name` TEXT NOT NULL,
                          `description` TEXT NOT NULL,
                          `price` REAL NOT NULL,
                          `platform` TEXT NOT NULL,
                          `imageUrl` TEXT,
                          `licensesJson` TEXT NOT NULL DEFAULT '[]',
                          `isSynced` INTEGER NOT NULL DEFAULT 0,
                          PRIMARY KEY(`id`)
                        )
                        """.trimIndent(),
                    )

                    val idExpr = if (columnExists(db, "products", "id")) "`id`" else "lower(hex(randomblob(16)))"
                    val nameExpr = if (columnExists(db, "products", "name")) "`name`" else "''"
                    val descExpr = if (columnExists(db, "products", "description")) "`description`" else "''"
                    val priceExpr = if (columnExists(db, "products", "price")) "`price`" else "0"
                    val platformExpr = if (columnExists(db, "products", "platform")) "`platform`" else "''"
                    val imageExpr = if (columnExists(db, "products", "imageUrl")) "`imageUrl`" else "NULL"
                    val licensesExpr = when {
                        columnExists(db, "products", "licensesJson") -> "COALESCE(`licensesJson`, '[]')"
                        columnExists(db, "products", "licenses") -> "COALESCE(`licenses`, '[]')"
                        else -> "'[]'"
                    }
                    val isSyncedExpr = when {
                        columnExists(db, "products", "isSynced") -> "CASE WHEN `isSynced` IN (1, '1', 'true', 'TRUE') THEN 1 ELSE 0 END"
                        else -> "0"
                    }

                    db.execSQL(
                        """
                        INSERT INTO `products_new` (
                          `id`,
                          `name`,
                          `description`,
                          `price`,
                          `platform`,
                          `imageUrl`,
                          `licensesJson`,
                          `isSynced`
                        )
                        SELECT
                          $idExpr,
                          $nameExpr,
                          $descExpr,
                          $priceExpr,
                          $platformExpr,
                          $imageExpr,
                          $licensesExpr,
                          $isSyncedExpr
                        FROM `products`
                        """.trimIndent(),
                    )
                    db.execSQL("DROP TABLE `products`")
                    db.execSQL("ALTER TABLE `products_new` RENAME TO `products`")
                }
            }

        internal val MIGRATION_11_12 =
            object : Migration(11, 12) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `products_new` (
                          `id` TEXT NOT NULL,
                          `name` TEXT NOT NULL,
                          `description` TEXT NOT NULL,
                          `price` REAL NOT NULL,
                          `platform` TEXT NOT NULL,
                          `imageUrl` TEXT,
                          `licensesJson` TEXT NOT NULL DEFAULT '[]',
                          `isSynced` INTEGER NOT NULL DEFAULT 0,
                          PRIMARY KEY(`id`)
                        )
                        """.trimIndent(),
                    )

                    val idExpr = if (columnExists(db, "products", "id")) "`id`" else "lower(hex(randomblob(16)))"
                    val nameExpr = if (columnExists(db, "products", "name")) "`name`" else "''"
                    val descExpr = if (columnExists(db, "products", "description")) "`description`" else "''"
                    val priceExpr = if (columnExists(db, "products", "price")) "`price`" else "0"
                    val platformExpr = if (columnExists(db, "products", "platform")) "`platform`" else "''"
                    val imageExpr = if (columnExists(db, "products", "imageUrl")) "`imageUrl`" else "NULL"
                    val licensesExpr = when {
                        columnExists(db, "products", "licensesJson") -> "COALESCE(`licensesJson`, '[]')"
                        columnExists(db, "products", "licenses") -> "COALESCE(`licenses`, '[]')"
                        else -> "'[]'"
                    }
                    val isSyncedExpr = when {
                        columnExists(db, "products", "isSynced") -> "CASE WHEN `isSynced` IN (1, '1', 'true', 'TRUE') THEN 1 ELSE 0 END"
                        else -> "0"
                    }

                    db.execSQL(
                        """
                        INSERT INTO `products_new` (
                          `id`,
                          `name`,
                          `description`,
                          `price`,
                          `platform`,
                          `imageUrl`,
                          `licensesJson`,
                          `isSynced`
                        )
                        SELECT
                          $idExpr,
                          $nameExpr,
                          $descExpr,
                          $priceExpr,
                          $platformExpr,
                          $imageExpr,
                          $licensesExpr,
                          $isSyncedExpr
                        FROM `products`
                        """.trimIndent(),
                    )
                    db.execSQL("DROP TABLE `products`")
                    db.execSQL("ALTER TABLE `products_new` RENAME TO `products`")
                }
            }

        val MIGRATION_12_13 =
            object : Migration(12, 13) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    addColumnIfMissing(db, "sales", "saleNumber", "TEXT")
                    addColumnIfMissing(db, "sales", "country", "TEXT NOT NULL DEFAULT 'EC'")
                    addColumnIfMissing(db, "sales", "currency", "TEXT NOT NULL DEFAULT 'USD'")
                }
            }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val isDebug =
                    (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                if (isDebug) {
                    val prefs = context.getSharedPreferences("db_reset", Context.MODE_PRIVATE)
                    val lastReset = prefs.getInt("last_reset_version", 0)
                    if (lastReset < 13) {
                        context.deleteDatabase("dismal_database")
                        prefs.edit().putInt("last_reset_version", 13).apply()
                    }
                }
                val builder =
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "dismal_database",
                    )
                val instance =
                    if (isDebug) {
                        builder
                            .fallbackToDestructiveMigration()
                            .build()
                    } else {
                        builder
                            .addMigrations(
                                MIGRATION_1_2,
                                MIGRATION_2_3,
                                MIGRATION_3_4,
                                MIGRATION_4_5,
                                MIGRATION_5_6,
                                MIGRATION_6_7,
                                MIGRATION_7_8,
                                MIGRATION_8_9,
                                MIGRATION_9_10,
                                MIGRATION_10_11,
                                MIGRATION_11_12,
                                MIGRATION_12_13,
                            )
                            .build()
                    }
                INSTANCE = instance
                instance
            }
        }

        private fun addColumnIfMissing(
            db: SupportSQLiteDatabase,
            table: String,
            column: String,
            definition: String,
        ) {
            if (!columnExists(db, table, column)) {
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $definition")
            }
        }

        private fun columnExists(
            db: SupportSQLiteDatabase,
            table: String,
            column: String,
        ): Boolean {
            db.query("PRAGMA table_info(`$table`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == column) {
                        return true
                    }
                }
            }
            return false
        }
    }
}
