package com.dismal.app.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test

class AppDatabaseMigrationTest {
    @Test
    fun migrate12To13_addsSaleNumberAndMarketFields() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DB)

        val config =
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(TEST_DB)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(12) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS `sales` (
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
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build()

        val db = FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
        AppDatabase.MIGRATION_12_13.migrate(db)

        assertColumnExists(db, "sales", "saleNumber")
        assertColumnExists(db, "sales", "country")
        assertColumnExists(db, "sales", "currency")
        db.close()
    }

    @Test
    fun migrate1To2_addsSyncAndLicensesJson() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DB)

        val config =
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(TEST_DB)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS `products` (
                                    `id` TEXT NOT NULL,
                                    `name` TEXT NOT NULL,
                                    `description` TEXT NOT NULL,
                                    `price` REAL NOT NULL,
                                    `platform` TEXT NOT NULL,
                                    `imageUrl` TEXT,
                                    `licenses` TEXT,
                                    PRIMARY KEY(`id`)
                                )
                                """.trimIndent(),
                            )
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS `customers` (
                                    `id` TEXT NOT NULL,
                                    `name` TEXT NOT NULL,
                                    `email` TEXT NOT NULL,
                                    `phone` TEXT,
                                    PRIMARY KEY(`id`)
                                )
                                """.trimIndent(),
                            )
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS `sales` (
                                    `id` TEXT NOT NULL,
                                    `customerId` TEXT NOT NULL,
                                    `productId` TEXT NOT NULL,
                                    `quantity` INTEGER NOT NULL,
                                    `totalPrice` REAL NOT NULL,
                                    `saleDate` INTEGER NOT NULL,
                                    PRIMARY KEY(`id`)
                                )
                                """.trimIndent(),
                            )
                            db.execSQL(
                                """
                                INSERT INTO `products` (`id`, `name`, `description`, `price`, `platform`, `imageUrl`, `licenses`)
                                VALUES ('p1', 'Prod', 'Desc', 10.0, 'web', NULL, '[]')
                                """.trimIndent(),
                            )
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        AppDatabase.MIGRATION_1_2.migrate(db)

        assertColumnExists(db, "products", "licensesJson")
        assertColumnExists(db, "products", "isSynced")
        assertColumnExists(db, "customers", "isSynced")
        assertColumnExists(db, "sales", "isSynced")
        db.close()
    }

    private companion object {
        private const val TEST_DB = "migration-test"
    }

    private fun assertColumnExists(
        db: SupportSQLiteDatabase,
        table: String,
        column: String,
    ) {
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) {
                    return
                }
            }
        }
        throw AssertionError("Missing column $column in $table")
    }
}
