package com.lifefighter.overlord.action.accessibility.wechat

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.lifefighter.overlord.AppConst
import com.lifefighter.overlord.net.AiChat
import com.lifefighter.utils.*
import com.lifefighter.widget.accessibility.ExAccessibilityService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.regex.Pattern


/**
 * @author xzp
 * @created on 2022/2/10.
 */
@TargetApi(Build.VERSION_CODES.N)
class WechatAccessibilityService : ExAccessibilityService() {
    private lateinit var mWechatAccessibilitySettingData: WechatAccessibilitySettingData
    private val mChatNamePattern = Pattern.compile("^当前所在页面,与(.*)的聊天$")
    private var mSendMessageJob: Job? = null
    override fun onStart() {
        EventBusManager.register(this)
        mWechatAccessibilitySettingData =
            AppConfigsUtils.getString(AppConst.WECHAT_ACCESSIBILITY_SETTING, null)
                ?.parseJsonOrNull()
                ?: WechatAccessibilitySettingData()
    }

    override fun onStop() {
        EventBusManager.unregister(this)
    }

    override fun onReceiveEvent(event: AccessibilityEvent?) {
        if (mSendMessageJob?.isActive.orFalse()) {
            return
        }
        mSendMessageJob = launch {
            bg {
                val switchButtonNode = findSwitchButton() ?: return@bg
                val windowsTitle = rootInActiveWindow?.contentDescription?.toString().orEmpty()
                sendLog("微信当前页面标题: $windowsTitle")
                val userName = getChatUserName()
                val inWhite = ui {
                    mWechatAccessibilitySettingData.whiteList.orEmpty().contains(userName)
                }
                if (inWhite.not()) {
                    sendLog("当前对象不在聊天列表中，忽略")
                    return@bg
                }
                if (switchButtonNode.contentDescription?.toString() == "切换到键盘") {
                    sendLog("发现当前是语音输入，切换模式")
                    clickNode(switchButtonNode)
                    delay(1000)
                    back()
                    delay(1000)
                    sendLog("切换完成")
                }
                val editTextNode =
                    rootInActiveWindow?.findFirstAccessibilityNodeInfoByClassName("android.widget.EditText")

                if (editTextNode == null) {
                    sendLog("找不到输入框")
                    return@bg
                }
                val bottomBarNode = editTextNode.parent?.parent ?: return@bg
                val sendButtonNode = bottomBarNode.findFirstAccessibilityNodeInfoByText("发送")
                if (sendButtonNode != null) {
                    sendLog("发现发送按钮，点击发送按钮")
                    clickNode(sendButtonNode)
                    delay(100)
                    sendLog("发送完成")
                    return@bg
                }
                sendLog("开始查找聊天记录")
                val receiveList = getReceiveText()
                if (receiveList.isEmpty()) {
                    sendLog("未发现聊天记录")
                    return@bg
                }
                sendLog("找到聊天记录:\n ${receiveList.joinToString(separator = "\n")}")
                sendLog("开始回复该聊天记录")
                val sendMessage = receiveList.map {
                    bgImmediately { AiChat.getChatResult(it) }
                }.map {
                    it.await()
                }.joinToString(separator = "\n")
                sendLog("回复内容: $sendMessage")
                editTextNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        sendMessage
                    )
                })
                delay(100)
                sendLog("回复完成")
            }
        }
    }

    private fun getReceiveText(): List<String> {
        val scrollViewNode =
            rootInActiveWindow.findFirstAccessibilityNodeInfoByClassName("android.widget.ListView")
                ?: return emptyList()
        val chatItemCount = scrollViewNode.childCount
        val screenWidth = rootInActiveWindow?.getBoundsInScreen()?.width().orZero()
        val receiveList = mutableListOf<String>()
        for (i in chatItemCount - 1 downTo 0) {
            val chatItemNode = scrollViewNode.getChild(i) ?: continue
            val imageNode =
                chatItemNode.findFirstAccessibilityNodeInfoByViewId("com.tencent.mm:id/au2")
                    ?: continue
            val imageBounds = imageNode.getBoundsInScreen()
            if (imageBounds.left > screenWidth / 2) {
                sendLog("最后一条聊天是本人，所以不再回复")
                break
            }
            val textNode =
                chatItemNode.findFirstAccessibilityNodeInfoByViewId("com.tencent.mm:id/auk")
            if (textNode?.className?.toString() != "android.widget.TextView") {
                sendLog("倒数第${chatItemCount - i}条聊天不是文本信息，忽略")
                continue
            }
            val content = textNode.text?.toString().orEmpty()
            if (content.isNotEmpty()) {
                receiveList.add(0, content)
            }
        }
        return receiveList
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: WechatAccessibilitySettingUpdateEvent) {
        mWechatAccessibilitySettingData =
            AppConfigsUtils.getString(AppConst.WECHAT_ACCESSIBILITY_SETTING, null)
                ?.parseJsonOrNull()
                ?: WechatAccessibilitySettingData()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: WechatAccessibilityAddWhiteEvent) {
        launch {
            bg {
                if (findSwitchButton() == null) {
                    return@bg
                }
                val userName = getChatUserName()
                if (userName.isNullOrEmpty()) {
                    return@bg
                }
                ui {
                    val whiteList = mWechatAccessibilitySettingData.whiteList.orEmpty()
                    if (whiteList.contains(userName).not()) {
                        mWechatAccessibilitySettingData.whiteList = whiteList.plus(userName)
                        AppConfigsUtils.putString(
                            AppConst.WECHAT_ACCESSIBILITY_SETTING,
                            mWechatAccessibilitySettingData.toJson()
                        )
                        EventBusManager.post(WechatAccessibilitySettingUpdateEvent())
                        sendLog("已添加该用户到自动聊天列表")
                    } else {
                        sendLog("该用户已经在自动聊天列表")
                    }
                }
            }
        }
    }

    private fun findSwitchButton(): AccessibilityNodeInfo? {
        val switchButtonNode =
            rootInActiveWindow?.findFirstAccessibilityNodeInfoByViewId("com.tencent.mm:id/ax7")
        val switchButtonDesc = switchButtonNode?.contentDescription?.toString()
        if (switchButtonDesc !in arrayOf("切换到键盘", "切换到按住说话")) {
            sendLog("当前页面不在聊天页")
            return null
        }
        return switchButtonNode
    }

    private fun getChatUserName(): String? {
        val windowsTitle = rootInActiveWindow?.contentDescription?.toString().orEmpty()
        sendLog("微信当前页面标题: $windowsTitle")
        val titleMatcher = mChatNamePattern.matcher(windowsTitle)
        if (titleMatcher.matches()) {
            val userName = titleMatcher.group(1).orEmpty()
            sendLog("微信当前聊天对象: $userName")
            return userName
        }
        return null
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: WechatAccessibilityDeleteWhiteEvent) {
        launch {
            bg {
                if (findSwitchButton() == null) {
                    return@bg
                }
                val userName = getChatUserName()
                if (userName.isNullOrEmpty()) {
                    return@bg
                }
                ui {
                    val whiteList = mWechatAccessibilitySettingData.whiteList.orEmpty()
                    if (whiteList.contains(userName)) {
                        mWechatAccessibilitySettingData.whiteList = whiteList.toMutableList().also {
                            it.remove(userName)
                        }
                        AppConfigsUtils.putString(
                            AppConst.WECHAT_ACCESSIBILITY_SETTING,
                            mWechatAccessibilitySettingData.toJson()
                        )
                        EventBusManager.post(WechatAccessibilitySettingUpdateEvent())
                        sendLog("已从自动聊天列表移除该用户")
                    } else {
                        sendLog("该用户不在自动聊天列表中")
                    }
                }

            }
        }
    }

    private fun sendLog(message: CharSequence) {
        logDebug(message.toString())
        EventBusManager.post(WechatAccessibilityLogEvent(message))
    }


}

