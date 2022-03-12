package com.lifefighter.overlord.action.accessibility.wool

import android.annotation.TargetApi
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.lifefighter.utils.EventBusManager
import com.lifefighter.widget.accessibility.ExAccessibilityService
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


/**
 * @author xzp
 * @created on 2022/2/10.
 */
@TargetApi(Build.VERSION_CODES.N)
class WoolAccessibilityService : ExAccessibilityService() {

    override fun onStart() {
        EventBusManager.register(this)
    }


    override fun onStop() {
        EventBusManager.unregister(this)

    }

    override fun onReceiveEvent(event: AccessibilityEvent?) {
        EventBusManager.post(AccessibilityPackageNameEvent(event?.packageName?.toString()))
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: AccessibilityTouchEvent) {
        clickPoint(event.point)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: AccessibilityScrollEvent) {
        scrollTo(event.from, event.to)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: AccessibilityGlobalEvent) {
        performGlobalAction(event.action)
    }

}

