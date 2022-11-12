package com.lifefighter.overlord.action.sign

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
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
import com.lifefighter.utils.*
import com.lifefighter.widget.adapter.DataBindingAdapter
import com.lifefighter.widget.adapter.ViewItemBinder
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.TimeUnit

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
        mihoyoItemBinder.onItemClick = { _, item, _ ->
            showMoreOptions(item)
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

    private fun showMoreOptions(item: MihoyoAccountItemModel) {
        val dialogBuilder = AlertDialog.Builder(this)
        val options = arrayOf(DELETE_OPTION, CHANGE_UA_OPTION)
        dialogBuilder.setItems(options) { _, which ->
            when (options[which]) {
                DELETE_OPTION -> delete(item)
                CHANGE_UA_OPTION -> changeUserAgent(item)
            }
        }
        dialogBuilder.create().show()
    }

    private fun delete(item: MihoyoAccountItemModel) {
        AlertDialog.Builder(this).setMessage("确定删除?")
            .setPositiveButton("确定") { _, _ ->
                launch {
                    val accountDao = get<MihoyoAccountDao>()
                    accountDao.delete(item.account)
                    val position = adapter.getItemPosition(item)
                    if (position >= 0) {
                        adapter.removeAt(position)
                    }
                }
            }
            .setNegativeButton("取消", null)
            .create().show()
    }

    private fun changeUserAgent(item: MihoyoAccountItemModel) {
        val dialogBuilder = AlertDialog.Builder(this)
        val contentView = LayoutInflater.from(this).inflate(R.layout.mihoyo_change_user_agent, null)
        val editText = contentView.findViewById<EditText>(R.id.editText)
        editText.setText(item.account.userAgent.orEmpty())
        dialogBuilder.setView(contentView)
            .setPositiveButton("确定") { _, _ ->
                launch {
                    val accountDao = get<MihoyoAccountDao>()
                    val newUserAgent = editText.text.toString()
                    item.account.userAgent = newUserAgent
                    accountDao.update(item.account)
                }
            }
            .setNegativeButton("取消", null)

        dialogBuilder.create().show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: MihoyoAccountAddEvent) {
        viewBinding.refreshLayout.autoRefresh()
    }

    private fun signAllAccount() {
        launch {
            val signHelper = get<MihoyoSignHelper>()
            val accountList = signHelper.getUnsignedAccount()
            if (accountList.isEmpty()) {
                toast("所有账户都已经签到过了")
                return@launch
            }
            accountList.forEach { account ->
                try {
                    signHelper.signGenshin(account)
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
                signHelper.updateAccountInfo(account)
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

    companion object {
        const val DELETE_OPTION = "删除"
        const val CHANGE_UA_OPTION = "修改UserAgent"
    }
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
    private val signHelper: MihoyoSignHelper,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            signHelper.getUnsignedAccount().forEach {
                try {
                    signHelper.signGenshin(it)
                    notifySignResult(it, true)
                    signHelper.updateAccountInfo(it)
                } catch (e: MihoyoException) {
                    when (e.code) {
                        AppConst.MIHOYO_ALREADY_SIGN_CODE -> {
                            notifySignResult(it, false, "你已经签到过了")
                            signHelper.updateAccountInfo(it)
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


    companion object {
        val TAG: String = SignWork::class.java.name
        fun startWork(context: Context) {
            val request = PeriodicWorkRequestBuilder<SignWork>(
                4,
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