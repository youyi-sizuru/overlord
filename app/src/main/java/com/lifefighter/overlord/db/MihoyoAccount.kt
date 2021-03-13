package com.lifefighter.overlord.db

import androidx.room.*

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
    val lastSignDay: Long = 0
)

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

    @Query("select * from mihoyo_account where lastSignDay<:today")
    suspend fun getNotSignAccounts(today: Long): List<MihoyoAccount>
}