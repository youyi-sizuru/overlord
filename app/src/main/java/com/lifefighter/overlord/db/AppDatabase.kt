package com.lifefighter.overlord.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.koin.dsl.module

/**
 * @author xzp
 * @created on 2021/3/13.
 */
@Database(entities = [MihoyoAccount::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getMihoyoAccountDao(): MihoyoAccountDao
}

object AppDatabaseModule {
    val module = module {
        single {
            Room.databaseBuilder(
                get(),
                AppDatabase::class.java, "overlord"
            ).build()
        }
        single {
            get<AppDatabase>().getMihoyoAccountDao()
        }
    }
}