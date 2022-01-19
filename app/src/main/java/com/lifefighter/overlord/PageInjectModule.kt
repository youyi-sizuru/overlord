package com.lifefighter.overlord

import com.lifefighter.overlord.action.sign.AddMihoyoAccountActivity
import com.lifefighter.overlord.action.sign.MihoyoSignConfigActivity
import com.lifefighter.overlord.action.wallpaper.WallpaperActivity
import org.koin.dsl.module

/**
 * @author xzp
 * @created on 2021/3/9.
 */
object PageInjectModule {
    val module = module {
        scope<MainActivity> { }
        scope<AddMihoyoAccountActivity> { }
        scope<MihoyoSignConfigActivity> { }
        scope<WallpaperActivity> { }
    }
}