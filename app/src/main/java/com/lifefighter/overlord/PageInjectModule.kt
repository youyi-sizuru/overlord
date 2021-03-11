package com.lifefighter.overlord

import com.lifefighter.overlord.action.sign.MihoyoAutoSignActivity
import org.koin.dsl.module

/**
 * @author xzp
 * @created on 2021/3/9.
 */
object PageInjectModule {
    val module = module {
        scope<MainActivity> { }
        scope<MihoyoAutoSignActivity> { }
    }
}