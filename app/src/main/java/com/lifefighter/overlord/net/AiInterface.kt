package com.lifefighter.overlord.net

import com.lifefighter.utils.bg
import com.lifefighter.utils.parseMapJson
import com.lifefighter.utils.tryOrNull
import org.koin.java.KoinJavaComponent.getKoin
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * @author xzp
 * @created on 2022/2/10.
 */

interface AiInterface {
    @GET("openapi/api")
    suspend fun api(
        @Query("info") msg: String,
        @Query("key") key: String,
    ): String?

    @GET("https://api.qingyunke.com/api.php?key=free&appid=0")
    suspend fun qingyunke(@Query("msg") msg: String, @Query("_") timestamp: Long): String?
}

object AiChat {
    private val codes =
        arrayOf(
            "49de46c409c047d19b2ed2285e8775a6",
            "bd35d0e4054c6a4c06059f1a454bd2d3",
            "dfeb1cc8125943d29764a2f2f5c33739",
            "7c8cdb56b0dc4450a8deef30a496bd4c",
        )

    suspend fun getChatResult(receiveText: String): String = bg {
        val aiInterface = getKoin().get<AiInterface>()
        val content: String? = kotlin.run {
            for (code in codes) {
                val content = tryOrNull {
                    val result = aiInterface.api(receiveText, code).parseMapJson<String>()
                    if (result["code"]?.toInt() == 0) {
                        result["content"]
                    } else {
                        null
                    }
                }
                if (content != null) {
                    return@run content
                }
            }
            return@run null
        } ?: tryOrNull {
            aiInterface.qingyunke(receiveText, System.currentTimeMillis())
                .parseMapJson<String>()["content"]?.replace("{br}", "\n")
        }
        if (content != null) {
            return@bg content
        }
        return@bg "我没有听懂，能再说一遍吗?"
    }
}