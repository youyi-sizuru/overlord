package com.lifefighter.overlord.net

import com.lifefighter.utils.*
import org.koin.java.KoinJavaComponent.getKoin
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.*

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
    private val mKeys =
        arrayOf(
            "5da047a95db8450ea6e710dd065d4be4",
            "49de46c409c047d19b2ed2285e8775a6",
            "bd35d0e4054c6a4c06059f1a454bd2d3",
            "dfeb1cc8125943d29764a2f2f5c33739",
            "7c8cdb56b0dc4450a8deef30a496bd4c",
        )
    private var mTodayTime = 0L
    private val mTodayTempKeys = mutableListOf<String>()

    suspend fun getChatResult(receiveText: String): String = bg {
        val aiInterface = getKoin().get<AiInterface>()
        val content: String? = kotlin.run {
            val todayTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            if (todayTime != mTodayTime) {
                ui {
                    mTodayTime = todayTime
                    mTodayTempKeys.clear()
                    mTodayTempKeys.addAll(mKeys)
                }
            }
            val keys = mTodayTempKeys.toList()
            for (key in keys) {
                val content = tryOrNull {
                    val result = aiInterface.api(receiveText, key).parseMapJson<String>()
                    val code = result["code"]?.toInt()
                    if (code == 40004) {
                        logDebug("$key 今日的调用数量已经没有了")
                        ui {
                            mTodayTempKeys.remove(key)
                        }
                    }
                    if (code == 10000) {
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