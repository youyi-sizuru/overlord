package com.lifefighter.overlord.action.accessibility.wechat

/**
 * @author xzp
 * @created on 2022/2/10.
 */
class WechatReceiveMessageEvent(val message: String)

class WechatSendMessageEvent(val receive: String, val send: String)