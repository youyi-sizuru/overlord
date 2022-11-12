package com.lifefighter.overlord.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.koin.dsl.module

/**
 * @author xzp
 * @created on 2021/3/13.
 */
@Database(entities = [MihoyoAccount::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getMihoyoAccountDao(): MihoyoAccountDao
}

object AppDatabaseModule {
    val module = module {
        single {
            Room.databaseBuilder(
                get(),
                AppDatabase::class.java, "overlord"
            ).addMigrations(object : Migration(1, 2) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("alter table mihoyo_account add column level INTEGER NOT NULL default 0")
                }
            }, object :Migration(2, 3){
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("alter table mihoyo_account add column userAgent TEXT")
                }

            }).build()
        }
        single {
            get<AppDatabase>().getMihoyoAccountDao()
        }
    }
}