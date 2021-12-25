package com.lifefighter.overlord.action.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import com.lifefighter.base.BaseActivity
import com.lifefighter.overlord.R
import com.lifefighter.overlord.databinding.ClockWallpaperItemBinding
import com.lifefighter.overlord.databinding.WallpaperBinding
import com.lifefighter.widget.adapter.DataBindingAdapter
import com.lifefighter.widget.adapter.ViewItemBinder
import kotlin.reflect.KClass


/**
 * @author xzp
 * @created on 2021/12/25.
 */
class WallpaperActivity : BaseActivity<WallpaperBinding>() {
    override fun getLayoutId(): Int = R.layout.wallpaper
    private val adapter = DataBindingAdapter()
    override fun onLifecycleInit(savedInstanceState: Bundle?) {
        viewBinding.listView.adapter = adapter
        adapter.addItemBinder(
            ViewItemBinder<ClockModel, ClockWallpaperItemBinding>(
                layoutId = R.layout.clock_wallpaper_item,
                lifecycleOwner = this
            ).also {
                it.onItemClick = { _, _, _ ->
                    startWallpaper(ClockWallpaperService::class)
                }
            }
        )
        adapter.addData(ClockModel())
    }

    private fun startWallpaper(wallpaperServiceClass: KClass<*>) {
        WallpaperManager.getInstance(this).clear()
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).also {
            it.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(
                    this,
                    wallpaperServiceClass.java
                )
            )
        }
        startActivity(intent)
    }
}

class ClockModel