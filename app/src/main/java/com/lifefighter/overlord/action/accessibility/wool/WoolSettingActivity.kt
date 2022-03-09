package com.lifefighter.overlord.action.accessibility.wool

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.*
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Message
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.lifefighter.base.BaseActivity
import com.lifefighter.base.dp2px
import com.lifefighter.ocr.OcrLibrary
import com.lifefighter.overlord.R
import com.lifefighter.overlord.databinding.ActivityWoolSettingBinding
import com.lifefighter.utils.*
import kotlinx.coroutines.delay


/**
 * @author xzp
 * @created on 2022/2/14.
 */
class WoolSettingActivity : BaseActivity<ActivityWoolSettingBinding>() {
    override fun getLayoutId(): Int = R.layout.activity_wool_setting
    private var mServiceConnection: ServiceConnectionWithMessenger? = null
    private lateinit var mMediaProjectionLauncher: ActivityResultLauncher<Intent>
    private var mScreenImageReader: ImageReader? = null

    @SuppressLint("WrongConstant")
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
                    if (data != null && it.resultCode == RESULT_OK) {
                        val connection = ServiceConnectionWithMessenger().also {
                            mServiceConnection = it
                        }
                        bindService(
                            Intent(this@WoolSettingActivity, WoolRecordService::class.java),
                            connection,
                            Service.BIND_AUTO_CREATE
                        )
                        connection.sendMessage(Message.obtain().also {
                            it.what = WoolRecordService.WHAT_START_RECORD
                            it.obj = data
                        })
                    }
                }
            }
        launch {

        }
    }


    override fun onLifecycleDestroy() {
        mServiceConnection?.let {
            unbindService(it)
        }
        mServiceConnection = null
        mScreenImageReader?.close()
        mScreenImageReader = null
    }
}