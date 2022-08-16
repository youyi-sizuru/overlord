package com.lifefighter.overlord

import android.app.WallpaperManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.lifefighter.base.BaseActivity
import com.lifefighter.base.string
import com.lifefighter.overlord.action.accessibility.wechat.WechatAccessibilityService
import com.lifefighter.overlord.action.accessibility.wechat.WechatAccessibilitySettingActivity
import com.lifefighter.overlord.action.sign.MihoyoSignConfigActivity
import com.lifefighter.overlord.action.wallpaper.WallpaperActivity
import com.lifefighter.overlord.databinding.ActivityMainBinding
import com.lifefighter.overlord.databinding.MainToolItemBinding
import com.lifefighter.overlord.model.ToolData
import com.lifefighter.utils.AccessibilityServiceUtils
import com.lifefighter.utils.orTrue
import com.lifefighter.utils.toast
import com.lifefighter.utils.tryOrNull
import com.lifefighter.widget.adapter.DataBindingAdapter
import com.lifefighter.widget.adapter.ViewItemBinder

class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun getLayoutId(): Int = R.layout.activity_main
    override fun onLifecycleInit(savedInstanceState: Bundle?) {
        val adapter = DataBindingAdapter()
        adapter.addItemBinder(
            ViewItemBinder<ToolData, MainToolItemBinding>(
                R.layout.main_tool_item,
                this
            ).also {
                it.onItemClick = ::onToolClick
            }
        )
        adapter.addData(ToolData(0, string(R.string.mihoyo_auto_sign_title)))
        adapter.addData(ToolData(1, string(R.string.wallpaper_title)))
        adapter.addData(ToolData(2, string(R.string.wechat_support_title)))
        viewBinding.list.adapter = adapter
        viewBinding.list.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))
    }

    private fun onToolClick(binding: MainToolItemBinding, data: ToolData, position: Int) {
        when (data.id) {
            0L -> {
                route(MihoyoSignConfigActivity::class)
            }
            1L -> {
                val wallpaperManager = WallpaperManager.getInstance(this)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    toast("系统版本过低，请更新系统至android 7.0以上")
                    return
                }
                if (tryOrNull { wallpaperManager.isWallpaperSupported.not() }.orTrue()) {
                    toast("该系统暂不支持壁纸服务")
                    return
                }
                if (tryOrNull { wallpaperManager.isSetWallpaperAllowed.not() }.orTrue()) {
                    toast("该系统暂不支持自定义壁纸")
                    return
                }
                route(WallpaperActivity::class)
            }
            2L -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    toast("系统版本过低，请更新系统至android 7.0以上")
                    return
                }
                if (AccessibilityServiceUtils.isAccessibilityServiceEnabled(
                        this,
                        WechatAccessibilityService::class
                    )
                ) {
                    route(WechatAccessibilitySettingActivity::class)
                } else {
                    toast(R.string.service_not_start_tips)
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
        }
    }
}