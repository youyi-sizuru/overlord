package com.lifefighter.wool.library

import com.lifefighter.proxy.wool.AppRunner
import com.lifefighter.proxy.wool.AppRunnerService
import com.lifefighter.proxy.wool.WoolProxy

/**
 * @author xzp
 * @created on 2022/3/11.
 */
class WoolProxyImpl : WoolProxy {
    override fun getAppRunner(service: AppRunnerService, appName: String): AppRunner? {
        return when (appName) {
            "com.taobao.taobao" -> TaobaoAppRunner(service)
            "com.eg.android.AlipayGphone" -> null
            "com.jingdong.app.mall" -> null
            "com.jd.jrapp" -> null
            else -> null
        }
    }
}