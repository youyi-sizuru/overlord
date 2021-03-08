package com.lifefighter.overlord.action.sign

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.lifefighter.base.BaseActivity
import com.lifefighter.overlord.R
import com.lifefighter.overlord.databinding.ActivityMihoyoAutoSignBinding
import com.lifefighter.utils.logDebug

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
                val cookies = CookieManager.getInstance().getCookie(url)
                logDebug(cookies?.toString().orEmpty())
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
                view.loadUrl(url)
                return true
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

    override fun onDestroy() {
        super.onDestroy()
        viewBinding.web.destroy()
    }
}