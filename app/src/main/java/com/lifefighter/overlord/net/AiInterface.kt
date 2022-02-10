package com.lifefighter.overlord.net

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * @author xzp
 * @created on 2022/2/10.
 */

interface AiInterface {
    @GET("openapi/api?key=49de46c409c047d19b2ed2285e8775a6")
    suspend fun api(
        @Query("info") msg: String
    ): String?
}