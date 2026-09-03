package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.KaptaDao
import com.example.data.local.entity.CompanyEntity
import com.example.data.local.entity.CompanyUserEntity
import com.example.data.local.entity.FinancialTransactionEntity
import com.example.data.local.entity.IaFunctionEntity
import com.example.data.local.entity.PosProductEntity
import com.example.data.local.entity.PosSaleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CompanyEntity::class,
        CompanyUserEntity::class,
        PosProductEntity::class,
        PosSaleEntity::class,
        FinancialTransactionEntity::class,
        IaFunctionEntity::class
    ],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun kaptaDao(): KaptaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kapta_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(context))
                .addMigrations(MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pos_products ADD COLUMN isService INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pos_sales ADD COLUMN tipoVenta TEXT NOT NULL DEFAULT 'Normal'")
            }
        }

        val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE financial_transactions ADD COLUMN stockAnterior INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE financial_transactions ADD COLUMN stockNuevo INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE financial_transactions ADD COLUMN usuario TEXT NOT NULL DEFAULT ''")
            }
        }
    }

    private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(database.kaptaDao())
                }
            }
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(database.kaptaDao())
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    if (database.kaptaDao().getAllCompaniesSync().isEmpty()) {
                        populateInitialData(database.kaptaDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: KaptaDao) {
            // Se han eliminado todas las empresas y datos de prueba estáticos. 
            // La fuente única de verdad es la hoja 'Data Maestra' de Google Sheets.
        }
    }
}
