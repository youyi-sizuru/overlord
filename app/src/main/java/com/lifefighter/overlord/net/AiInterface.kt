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
    @GET("openapi/api?key=49de46c409c047d19b2ed2285e8775a6")
    suspend fun api(
        @Query("info") msg: String
    ): String?

    @GET("https://api.qingyunke.com/api.php?key=free&appid=0")
    suspend fun qingyunke(@Query("msg") msg: String, @Query("_") timestamp: Long): String?
}

object AiChat {
    suspend fun getChatResult(receiveText: String): String = bg {
        val aiInterface = getKoin().get<AiInterface>()
        for (i in 0 until 2) {
            val content: String? = tryOrNull {
                val result = aiInterface.api(receiveText).parseMapJson<String>()
                if (result["code"]?.toInt() == 0) {
                    result["content"]
                } else {
                    null
                }
            } ?: tryOrNull {
                aiInterface.qingyunke(receiveText, System.currentTimeMillis())
                    .parseMapJson<String>()["content"]?.replace("{br}", "\n")
            }
            if (content != null) {
                return@bg content
            }
        }
        return@bg "我没有听懂，能再说一遍吗?"
    }
}