package com.lifefighter.overlord.action.sign

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
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
import com.lifefighter.utils.*
import com.lifefighter.widget.adapter.DataBindingAdapter
import com.lifefighter.widget.adapter.ViewItemBinder
import kotlinx.coroutines.delay
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * @author xzp
 * @created on 2021/3/13.
 */
class MihoyoSignConfigActivity : BaseActivity<ActivityMihoyoSignConfigBinding>() {
    override fun getLayoutId(): Int = R.layout.activity_mihoyo_sign_config
    private val adapter = DataBindingAdapter()
    private lateinit var model: Model
    override fun onLifecycleInit(savedInstanceState: Bundle?) {
        model = Model()
        viewBinding.m = model
        viewBinding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.addAccountButton -> {
                    route(AddMihoyoAccountActivity::class)
                }
                R.id.signMenuButton -> {
                    signAllAccount()
                }
            }
            true
        }
        viewBinding.listView.adapter = adapter
        val mihoyoItemBinder = ViewItemBinder<MihoyoAccountItemModel, MihoyoAccountItemBinding>(
            R.layout.mihoyo_account_item,
            this
        )
        mihoyoItemBinder.onItemClick = { _, item, position ->
            AlertDialog.Builder(this).setMessage("确定删除?")
                .setPositiveButton("确定") { _, _ ->
                    launch {
                        val accountDao = get<MihoyoAccountDao>()
                        accountDao.delete(item.account)
                        adapter.removeAt(position)
                    }
                }
                .setNegativeButton("取消", null)
                .create().show()
        }
        adapter.addItemBinder(mihoyoItemBinder)
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
            model.switchSignEnabled()
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
                    signGenshin(mihoyoInterface, account)
                    toast("恭喜${account.nickname}签到成功")
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

    inner class Model {
        private val signWorkRunningData = ExLiveData(false)
        val signEnabledTextData = signWorkRunningData.map {
            if (it.orFalse()) {
                "禁用自动签到"
            } else {
                "启用自动签到"
            }
        }

        init {
            signWorkRunningData.value =
                !WorkManagerUtils.isRequestFinishedByTag(rootContext, SignWork.TAG)
        }

        fun switchSignEnabled() {
            if (signWorkRunningData.value.orFalse()) {
                WorkManager.getInstance(rootContext).cancelAllWorkByTag(SignWork.TAG)
            } else {
                SignWork.startWork(rootContext)
            }
            signWorkRunningData.value = !signWorkRunningData.value.orFalse()
        }
    }
}

suspend fun signGenshin(mihoyoInterface: MihoyoInterface, account: MihoyoAccount) {
    var retryTimes = 3
    //验证码请求头
    val codeMap = hashMapOf<String, String>()
    while (retryTimes >= 0) {
        retryTimes--
        val result = mihoyoInterface.signGenshin(
            cookie = account.cookie,
            request = MihoyoSignRequest(region = account.region, uid = account.uid),
            headerMap = codeMap
        )
        if (result.riskCode == 375) {
            // 触发了验证码
            result.challenge?.let {
                codeMap["x-rpc-challenge"] = it
            }
            val validate = mihoyoInterface.getSignCode(
                gt = result.gt,
                challenge = result.challenge
            ).let {
                val pattern = Pattern.compile("^.+\"validate\": \"(.+?)\".+$")
                val matcher = pattern.matcher(it)
                if(matcher.matches()){
                    matcher.group(1)
                }else {
                    null
                }
            } ?: break
            codeMap["x-rpc-validate"] = validate
            codeMap["x-rpc-seccode"] = "$validate|jordan"
            delay(3000)
        } else {
            return
        }
    }
    throw MihoyoException(code = -1000, "验证码无法破解")
}

class MihoyoAccountItemModel(val account: MihoyoAccount) {
    val name = "${account.nickname}(${account.regionName})"
    val todaySigned = account.todaySigned
    val signMessage = "本月已经签到${account.signDays}天"
    val logoText = account.nickname.firstOrNull()?.toString() ?: "?"
    val levelDesc = "等级: ${account.level}"
}

class SignWork(
    context: Context,
    private val accountDao: MihoyoAccountDao,
    private val mihoyoInterface: MihoyoInterface,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            accountDao.getAll().filter {
                !it.todaySigned
            }.forEach {
                try {
                    signGenshin(mihoyoInterface, it)
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
        val signInfo = mihoyoInterface.getSignInfo(
            cookie = account.cookie,
            region = account.region,
            uid = account.uid
        )
        val userInfo = tryOrNull {
            mihoyoInterface.getRoles(account.cookie).list?.firstOrNull {
                it.gameUid == account.uid && it.region == account.region
            }
        }
        if (userInfo == null) {
            accountDao.update(
                account.copy(
                    lastSignDay = if (signInfo.sign != true) account.lastSignDay else System.currentTimeMillis(),
                    signDays = signInfo.totalSignDay.orZero()
                )
            )
        } else {
            accountDao.update(
                account.copy(
                    nickname = userInfo.nickname.orEmpty(),
                    level = userInfo.level.orZero(),
                    lastSignDay = if (signInfo.sign != true) account.lastSignDay else System.currentTimeMillis(),
                    signDays = signInfo.totalSignDay.orZero()
                )
            )
        }
    }

    companion object {
        val TAG = SignWork::class.java.name
        fun startWork(context: Context) {
            val request = PeriodicWorkRequestBuilder<SignWork>(
                1,
                TimeUnit.HOURS
            ).addTag(TAG).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    AppConst.MIHOYO_SIGN_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}