package com.lifefighter.overlord.action.accessibility.wool

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.*
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Messenger
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lifefighter.base.BaseService
import com.lifefighter.ocr.OcrLibrary
import com.lifefighter.overlord.AppConst
import com.lifefighter.overlord.R
import com.lifefighter.utils.ServiceBinder
import com.lifefighter.utils.getScreenRealSize
import com.lifefighter.utils.tryOrNull

/**
 * @author xzp
 * @created on 2022/2/14.
 */
@SuppressLint("WrongConstant")
class WoolRecordService : BaseService() {
    private var mMediaProjection: MediaProjection? = null
    private var mScreenImageReader: ImageReader? = null
    private val mSameProgressBinder: ServiceBinder by lazy {
        ServiceBinder(this)
    }
    private val mDiffProgressMessenger: Messenger by lazy {
        Messenger(Handler(Looper.getMainLooper()) { message ->
            if (message.what == WHAT_START_RECORD) {
                if (mMediaProjection == null) {
                    val recordIntent = message.obj as? Intent
                    if (recordIntent != null) {
                        val mediaProjectionManager =
                            getSystemService(MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                        mMediaProjection = mediaProjectionManager?.getMediaProjection(
                            RESULT_OK,
                            recordIntent
                        )
                    }
                    val screenPoint = getScreenRealSize()
                    mMediaProjection?.createVirtualDisplay(
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
                return@Handler true
            }
            return@Handler false
        })
    }

    init {
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                mMediaProjection?.stop()
                mMediaProjection = null
            } else if (event == Lifecycle.Event.ON_CREATE) {
                val notification =
                    NotificationCompat.Builder(this, AppConst.RECORD_CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("正在录制屏幕")
                        .setContentText("请不要关闭该通知")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(false).build()
                startForeground(AppConst.RECORD_SERVICE_ID, notification)
            }
        })
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        val sameProgress = intent.getBooleanExtra(INTENT_SAME_PROGRESS, false)
        return if (sameProgress) {
            mSameProgressBinder
        } else {
            mDiffProgressMessenger.binder
        }
    }

    @WorkerThread
    @Synchronized
    fun acquireScreenShot(): Bitmap? {
        OcrLibrary.initLibrary(assets)
        val image = tryOrNull { mScreenImageReader?.acquireLatestImage() } ?: return null
        return tryOrNull {
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
        }.also {
            image.close()
        }
    }


    companion object {
        /**
         * 该intent是否来源自同一个进程
         */
        const val INTENT_SAME_PROGRESS = "INTENT_SAME_PROGRESS"

        const val WHAT_START_RECORD = 1
    }
}