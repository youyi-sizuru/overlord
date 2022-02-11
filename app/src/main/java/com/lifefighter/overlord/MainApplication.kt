package com.lifefighter.overlord

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.lifefighter.overlord.db.AppDatabaseModule
import com.lifefighter.overlord.net.AppInterfaceModule
import com.lifefighter.utils.*
import com.lifefighter.widget.adapter.DataBindingHelper
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.KoinExperimentalAPI
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * @author xzp
 * @created on 2021/3/8.
 */
@KoinApiExtension
class MainApplication : Application(), KoinComponent {
    @KoinExperimentalAPI
    override fun onCreate() {
        super.onCreate()
        DataBindingHelper.DEFAULT_BINDING_VARIABLE = BR.m
        LogUtils.init()
        EventBusManager.init()
        ToastUtils.init(this)
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MainApplication)
            workManagerFactory()
            modules(NetUtils.module, AppInterfaceModule.module)
            modules(PageInjectModule.module)
            modules(AppDatabaseModule.module)

        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mihoyoChannel = NotificationChannel(
                AppConst.MIHOYO_CHANNEL_ID,
                "原神签到",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            mihoyoChannel.description = "原神签到是否成功的通知"
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mihoyoChannel)
        }
        AppConfigsUtils.init(this)
    }
}