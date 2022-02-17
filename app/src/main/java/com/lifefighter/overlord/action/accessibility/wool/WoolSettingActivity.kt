package com.lifefighter.overlord.action.accessibility.wool

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.*
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.Bundle
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
    private var mServiceConnection: ServiceConnectionWithService? = null
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
                        val screenPoint = getScreenRealSize()
                        val mediaProjection = service?.startRecord(it.resultCode, data)

                        mediaProjection?.createVirtualDisplay(
                            "record",
                            screenPoint.x,
                            screenPoint.y,
                            1,
                            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                            ImageReader.newInstance(
                                screenPoint.x,
                                screenPoint.y,
                                PixelFormat.RGBA_8888,
                                2
                            ).also {
                                mScreenImageReader = it
                            }.surface, null, null
                        )
                    }
                }
            }
        launch {
            OcrLibrary.initLibrary(assets)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = Color.BLUE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp2px(3).toFloat()
            while (true) {
                val image = bg { tryOrNull { mScreenImageReader?.acquireLatestImage() } }
                if (image != null) {
                    val screenBitmap = bg {
                        tryOrNull {
                            val width = image.width
                            val height = image.height
                            val plane = image.planes[0]
                            val pixelStride = plane.pixelStride
                            val rowStride = plane.rowStride
                            val rowPadding = rowStride - pixelStride * width
                            Bitmap.createBitmap(
                                width + rowPadding / pixelStride,
                                height,
                                Bitmap.Config.ARGB_8888
                            ).also {
                                it.copyPixelsFromBuffer(plane.buffer)
                            }
                        }
                    }
                    if (screenBitmap != null) {
                        val resultArray = OcrLibrary.detectBitmap(screenBitmap, true).orEmpty()
                        val resultBitmap = Bitmap.createBitmap(
                            screenBitmap.width,
                            screenBitmap.height,
                            Bitmap.Config.RGB_565
                        )
                        val canvas = Canvas(resultBitmap)
                        canvas.drawBitmap(screenBitmap, 0f, 0f, null)
                        for (result in resultArray) {
                            canvas.drawRect(
                                RectF(result.x0, result.y0, result.x2, result.y2),
                                paint
                            )
                        }
                        viewBinding.image.setImageBitmap(resultBitmap)
                    }

                    image.close()
                }
                delay(2000)
            }
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