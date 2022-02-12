package com.lifefighter.overlord.action.accessibility.wechat

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.whenResumed
import com.lifefighter.base.BaseActivity
import com.lifefighter.overlord.AppConst
import com.lifefighter.overlord.R
import com.lifefighter.overlord.databinding.ActivityWechatAccessibilitySettingBinding
import com.lifefighter.overlord.databinding.WechatAccessibilityLogItemBinding
import com.lifefighter.overlord.databinding.WechatAccessibilitySettingFloatWindowBinding
import com.lifefighter.overlord.databinding.WechatWhiteListItemBinding
import com.lifefighter.utils.*
import com.lifefighter.widget.adapter.DataBindingAdapter
import com.lifefighter.widget.adapter.ViewItemBinder
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.ShowPattern
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * @author xzp
 * @created on 2022/2/11.
 */
class WechatAccessibilitySettingActivity :
    BaseActivity<ActivityWechatAccessibilitySettingBinding>() {
    override fun getLayoutId(): Int = R.layout.activity_wechat_accessibility_setting
    private val whiteAdapter = DataBindingAdapter()
    private val messageAdapter = DataBindingAdapter()
    private lateinit var model: Model
    override fun onLifecycleInit(savedInstanceState: Bundle?) {
        model = Model()
        viewBinding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.addWhiteButton -> {
                    createWechatSettingFloat()
                }
            }
            true
        }
        viewBinding.listView.adapter = whiteAdapter
        val whiteItemBinder = ViewItemBinder<String, WechatWhiteListItemBinding>(
            R.layout.wechat_white_list_item,
            this
        )


        whiteItemBinder.onItemClick = { _, item, position ->
            AlertDialog.Builder(this).setMessage("确定删除?")
                .setPositiveButton("确定") { _, _ ->
                    launch {
                        val settingData = model.settingData
                        if (settingData != null) {
                            settingData.whiteList = settingData.whiteList?.toMutableList()?.also {
                                it.removeAt(position)
                            }
                            AppConfigsUtils.putString(
                                AppConst.WECHAT_ACCESSIBILITY_SETTING,
                                settingData.toJson()
                            )
                            EventBusManager.post(WechatAccessibilitySettingUpdateEvent())
                        }
                    }
                }
                .setNegativeButton("取消", null)
                .create().show()
        }
        whiteAdapter.addItemBinder(whiteItemBinder)
        viewBinding.refreshLayout.setOnRefreshListener { layout ->
            launch(failure = {
                layout.finishRefresh(false)
            }) {
                val data = AppConfigsUtils.getString(AppConst.WECHAT_ACCESSIBILITY_SETTING, null)
                    ?.parseJsonOrNull()
                    ?: WechatAccessibilitySettingData()
                model.settingData = data
                whiteAdapter.setList(data.whiteList)
                layout.finishRefresh()
            }
        }
        val messageItemBinding =
            ViewItemBinder<WechatAccessibilityLogEvent, WechatAccessibilityLogItemBinding>(
                R.layout.wechat_accessibility_log_item,
                this
            )
        messageAdapter.addItemBinder(messageItemBinding)
        launch {
            whenResumed {
                viewBinding.refreshLayout.autoRefresh()
            }
        }
    }

    private fun createWechatSettingFloat() {
        if (EasyFloat.getFloatView(AppConst.WECHAT_FLOAT_TAG) == null) {
            EasyFloat.with(this).setLayout(R.layout.wechat_accessibility_setting_float_window)
                .setDragEnable(false)
                .setGravity(Gravity.END or Gravity.TOP)
                .setTag(AppConst.WECHAT_FLOAT_TAG)
                .setShowPattern(ShowPattern.BACKGROUND)
                .registerCallback {
                    createResult { isCreate, msg, view ->
                        if (isCreate.not() || view == null) {
                            logError(msg)
                            return@createResult
                        }
                        val rootView = (view as ViewGroup).getChildAt(0)
                        val binding =
                            DataBindingUtil.bind<WechatAccessibilitySettingFloatWindowBinding>(
                                rootView
                            ) ?: return@createResult
                        binding.closeView.setOnClickListener {
                            EasyFloat.dismiss(AppConst.WECHAT_FLOAT_TAG)
                        }
                        binding.addView.setOnClickListener {
                            EventBusManager.post(WechatAccessibilityAddWhiteEvent())
                        }
                        binding.deleteView.setOnClickListener {
                            EventBusManager.post(WechatAccessibilityDeleteWhiteEvent())
                        }
                        binding.listView.adapter = messageAdapter
                        val launchIntent =
                            packageManager.getLaunchIntentForPackage("com.tencent.mm")
                        startActivity(launchIntent)
                    }
                }
                .show()
        } else {
            val launchIntent = packageManager.getLaunchIntentForPackage("com.tencent.mm")
            startActivity(launchIntent)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: WechatAccessibilitySettingUpdateEvent) {
        launch {
            whenResumed {
                viewBinding.refreshLayout.autoRefresh()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EasyFloat.dismiss(AppConst.WECHAT_FLOAT_TAG)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: WechatAccessibilityLogEvent) {
        messageAdapter.setList(listOf(event).plus(messageAdapter.data.take(49)))
    }

    inner class Model {
        var settingData: WechatAccessibilitySettingData? = null
    }

}