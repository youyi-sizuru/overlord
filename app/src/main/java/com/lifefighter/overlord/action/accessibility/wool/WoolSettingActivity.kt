package com.lifefighter.overlord.action.accessibility.wool

import android.app.Service
import android.content.Intent
import android.graphics.*
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Message
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import com.lifefighter.base.BaseActivity
import com.lifefighter.overlord.AppConst
import com.lifefighter.overlord.R
import com.lifefighter.overlord.databinding.ActivityWoolSettingBinding
import com.lifefighter.overlord.databinding.WoolAppItemBinding
import com.lifefighter.overlord.databinding.WoolSettingFloatWindowBinding
import com.lifefighter.utils.*
import com.lifefighter.widget.adapter.DataBindingAdapter
import com.lifefighter.widget.adapter.ViewItemBinder
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.ShowPattern


/**
 * @author xzp
 * @created on 2022/2/14.
 */
class WoolSettingActivity : BaseActivity<ActivityWoolSettingBinding>() {
    override fun getLayoutId(): Int = R.layout.activity_wool_setting

    private lateinit var mMediaProjectionLauncher: ActivityResultLauncher<Intent>
    private lateinit var model: Model
    private lateinit var woolAppAdapter: DataBindingAdapter

    override fun onLifecycleInit(savedInstanceState: Bundle?) {
        model = Model()
        viewBinding.m = model
        woolAppAdapter = DataBindingAdapter().apply {
            addItemBinder(
                ViewItemBinder<WoolAppModel, WoolAppItemBinding>(
                    R.layout.wool_app_item,
                    this@WoolSettingActivity
                ).apply {
                    onItemClick = { _, model, _ ->
                        model.toggle()
                    }
                }
            )
            addData(
                listOf(
                    WoolAppModel("淘宝", "com.taobao.taobao"),
                    WoolAppModel("支付宝", "com.eg.android.AlipayGphone"),
                    WoolAppModel("京东", "com.jingdong.app.mall"),
                    WoolAppModel("京东金融", "com.jd.jrapp")
                )
            )
        }
        viewBinding.listView.adapter = woolAppAdapter
        viewBinding.listView.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )
        viewBinding.start.setOnClickListener {
            val serviceConnection = model.mServiceConnectionData.value
            if (serviceConnection != null) {
                unBindService()

                return@setOnClickListener
            }
            if (AccessibilityServiceUtils.isAccessibilityServiceEnabled(
                    this,
                    WoolAccessibilityService::class
                ).not()
            ) {
                toast(R.string.service_not_start_tips)
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                return@setOnClickListener
            }
            val selectedApp = woolAppAdapter.data.filter {
                it is WoolAppModel && it.selectedData.value.orFalse()
            }.map {
                it as WoolAppModel
            }
            if (selectedApp.isEmpty()) {
                toast("请选择你需要的APP")
                return@setOnClickListener
            }
            AppConfigsUtils.putString(AppConst.WOOL_APP_PACKAGE_NAMES,
                selectedApp.joinToString(separator = ",") {
                    it.packageName
                })
            createWoolSettingFloat()
        }
        mMediaProjectionLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                launch {
                    val data = it.data
                    if (data != null && it.resultCode == RESULT_OK) {
                        val connection = ServiceConnectionWithMessenger().also {
                            model.mServiceConnectionData.value = it
                        }
                        bindService(
                            Intent(this@WoolSettingActivity, WoolRecordService::class.java),
                            connection,
                            Service.BIND_AUTO_CREATE
                        )
                        connection.sendMessage(
                            Message.obtain(
                                null,
                                WoolRecordService.WHAT_START_RECORD,
                                data
                            )
                        )
                    }
                }
            }
    }

    private fun createWoolSettingFloat() {
        if (EasyFloat.getFloatView(AppConst.WOOL_FLOAT_TAG) == null) {
            EasyFloat.with(this).setLayout(R.layout.wool_setting_float_window)
                .setDragEnable(false)
                .setGravity(Gravity.END or Gravity.TOP)
                .setTag(AppConst.WOOL_FLOAT_TAG)
                .setShowPattern(ShowPattern.ALL_TIME)
                .registerCallback {
                    createResult { isCreate, msg, view ->
                        if (isCreate.not() || view == null) {
                            logError(msg)
                            toast("无法创建悬浮窗，请确定是否打开悬浮窗权限")
                            return@createResult
                        }
                        val rootView = (view as ViewGroup).getChildAt(0)
                        val binding =
                            DataBindingUtil.bind<WoolSettingFloatWindowBinding>(
                                rootView
                            ) ?: return@createResult
                        binding.m = model
                        binding.lifecycleOwner = this@WoolSettingActivity
                        binding.stopView.setOnClickListener {
                            unBindService()
                        }
                        val mediaProjectionManager =
                            getSystemService(MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                        if (mediaProjectionManager != null) {
                            val captureIntent: Intent =
                                mediaProjectionManager.createScreenCaptureIntent()
                            mMediaProjectionLauncher.launch(captureIntent)
                        } else {
                            toast("找不到录制服务")
                        }
                    }
                }
                .show()
        }
    }

    private fun unBindService() {
        model.mServiceConnectionData.value?.let {
            unbindService(it)
        }
        model.mServiceConnectionData.value = null
        EasyFloat.dismiss(AppConst.WOOL_FLOAT_TAG)
    }

    override fun onLifecycleDestroy() {
        unBindService()
    }

    inner class Model {
        val mServiceConnectionData = ExLiveData<ServiceConnectionWithMessenger?>(null)
        val startNameData = mServiceConnectionData.map {
            if (it == null) "开始" else "结束"
        }
        val serviceStartData = mServiceConnectionData.map {
            it != null
        }
    }
}

class WoolAppModel(val name: String, val packageName: String) {
    val selectedData = ExLiveData(false)
    fun toggle() {
        selectedData.value = selectedData.value.orFalse().not()
    }
}