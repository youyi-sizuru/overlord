package com.lifefighter.overlord

import com.lifefighter.base.BaseActivity
import com.lifefighter.overlord.databinding.ActivityMainBinding

class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun getLayoutId(): Int = R.layout.activity_main
    override fun onLifecycleStart() {
        setSupportActionBar(viewBinding.toolbar)

    }
}