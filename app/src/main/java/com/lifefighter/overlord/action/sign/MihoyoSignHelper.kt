package com.lifefighter.overlord.action.sign

import com.lifefighter.overlord.db.MihoyoAccount
import com.lifefighter.overlord.db.MihoyoAccountDao
import com.lifefighter.overlord.net.*
import com.lifefighter.utils.orZero
import com.lifefighter.utils.parseJson
import com.lifefighter.utils.tryOrNull
import kotlinx.coroutines.delay
import java.util.regex.Pattern

class MihoyoSignHelper(
    private val accountDao: MihoyoAccountDao,
    private val mihoyoInterface: MihoyoInterface,
    private val stringInterface: StringInterface
) {
    suspend fun signGenshin(account: MihoyoAccount) {
        var retryTimes = 3
        //验证码请求头
        val codeMap = hashMapOf<String, String>()
        while (retryTimes >= 0) {
            retryTimes--
            val result = mihoyoInterface.signGenshin(
                cookie = account.cookie,
                request = MihoyoSignRequest(region = account.region, uid = account.uid),
                headerMap = codeMap,
                userAgent = account.userAgent
            )
            if (result.riskCode == 375) {
                // 触发了验证码
                result.challenge?.let {
                    codeMap["x-rpc-challenge"] = it
                }
                val validate = stringInterface.getSignCode(
                    gt = result.gt,
                    challenge = result.challenge
                ).let {
                    val testResult = getGeeTestResult(it)
                    testResult?.data?.let { testData ->
                        if (testData.validate.isNullOrEmpty().not()) {
                            testData.validate
                        } else {
                            null
                        }
                    }
                } ?: break

                codeMap["x-rpc-validate"] = validate
                codeMap["x-rpc-seccode"] = "$validate|jordan"
                delay(3000)
            } else {
                return
            }
        }
        throw MihoyoException(code = -1000, "验证码无法破解")
    }


    suspend fun updateAccountInfo(account: MihoyoAccount) {
        val signInfo = mihoyoInterface.getSignInfo(
            cookie = account.cookie,
            region = account.region,
            uid = account.uid
        )
        val userInfo = tryOrNull {
            mihoyoInterface.getRoles(account.cookie).list?.firstOrNull {
                it.gameUid == account.uid && it.region == account.region
            }
        }
        if (userInfo == null) {
            accountDao.update(
                account.copy(
                    lastSignDay = if (signInfo.sign != true) account.lastSignDay else System.currentTimeMillis(),
                    signDays = signInfo.totalSignDay.orZero()
                )
            )
        } else {
            accountDao.update(
                account.copy(
                    nickname = userInfo.nickname.orEmpty(),
                    level = userInfo.level.orZero(),
                    lastSignDay = if (signInfo.sign != true) account.lastSignDay else System.currentTimeMillis(),
                    signDays = signInfo.totalSignDay.orZero()
                )
            )
        }
    }

    suspend fun getUnsignedAccount(): List<MihoyoAccount> {
        return accountDao.getAll().filter {
            !it.todaySigned
        }
    }

    private fun getGeeTestResult(str: String): GeeTestResult? {
        val pattern = Pattern.compile("^.*?\\((.*)\\)$")
        val matcher = pattern.matcher(str)
        return if (matcher.matches()) {
            matcher.group(1)?.parseJson()
        } else {
            null
        }
    }
}