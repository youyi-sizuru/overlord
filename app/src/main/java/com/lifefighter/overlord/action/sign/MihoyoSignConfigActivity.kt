package com.lifefighter.overlord.action.sign

import android.content.Context
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.whenResumed
import androidx.work.*
import com.lifefighter.base.BaseActivity
import com.lifefighter.base.withLoadingDialog
import com.lifefighter.overlord.AppConst
import com.lifefighter.overlord.R
import com.lifefighter.overlord.databinding.ActivityMihoyoSignConfigBinding
import com.lifefighter.overlord.databinding.MihoyoAccountItemBinding
import com.lifefighter.overlord.db.MihoyoAccount
import com.lifefighter.overlord.db.MihoyoAccountDao
import com.lifefighter.overlord.net.MihoyoException
import com.lifefighter.overlord.net.MihoyoInterface
import com.lifefighter.overlord.net.MihoyoSignRequest
import com.lifefighter.utils.orZero
import com.lifefighter.utils.toast
import com.lifefighter.utils.ui
import com.lifefighter.widget.adapter.DataBindingAdapter
import com.lifefighter.widget.adapter.ViewItemBinder
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.java.KoinJavaComponent.getKoin
import java.util.concurrent.TimeUnit

/**
 * @author xzp
 * @created on 2021/3/13.
 */
class MihoyoSignConfigActivity : BaseActivity<ActivityMihoyoSignConfigBinding>() {
    override fun getLayoutId(): Int = R.layout.activity_mihoyo_sign_config
    private val adapter = DataBindingAdapter()
    override fun onLifecycleInit(savedInstanceState: Bundle?) {
        viewBinding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.addAccountButton -> {
                    route(AddMihoyoAccountActivity::class)
                }
                R.id.signButton -> {
                    signAllAccount()
                }
            }
            true
        }
        viewBinding.listView.adapter = adapter
        adapter.addItemBinder(
            ViewItemBinder<MihoyoAccountItemModel, MihoyoAccountItemBinding>(
                R.layout.mihoyo_account_item,
                this
            )
        )
        viewBinding.refreshLayout.setOnRefreshListener { layout ->
            launch(failure = {
                layout.finishRefresh(false)
            }) {
                val accountList = get<MihoyoAccountDao>().getAll().map {
                    MihoyoAccountItemModel(it)
                }
                adapter.setList(accountList)
                layout.finishRefresh()
            }
        }
        launch {
            whenResumed {
                viewBinding.refreshLayout.autoRefresh()
            }
        }

        viewBinding.signButton.setOnClickListener {
            SignWork.startWork(rootContext)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: MihoyoAccountAddEvent) {
        viewBinding.refreshLayout.autoRefresh()
    }

    private fun signAllAccount() {
        launch {
            val accountDao = get<MihoyoAccountDao>()
            val mihoyoInterface = get<MihoyoInterface>()
            val accountList = accountDao.getAll().filter {
                !it.todaySigned
            }
            if (accountList.isEmpty()) {
                toast("所有账户都已经签到过了")
                return@launch
            }
            accountList.filter {
                !it.todaySigned
            }.forEach { account ->
                try {
                    mihoyoInterface.signGenshin(
                        cookie = account.cookie,
                        request = MihoyoSignRequest(region = account.region, uid = account.uid)
                    )
                } catch (e: MihoyoException) {
                    when (e.code) {
                        AppConst.MIHOYO_ALREADY_SIGN_CODE -> {
                            toast("${account.nickname}已经签到过了")
                        }
                        else -> {
                            toast("签到失败：${e.message}")
                        }
                    }
                }
                val signInfo = mihoyoInterface.getSignInfo(
                    cookie = account.cookie,
                    region = account.region,
                    uid = account.uid
                )
                accountDao.update(
                    account.copy(
                        lastSignDay = if (signInfo.sign != true) account.lastSignDay else System.currentTimeMillis(),
                        signDays = signInfo.totalSignDay.orZero()
                    )
                )
            }
            viewBinding.refreshLayout.autoRefresh()
        }.withLoadingDialog(this)
    }
}

class MihoyoAccountItemModel(account: MihoyoAccount) {
    val name = "${account.nickname}(${account.regionName})"
    val todaySigned = account.todaySigned
    val signMessage = "本月已经签到${account.signDays}天"
    val logoText = account.nickname.firstOrNull()?.toString() ?: "?"
}

class SignWork(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val accountDao = getKoin().get<MihoyoAccountDao>()
            val mihoyoInterface = getKoin().get<MihoyoInterface>()
            accountDao.getAll().filter {
                !it.todaySigned
            }.forEach {
                try {
                    mihoyoInterface.signGenshin(
                        cookie = it.cookie,
                        request = MihoyoSignRequest(region = it.region, uid = it.uid)
                    )
                    notifySignResult(it, true)
                    updateAccountInfo(it)
                } catch (e: MihoyoException) {
                    when (e.code) {
                        -5003 -> {
                            notifySignResult(it, false, "你已经签到过了")
                            updateAccountInfo(it)
                        }
                        else -> {
                            notifySignResult(it, false, e.message)
                        }
                    }
                } catch (e: Throwable) {
                    notifySignResult(it, false, e.message)
                }
            }
            return Result.success()
        } catch (e: Throwable) {
            return Result.failure()
        }


    }

    private suspend fun notifySignResult(
        account: MihoyoAccount,
        success: Boolean,
        message: String? = null
    ) = ui {
        val notification =
            NotificationCompat.Builder(applicationContext, AppConst.MIHOYO_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(if (success) "签到成功" else "签到失败")
                .setContentText(if (success) "恭喜${account.nickname}, 赶紧上线领取吧!!" else "不好意思，${account.nickname},失败原因:$message")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true).build()
        NotificationManagerCompat.from(applicationContext)
            .notify(account.uid.hashCode(), notification)
    }

    private suspend fun updateAccountInfo(account: MihoyoAccount) {
        val accountDao = getKoin().get<MihoyoAccountDao>()
        val mihoyoInterface = getKoin().get<MihoyoInterface>()
        val signInfo = mihoyoInterface.getSignInfo(
            cookie = account.cookie,
            region = account.region,
            uid = account.uid
        )
        accountDao.update(
            account.copy(
                lastSignDay = if (signInfo.sign != true) account.lastSignDay else System.currentTimeMillis(),
                signDays = signInfo.totalSignDay.orZero()
            )
        )
    }

    companion object {
        fun startWork(context: Context) {
            val request = PeriodicWorkRequestBuilder<SignWork>(
                2,
                TimeUnit.HOURS
            ).addTag(SignWork::class.java.name).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    AppConst.MIHOYO_SIGN_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}