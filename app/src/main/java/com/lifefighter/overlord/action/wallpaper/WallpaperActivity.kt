package com.lifefighter.overlord.action.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lifefighter.base.BaseActivity
import com.lifefighter.overlord.AppConst
import com.lifefighter.overlord.R
import com.lifefighter.overlord.action.wallpaper.bricks.BricksGame
import com.lifefighter.overlord.action.wallpaper.clock.ClockGame
import com.lifefighter.overlord.databinding.CanvasWallpaperItemBinding
import com.lifefighter.overlord.databinding.WallpaperBinding
import com.lifefighter.widget.adapter.DataBindingAdapter
import com.lifefighter.widget.adapter.ViewItemBinder
import com.tencent.mmkv.MMKV


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
            ViewItemBinder<CanvasModel, CanvasWallpaperItemBinding>(
                layoutId = R.layout.canvas_wallpaper_item,
                lifecycleOwner = this,
                customConvert = { binding, data ->
                    binding.clockView.canvasPainter = data.game
                    binding.clockView.postInvalidate()
                }
            ).also {
                it.onItemClick = { _, data, _ ->
                    MMKV.defaultMMKV().encode(AppConst.GAME_TYPE, data.game.javaClass.name)
                    startWallpaper()
                }
            }
        )
        adapter.addData(CanvasModel(ClockGame()))
        adapter.addData(CanvasModel(BricksGame().also {
            it.onStart(this)
            it.onOffset(0f, 0f)
            it.onOffset(-1f, 0f)
            this.lifecycle.addObserver(LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    it.onEnd(this)
                }
            })
        }))
    }

    override fun onResume() {
        super.onResume()
        adapter.data.forEach {
            (it as? CanvasModel)?.game?.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        adapter.data.forEach {
            (it as? CanvasModel)?.game?.onPause()
        }
    }

    private fun startWallpaper() {
        WallpaperManager.getInstance(this).clear()
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).also {
            it.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(
                    this,
                    GameWallpaperService::class.java
                )
            )
        }
        startActivity(intent)
    }
}

class CanvasModel(val game: CanvasGame) {
    val name = game.getName()
}