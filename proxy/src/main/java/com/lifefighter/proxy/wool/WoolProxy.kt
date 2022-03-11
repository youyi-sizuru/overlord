package com.lifefighter.proxy.wool

/**
 * @author xzp
 * @created on 2022/3/8.
 */
interface WoolProxy {
    fun getAppRunner(service: AppRunnerService, appName: String): AppRunner?
}