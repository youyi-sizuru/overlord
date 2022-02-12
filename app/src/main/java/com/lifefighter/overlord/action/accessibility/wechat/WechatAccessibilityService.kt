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
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import java.util.concurrent.ConcurrentHashMap


/**
 * @author xzp
 * @created on 2022/2/10.
 */
@TargetApi(Build.VERSION_CODES.N)
class WechatAccessibilityService : ExAccessibilityService() {
    private lateinit var mWechatAccessibilitySettingData: WechatAccessibilitySettingData
    private val mChatListMap = ConcurrentHashMap<String, MutableList<String>>()
    private var mSendMessageJob: Job? = null
    private var mReceiveMessageJob: Job? = null
    override fun onStart() {
        EventBusManager.register(this)
        mWechatAccessibilitySettingData =
            AppConfigsUtils.getString(AppConst.WECHAT_ACCESSIBILITY_SETTING, null)
                ?.parseJsonOrNull()
                ?: WechatAccessibilitySettingData()
        mSendMessageJob = launch {
            bg {
                while (true) {
                    tryOrNothing {
                        startSendMessage()
                    }
                    delay(100)
                }
            }
        }
    }

    private suspend fun startSendMessage() = bg {
        val userName = getChatUserName() ?: return@bg
        val isWhite = ui {
            mWechatAccessibilitySettingData.whiteList.orEmpty().contains(userName)
        }
        if (isWhite.not()) {
            return@bg
        }
        val switchButtonNode = findSwitchButton() ?: return@bg
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

        if (mChatListMap[userName].isNullOrEmpty()) {
            return@bg
        }
        val message = mChatListMap[userName]?.firstOrNull() ?: return@bg
        val sendMessage = AiChat.getChatResult(message)
        sendLog("消息:\n${message}\n回复内容: \n$sendMessage")
        editTextNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                sendMessage
            )
        })
        delay(300)
        var findSendButton = false
        for (i in 0 until 10) {
            val sendButtonNode =
                rootInActiveWindow?.findAccessibilityNodeInfosByClassName("android.widget.Button")
                    ?.firstOrNull {
                        it.text?.toString() == "发送"
                    }
            if (sendButtonNode != null) {
                sendLog("发现发送按钮，点击发送按钮")
                clickNode(sendButtonNode)
                delay(100)
                sendLog("发送完成")
                findSendButton = true
                break
            }
            delay(100)
        }
        mChatListMap[userName]?.remove(message)
        if (findSendButton) {
            sendLog("回复完成")
        } else {
            sendLog("找不到发送按钮，估计出错了，忽略该消息")
        }
    }

    override fun onStop() {
        EventBusManager.unregister(this)
        tryOrNothing {
            mSendMessageJob?.cancel()
            mSendMessageJob = null
        }
    }

    override fun onReceiveEvent(event: AccessibilityEvent?) {
        if (mReceiveMessageJob?.isActive.orFalse()) {
            logDebug("事件太频繁了,停掉当前job等待稳定")
            mReceiveMessageJob?.cancel()
        }
        mReceiveMessageJob = launch {
            delay(100)
            startWechatChatRobot()
        }

    }

    private fun startWechatChatRobot() {
        tryOrNothing {
            if (findSwitchButton() == null) {
                sendLog(
                    "当前页面不在聊天页: ${
                        rootInActiveWindow?.contentDescription?.toString().orEmpty()
                    }"
                )
                return
            }
            findReceiveText()
        }

    }

    private fun findReceiveText() {
        sendLog("开始查找聊天记录")
        val scrollViewNode =
            rootInActiveWindow.findFirstAccessibilityNodeInfoByClassName("android.widget.ListView")
                ?: return
        val chatItemCount = scrollViewNode.childCount
        val screenWidth = rootInActiveWindow?.getBoundsInScreen()?.width().orZero()
        for (i in chatItemCount - 1 downTo 0) {
            val chatItemNode = scrollViewNode.getChild(i) ?: continue
            val imageNode =
                chatItemNode.findAccessibilityNodeInfosByClassName("android.widget.ImageView")
                    .firstOrNull {
                        it.contentDescription?.toString()?.endsWith("头像").orFalse()
                    } ?: continue
            val imageBounds = imageNode.getBoundsInScreen()
            if (imageBounds.left > screenWidth / 2) {
                sendLog("最后一条聊天是本人，所以不再回复")
                break
            }
            val userName = imageNode.contentDescription?.toString()?.removeSuffix("头像").orEmpty()
            if (mWechatAccessibilitySettingData.whiteList.orEmpty().contains(userName).not()) {
                sendLog("当前对象不在自动聊天列表中，忽略，用户名: $userName")
                break
            }
            val textNode =
                chatItemNode.findFirstAccessibilityNodeInfoByClassName("android.widget.TextView")
            val message = textNode?.text?.toString().orEmpty()
            if (message.isNotEmpty()) {
                if (addChatMessage(userName, message)) {
                    sendLog("找到聊天记录,用户名: $userName\n消息: $message")
                    continue
                } else {
                    break
                }
            }
            val faceNode =
                chatItemNode.findAccessibilityNodeInfosByClassName("android.widget.FrameLayout")
                    .firstOrNull {
                        it.contentDescription.isNullOrEmpty().not()
                    }
            val faceDesc = faceNode?.contentDescription?.toString().orEmpty()
            if (faceDesc.isNotEmpty()) {
                if (addChatMessage(userName, faceDesc)) {
                    sendLog("找到表情记录,用户名: $userName\n表情描述: $faceDesc")
                    continue
                } else {
                    break
                }
            }
        }
        sendLog("结束查找聊天记录")
    }

    private fun addChatMessage(userName: String, message: String): Boolean {
        val messageList =
            mChatListMap[userName] ?: Collections.synchronizedList(mutableListOf<String>()).also {
                mChatListMap[userName] = it
            }
        if (messageList.contains(message).not()) {
            messageList.add(message)
            return true
        }
        return false
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: WechatAccessibilitySettingUpdateEvent) {
        mWechatAccessibilitySettingData =
            AppConfigsUtils.getString(AppConst.WECHAT_ACCESSIBILITY_SETTING, null)
                ?.parseJsonOrNull()
                ?: WechatAccessibilitySettingData()
        val whiteList = mWechatAccessibilitySettingData.whiteList.orEmpty()
        for (userName in mChatListMap.keys()) {
            if (userName !in whiteList) {
                mChatListMap.remove(userName)
            }
        }
        startWechatChatRobot()
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
                        sendLog("已添加该用户到自动聊天列表, 用户名: $userName")
                        EventBusManager.post(WechatAccessibilitySettingUpdateEvent())
                    } else {
                        sendLog("该用户已经在自动聊天列表, 用户名: $userName")
                    }
                }
            }
        }
    }

    private fun findSwitchButton(): AccessibilityNodeInfo? {
        return rootInActiveWindow?.findAccessibilityNodeInfosByClassName("android.widget.ImageButton")
            ?.firstOrNull {
                val switchButtonDesc = it.contentDescription?.toString()
                switchButtonDesc !in arrayOf("切换到键盘", "切换到按住说话")
            }
    }

    private fun getChatUserName(): String? {
        val screenWidth = rootInActiveWindow?.getBoundsInScreen()?.width().orZero()
        val imageNode =
            rootInActiveWindow?.findAccessibilityNodeInfosByClassName("android.widget.ImageView")
                ?.firstOrNull {
                    it.contentDescription?.toString()?.endsWith("头像")
                        .orFalse() && it.getBoundsInScreen().right < screenWidth / 2
                }
        return imageNode?.contentDescription?.toString()?.removeSuffix("头像")
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
                        sendLog("已从自动聊天列表移除该用户, 用户名: $userName")
                        EventBusManager.post(WechatAccessibilitySettingUpdateEvent())
                    } else {
                        sendLog("该用户不在自动聊天列表中, 用户名: $userName")
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

