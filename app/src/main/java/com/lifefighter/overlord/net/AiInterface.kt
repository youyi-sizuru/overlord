package com.lifefighter.overlord.net

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * @author xzp
 * @created on 2022/2/10.
 */

interface AiInterface {
    @GET("api.php?key=free&appid=0")
    suspend fun api(
        @Query("msg") msg: String
    ): String?
}