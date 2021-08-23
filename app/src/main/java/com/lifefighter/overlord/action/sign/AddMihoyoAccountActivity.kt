package com.lifefighter.overlord.action.sign

import android.os.Bundle
import android.view.ViewGroup
import android.webkit.*
import com.lifefighter.base.BaseActivity
import com.lifefighter.base.withLoadingDialog
import com.lifefighter.overlord.R
import com.lifefighter.overlord.databinding.ActivityAddMihoyoAccountBinding
import com.lifefighter.overlord.db.MihoyoAccount
import com.lifefighter.overlord.db.MihoyoAccountDao
import com.lifefighter.overlord.net.MihoyoInterface
import com.lifefighter.utils.EventBusManager
import com.lifefighter.utils.orZero
import com.lifefighter.utils.toast


/**
 * @author xzp
 * @created on 2021/3/8.
 */
class AddMihoyoAccountActivity : BaseActivity<ActivityAddMihoyoAccountBinding>() {
    override fun getLayoutId(): Int = R.layout.activity_add_mihoyo_account
    override fun onLifecycleInit(savedInstanceState: Bundle?) {
        viewBinding.web.settings.apply {
            userAgentString =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.190 Safari/537.36"
            javaScriptEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            allowContentAccess = true
            databaseEnabled = true
            domStorageEnabled = true
            setAppCacheEnabled(true)
            savePassword = false
            saveFormData = false
            useWideViewPort = true
        }
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        viewBinding.web.apply {
            clearCache(true)
            clearHistory()
            removeJavascriptInterface("searchBoxJavaBridge_")
            scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
            loadUrl("https://bbs.mihoyo.com/ys/")
        }
        viewBinding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.getAccountButton) {
                startSign()
            }
            true
        }
    }

    private fun startSign() {
        launch {
            val cookie = CookieManager.getInstance().getCookie("https://bbs.mihoyo.com").orEmpty()
            if (!cookie.contains("account_id")) {
                toast("尚未登录，请登录后再获取账户")
                return@launch
            }
            val mihoyoInterface = get<MihoyoInterface>()
            mihoyoInterface.getRoles(cookie = cookie).list?.map {
                val signInfo =
                    mihoyoInterface.getSignInfo(
                        cookie = cookie,
                        region = it.region.orEmpty(),
                        uid = it.gameUid.orEmpty()
                    )
                MihoyoAccount(
                    uid = it.gameUid.orEmpty(),
                    region = it.region.orEmpty(),
                    nickname = it.nickname.orEmpty(),
                    regionName = it.regionName.orEmpty(),
                    cookie = cookie,
                    lastSignDay = if (signInfo.sign != true) 0 else System.currentTimeMillis(),
                    signDays = signInfo.totalSignDay.orZero()
                )
            }?.let {
                get<MihoyoAccountDao>().add(*it.toTypedArray())
            }
            toast("添加账户成功")
            EventBusManager.post(MihoyoAccountAddEvent())
            finish()
        }.withLoadingDialog(this)
    }

    override fun onLifecycleDestroy() {
        (viewBinding.web.parent as ViewGroup).removeView(viewBinding.web)
        viewBinding.web.destroy()
    }
}

class MihoyoAccountAddEvent