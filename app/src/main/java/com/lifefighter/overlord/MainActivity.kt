package com.lifefighter.overlord

import android.os.Bundle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.lifefighter.base.BaseActivity
import com.lifefighter.base.string
import com.lifefighter.overlord.action.sign.MihoyoSignConfigActivity
import com.lifefighter.overlord.databinding.ActivityMainBinding
import com.lifefighter.overlord.databinding.MainToolItemBinding
import com.lifefighter.overlord.model.ToolData
import com.lifefighter.widget.adapter.DataBindingAdapter
import com.lifefighter.widget.adapter.ViewItemBinder

class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun getLayoutId(): Int = R.layout.activity_main
    override fun onLifecycleInit(savedInstanceState: Bundle?) {
        val adapter = DataBindingAdapter()
        adapter.addItemBinder(
            ViewItemBinder<ToolData, MainToolItemBinding>(
                R.layout.main_tool_item,
                this
            ).also {
                it.onItemClick = ::onToolClick
            }
        )
        adapter.addData(ToolData(0, string(R.string.mihoyo_auto_sign_title)))
        viewBinding.list.adapter = adapter
        viewBinding.list.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))
    }

    private fun onToolClick(binding: MainToolItemBinding, data: ToolData, position: Int) {
        when (data.id) {
            0L -> {
                route(MihoyoSignConfigActivity::class)
            }
        }
    }
}