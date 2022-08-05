package com.lifefighter.wool.library

import com.lifefighter.ocr.OcrResult
import com.lifefighter.proxy.wool.AppRunnerService
import com.lifefighter.utils.bg
import kotlinx.coroutines.delay
import kotlin.math.abs

class JDAppRunner(service: AppRunnerService) : WoolAppRunner(service, "com.jingdong.app.mall") {
    override suspend fun gotoEventPage(): Boolean {
        return true
    }


    override suspend fun touchEventAndBack(): Boolean = bg {
        delay(3000)
        val completeButtonList = findTextByPattern("^去完成$")
        if (completeButtonList.isNullOrEmpty()) {
            return@bg false
        }
        completeButtonList.forEach {
            touchResult(it)
        }
        val grandTotalBar = findMaxMatchTextByPattern("^累计浏览.*$")
        if (grandTotalBar != null) {
            val button = findNearestCompleteButton(grandTotalBar, completeButtonList)
            if (button != null) {
                touchResult(button, 0, 60)
                delay(5000)
                val reviewButtons = findTextByPattern("^点我浏览$")
                if(reviewButtons.isEmpty()){
                    return@bg false
                }
                for (review in reviewButtons) {
                    touchResult(review)
                    delay(3000)
                    back()
                }
                delay(1000)
                back()
                return@bg true
            }
        }
        val secondTaskBar = findMaxMatchTextByPattern("^.*浏览.*([0-9]+)s.*$")
        if (secondTaskBar != null) {
            val button = findNearestCompleteButton(secondTaskBar, completeButtonList)
            if (button != null) {
                touchResult(button)
                delay(5000 + 10000)
                back()
                return@bg true
            }
        }
        return@bg false
    }

    private fun findNearestCompleteButton(
        target: OcrResult,
        buttonList: List<OcrResult>
    ): OcrResult? {
        for (button in buttonList) {
            val offset = target.y0 - button.y3
            if (abs(offset) < 100) {
                return button
            }
        }
        return null
    }

    companion object {
        const val TAG = "[JD]"
    }
}