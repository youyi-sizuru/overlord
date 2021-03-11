package com.lifefighter.overlord

import android.app.Application
import com.lifefighter.overlord.net.AppInterfaceModule
import com.lifefighter.utils.LogUtils
import com.lifefighter.utils.NetUtils
import com.lifefighter.widget.adapter.DataBindingHelper
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * @author xzp
 * @created on 2021/3/8.
 */
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DataBindingHelper.DEFAULT_BINDING_VARIABLE = BR.m
        LogUtils.init()
        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(NetUtils.netClientModule, AppInterfaceModule.module)
            modules(PageInjectModule.module)
        }
    }
}