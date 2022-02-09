package com.lifefighter.overlord.db

import androidx.room.*
import java.util.*

/**
 * @author xzp
 * @created on 2021/3/13.
 */
@Entity(tableName = "mihoyo_account")
data class MihoyoAccount(
    @PrimaryKey val uid: String,
    val region: String,
    val nickname: String,
    val regionName: String,
    val cookie: String,
    val lastSignDay: Long = 0,
    val signDays: Int = 0,
    val level: Int = 0,
) {
    val todaySigned: Boolean
        get() {
            val today = Calendar.getInstance()
            val signDay = Calendar.getInstance().apply {
                timeInMillis = lastSignDay
            }
            return today.get(Calendar.YEAR) == signDay.get(Calendar.YEAR) &&
                    today.get(Calendar.MONTH) == signDay.get(Calendar.MONTH) &&
                    today.get(Calendar.DAY_OF_MONTH) == signDay.get(Calendar.DAY_OF_MONTH)
        }
}

@Dao
interface MihoyoAccountDao {
    @Query("select * from mihoyo_account")
    suspend fun getAll(): List<MihoyoAccount>

    @Update
    suspend fun update(vararg mihoyoAccount: MihoyoAccount)

    @Delete
    suspend fun delete(vararg mihoyoAccount: MihoyoAccount)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(vararg mihoyoAccount: MihoyoAccount)
}