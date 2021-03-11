package com.lifefighter.overlord.action.sign

import android.os.Bundle
import android.webkit.*
import com.lifefighter.base.BaseActivity
import com.lifefighter.overlord.R
import com.lifefighter.overlord.databinding.ActivityMihoyoAutoSignBinding
import com.lifefighter.overlord.net.MihoyoInterface
import com.lifefighter.overlord.net.MihoyoSignRequest
import com.lifefighter.utils.logDebug
import com.lifefighter.utils.toJsonOrEmpty
import kotlinx.coroutines.delay

/**
 * @author xzp
 * @created on 2021/3/8.
 */
class MihoyoAutoSignActivity : BaseActivity<ActivityMihoyoAutoSignBinding>() {
    override fun getLayoutId(): Int = R.layout.activity_mihoyo_auto_sign
    override fun onLifecycleInit(savedInstanceState: Bundle?) {
        setSupportActionBar(viewBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        viewBinding.toolbar.setNavigationOnClickListener {
            finish()
        }
        viewBinding.web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                startSign()
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
                view.loadUrl(url)
                return true
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                return super.shouldInterceptRequest(view, request)
            }
        }
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
        viewBinding.web.removeJavascriptInterface("searchBoxJavaBridge_")
        viewBinding.web.scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
        viewBinding.web.loadUrl("https://bbs.mihoyo.com/ys/")
    }

    private fun startSign() {
        launch {
            val cookie = CookieManager.getInstance().getCookie("https://bbs.mihoyo.com").orEmpty()
            if (!cookie.contains("account_id")) {
                return@launch
            }
            val mihoyoInterface = get<MihoyoInterface>()
            val roleList = mihoyoInterface.getRoles(cookie = cookie).list
            roleList?.forEach {
                val signInfo =
                    mihoyoInterface.getSignInfo(
                        cookie = cookie,
                        region = it.region.orEmpty(),
                        uid = it.gameUid.orEmpty()
                    )
                logDebug(signInfo.toJsonOrEmpty())
                delay(1000)
                val result = mihoyoInterface.signGenshin(
                    cookie = cookie,
                    request = MihoyoSignRequest(
                        region = it.region.orEmpty(),
                        uid = it.gameUid.orEmpty()
                    )
                )
                logDebug(result.toString())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewBinding.web.destroy()
    }
}