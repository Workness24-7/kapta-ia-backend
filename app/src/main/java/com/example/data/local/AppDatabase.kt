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
    version = 10,
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
                .build()
                INSTANCE = instance
                instance
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
