package com.lifefighter.overlord.net

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface StringInterface {
    @GET("https://api.geetest.com/ajax.php?&lang=zh-cn&pt=3&client_type=web_mobile&callback=geetest_1663984420850")
    suspend fun getSignCode(
        @Query("gt") gt: String?,
        @Query("challenge") challenge: String?
    ): String
}

class GeeTestResult(
    val status: String? = null,
    val data: GeeTestData? = null
)

class GeeTestData(val result: String? = null, val validate: String? = null)
