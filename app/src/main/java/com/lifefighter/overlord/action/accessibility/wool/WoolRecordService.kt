package com.lifefighter.overlord.action.accessibility.wool

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lifefighter.base.BaseService
import com.lifefighter.ocr.OcrLibrary
import com.lifefighter.overlord.AppConst
import com.lifefighter.overlord.R
import com.lifefighter.proxy.wool.AppRunnerService
import com.lifefighter.utils.*
import com.lifefighter.wool.library.WoolProxyImpl
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * @author xzp
 * @created on 2022/2/14.
 */
@SuppressLint("WrongConstant")
class WoolRecordService : BaseService(), AppRunnerService {
    private var mMediaProjection: MediaProjection? = null
    private var mScreenImageReader: ImageReader? = null
    private val mDiffProgressMessenger: Messenger by lazy {
        Messenger(Handler(Looper.getMainLooper()) { message ->
            if (message.what == WHAT_START_RECORD) {
                logDebug("$TAG start record")
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
                    val screenPoint = application.getScreenRealSize()
                    logDebug("$TAG get screen size: ${screenPoint.x}x${screenPoint.y}")
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
                    startRunners()
                }
                return@Handler true
            }
            return@Handler false
        })
    }
    private var mAccessibilityPackageName: String? = null

    init {
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                EventBusManager.unregister(this@WoolRecordService)
                mMediaProjection?.stop()
                mMediaProjection = null
            } else if (event == Lifecycle.Event.ON_CREATE) {
                EventBusManager.register(this@WoolRecordService)
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
        return mDiffProgressMessenger.binder
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: AccessibilityPackageNameEvent) {
        if (mAccessibilityPackageName != event.packageName) {
            if (event.packageName != "com.android.systemui") {
                mAccessibilityPackageName = event.packageName
            }
            logDebug("$TAG packageName changed: ${event.packageName}")
        }
    }

    private fun startRunners() {
        launch(final = {
            mDiffProgressMessenger.send(Message.obtain().apply {
                what = WHAT_END_RECORD
            })
        }) {
            val packageNames =
                AppConfigsUtils.getString(AppConst.WOOL_APP_PACKAGE_NAMES, null) ?: return@launch
            val packageNameList = packageNames.split(",")
            for (packageName in packageNameList) {
                logDebug("$TAG start runner: $packageName")
                val runner = if (BuildConfig.DEBUG) {
                    WoolProxyImpl().getAppRunner(this@WoolRecordService, packageName)
                } else {
                    null
                }
                runner?.start()
            }
        }
    }

    @WorkerThread
    @Synchronized
    override fun acquireScreenShot(): Bitmap? {
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

    override fun touchPoint(point: Point) {
        EventBusManager.post(AccessibilityTouchEvent(point))
    }

    override fun scrollTo(from: Point, to: Point) {
        EventBusManager.post(AccessibilityScrollEvent(from, to))
    }

    override fun stop() {
        mDiffProgressMessenger.send(Message.obtain(null, WHAT_END_RECORD))
    }

    override fun getContext(): Context {
        return this
    }

    override fun getAccessibilityPackageName(): String? {
        return mAccessibilityPackageName
    }


    companion object {
        const val WHAT_START_RECORD = 1
        const val WHAT_END_RECORD = 1
        const val TAG = "[WoolRecordService]"
    }
}