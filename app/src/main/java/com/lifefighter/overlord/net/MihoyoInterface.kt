package com.lifefighter.overlord.net

import com.google.gson.annotations.SerializedName
import retrofit2.http.*

/**
 * @author xzp
 * @created on 2021/3/9.
 */
interface MihoyoInterface {

    @GET("binding/api/getUserGameRolesByCookie?game_biz=hk4e_cn")
    suspend fun getRoles(
        @Header("Cookie") cookie: String
    ): MihoyoRoleListData

    @GET("event/bbs_sign_reward/info?act_id=e202009291139501")
    suspend fun getSignInfo(
        @Header("Cookie") cookie: String,
        @Query("region") region: String,
        @Query("uid") uid: String
    ): MihoyoSignInfo

    @POST("event/bbs_sign_reward/sign")
    suspend fun signGenshin(
        @Header("Cookie") cookie: String,
        @Body request: MihoyoSignRequest,
        @HeaderMap headerMap: Map<String, String>? = null
    ): MihoyoSignResult

    @GET("https://api.geetest.com/ajax.php?&lang=zh-cn&pt=3&client_type=web_mobile&callback=geetest_1663984420850")
    suspend fun getSignCode(
        @Query("gt") gt: String?,
        @Query("challenge") challenge: String?
    ): String
}

class MihoyoRoleListData(val list: List<MihoyoRole>? = null)

class MihoyoRole(
    @SerializedName("game_biz")
    val gameBiz: String? = null,
    val region: String? = null,
    @SerializedName("game_uid")
    val gameUid: String? = null,
    val nickname: String? = null,
    val level: Int? = null,
    @SerializedName("is_chosen")
    val chosen: Boolean? = null,
    @SerializedName("region_name")
    val regionName: String? = null,
    @SerializedName("is_official")
    val official: Boolean? = null
)

class MihoyoSignInfo(
    @SerializedName("total_sign_day")
    val totalSignDay: Int? = null,
    val today: String? = null,
    @SerializedName("is_sign")
    val sign: Boolean? = null,
    @SerializedName("first_bind")
    val firstBind: Boolean? = null,
    @SerializedName("is_sub")
    val sub: Boolean? = null,
    @SerializedName("month_first")
    val monthFirst: Boolean? = null
)

class MihoyoSignRequest(
    @SerializedName("act_id") val actId: String = "e202009291139501",
    val region: String,
    val uid: String
)

class MihoyoSignResult(
    val code: String? = null,
    @SerializedName("risk_code")
    val riskCode: Int? = null,
    val gt: String? = null,
    val challenge: String? = null
)


class MihoyoValidateResult(val validate: String? = null)