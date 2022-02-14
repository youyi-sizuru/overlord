package com.lifefighter.overlord.action.accessibility.wool

import android.app.Service
import android.content.Intent
import android.hardware.display.DisplayManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.lifefighter.base.BaseActivity
import com.lifefighter.overlord.R
import com.lifefighter.overlord.databinding.ActivityWoolSettingBinding
import com.lifefighter.utils.ServiceConnectionWithService
import com.lifefighter.utils.launch
import com.lifefighter.utils.toast


/**
 * @author xzp
 * @created on 2022/2/14.
 */
class WoolSettingActivity : BaseActivity<ActivityWoolSettingBinding>() {
    override fun getLayoutId(): Int = R.layout.activity_wool_setting
    private var mServiceConnection: ServiceConnectionWithService? = null
    private lateinit var mMediaProjectionLauncher: ActivityResultLauncher<Intent>
    override fun onLifecycleInit(savedInstanceState: Bundle?) {
        viewBinding.start.setOnClickListener {
            if (mServiceConnection != null) {
                toast("录制已经开始了")
                return@setOnClickListener
            }
            val mediaProjectionManager =
                getSystemService(MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            if (mediaProjectionManager != null) {
                val captureIntent: Intent = mediaProjectionManager.createScreenCaptureIntent()
                mMediaProjectionLauncher.launch(captureIntent)
            } else {
                toast("找不到录制服务")
            }
        }
        mMediaProjectionLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                launch {
                    val data = it.data
                    if (data != null) {
                        val connection = ServiceConnectionWithService().also {
                            mServiceConnection = it
                        }
                        bindService(
                            Intent(this@WoolSettingActivity, WoolRecordService::class.java),
                            connection,
                            Service.BIND_AUTO_CREATE
                        )
                        val service = connection.getService() as? WoolRecordService
                        val mediaProjection = service?.startRecord(it.resultCode, data)
                        mediaProjection?.createVirtualDisplay(
                            "record",
                            1080,
                            1920,
                            1,
                            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                            viewBinding.surface.holder.surface, null, null
                        )
                    }
                }

            }

    }


    override fun onLifecycleDestroy() {
        mServiceConnection?.let {
            unbindService(it)
        }
        mServiceConnection = null
    }
}