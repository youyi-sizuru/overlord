package com.lifefighter.overlord.action.accessibility.wool

import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.whenStarted
import com.lifefighter.base.BaseService
import com.lifefighter.overlord.AppConst
import com.lifefighter.overlord.R
import com.lifefighter.utils.ServiceBinder
import com.lifefighter.utils.ui

/**
 * @author xzp
 * @created on 2022/2/14.
 */
class WoolRecordService : BaseService() {
    private var mMediaProjection: MediaProjection? = null

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

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return ServiceBinder(this)
    }

    suspend fun startRecord(resultCode: Int, data: Intent): MediaProjection? = ui {
        whenStarted {
            if (mMediaProjection == null) {
                val mediaProjectionManager =
                    getSystemService(MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                mMediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)
            }
            mMediaProjection
        }
    }
}