package com.lifefighter.overlord

import android.app.Application
import com.lifefighter.widget.adapter.DataBindingHelper

/**
 * @author xzp
 * @created on 2021/3/8.
 */
class MainApplication : Application(){
    override fun onCreate() {
        super.onCreate()
        DataBindingHelper.DEFAULT_BINDING_VARIABLE = BR.m
    }
}