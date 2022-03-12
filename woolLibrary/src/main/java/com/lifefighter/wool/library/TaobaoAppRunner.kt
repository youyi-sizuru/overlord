package com.lifefighter.wool.library

import com.lifefighter.proxy.wool.AppRunnerService
import com.lifefighter.utils.logDebug
import kotlinx.coroutines.delay

/**
 * @author xzp
 * @created on 2022/3/11.
 */
class TaobaoAppRunner(service: AppRunnerService) : WoolAppRunner(service, "com.taobao.taobao") {
    override suspend fun gotoEventPage(): Boolean {
        val entrance = findMaxMatchTextByPattern("^.*芭芭农场.*$")
        if (entrance != null) {
            logDebug("$TAG find entrance: $entrance")
            touchResult(entrance)
            delay(5000)
        } else {
            restartApp()
            if (findMaxMatchTextByPattern("^.*芭芭农场.*$") == null) {
                logDebug("$TAG can't find entrance")
                return false
            }
        }
        val eventFlag = findMaxMatchTextByPattern("^.*施肥.*$")
        if (eventFlag != null) {
            logDebug("$TAG find event flag: $eventFlag")
            return true
        }
        logDebug("$TAG can't find event flag: 施肥")
        return false
    }

    override suspend fun touchEventAndBack(): Boolean {
        back()
        delay(3000)
        restartApp()
        delay(5000)
        return true
    }

    companion object {
        const val TAG = "[TAOBAO]"
    }
}