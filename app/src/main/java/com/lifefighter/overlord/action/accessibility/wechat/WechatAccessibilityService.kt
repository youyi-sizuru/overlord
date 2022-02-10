package com.lifefighter.overlord.action.accessibility.wechat

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.lifefighter.overlord.net.AiInterface
import com.lifefighter.utils.*
import com.lifefighter.widget.accessibility.ExAccessibilityService
import kotlinx.coroutines.*
import org.koin.android.ext.android.getKoin
import java.util.regex.Pattern

/**
 * @author xzp
 * @created on 2022/2/10.
 */
@TargetApi(Build.VERSION_CODES.N)
class WechatAccessibilityService : ExAccessibilityService() {
    private var mSendMessage: WechatSendMessageEvent? = null
    private var mInProgressMessage: Boolean = false
    private val mWhiteUserList = listOf("老姐")
    private val mChatNamePattern = Pattern.compile("^当前所在页面,与(.*)的聊天$")
    override fun onStart() {
        mInProgressMessage = false
    }

    override fun onStop() {
    }

    override fun onReceiveEvent(event: AccessibilityEvent?) {
        tryOrNothing {
            val windowsTitle = rootInActiveWindow?.contentDescription?.toString().orEmpty()
            val titleMatcher = mChatNamePattern.matcher(windowsTitle)
            if (titleMatcher.matches()) {
                val userName = titleMatcher.group(1).orEmpty()
                if (mWhiteUserList.contains(userName).not()) {
                    return
                }
            }
            if (mInProgressMessage) {
                return
            }
            val switchToKeyboardNode =
                rootInActiveWindow?.findFirstAccessibilityNodeInfoByDescription("切换到键盘")
            if (switchToKeyboardNode != null) {
                clickNode(switchToKeyboardNode)
                GlobalScope.launch(Dispatchers.Main) {
                    delay(1000)
                    keepSafe()
                    back()
                }
                return
            }

            val editTextNode =
                rootInActiveWindow?.findFirstAccessibilityNodeInfoByClassName("android.widget.EditText")
                    ?: return
            val bottomBarNode = editTextNode.parent?.parent ?: return
            if (bottomBarNode.findFirstAccessibilityNodeInfoByDescription("切换到按住说话") == null) {
                return
            }

            val sendButtonNode = bottomBarNode.findFirstAccessibilityNodeInfoByText("发送")
            if (sendButtonNode != null) {
                clickNode(sendButtonNode)
                return
            }
            val scrollViewNode =
                rootInActiveWindow.findFirstAccessibilityNodeInfoByClassName("android.widget.ListView")
                    ?: return
            val chatItemCount = scrollViewNode.childCount
            var chatTextNode: AccessibilityNodeInfo? = null
            val screenWidth = rootInActiveWindow?.getBoundsInScreen()?.width().orZero()
            for (i in chatItemCount - 1 downTo 0) {
                val chatItemNode = scrollViewNode.getChild(i) ?: continue
                val imageNode =
                    chatItemNode.findAccessibilityNodeInfosByClassName("android.widget.ImageView")
                        .firstOrNull {
                            it.contentDescription?.toString().orEmpty().contains("头像")
                        } ?: continue
                val imageBounds = imageNode.getBoundsInScreen()
                if (imageBounds.left > screenWidth / 2) {
                    break
                }
                val textNode = imageNode.parent?.getChild(1)
                if (textNode?.className?.toString() != "android.widget.TextView") {
                    continue
                }
                chatTextNode = textNode
                break
            }
            if (chatTextNode == null) {
                return
            }
            val receiveText = chatTextNode.text?.toString().orEmpty()
            val sendEvent = mSendMessage
            if (sendEvent != null) {
                mSendMessage = null
                if (sendEvent.receive == receiveText) {
                    sendMessage(editTextNode, sendEvent.send)
                }
            } else {
                mInProgressMessage = true
                GlobalScope.launch(Dispatchers.Main + CoroutineExceptionHandler { _, _ ->
                    mInProgressMessage = false
                }) {
                    val sendMessage = bg {
                        val aiInterface = getKoin().get<AiInterface>()
                        val resultMap =
                            aiInterface.api(receiveText).parseMapJson<String>()
                        resultMap["content"]
                    }
                    if (sendMessage != null) {
                        mSendMessage = WechatSendMessageEvent(receiveText, "$sendMessage(这是AI)")
                    }
                    mInProgressMessage = false
                }
            }
        }
    }


    private fun sendMessage(editTextNode: AccessibilityNodeInfo, message: String) {
        keepSafe()
        editTextNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                message
            )
        })
    }

}

