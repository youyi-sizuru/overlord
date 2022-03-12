package com.lifefighter.overlord.action.accessibility.wool

import android.graphics.Point

/**
 * @author xzp
 * @created on 2022/3/11.
 */
class AccessibilityPackageNameEvent(val packageName: String?)

class AccessibilityTouchEvent(val point: Point)

class AccessibilityScrollEvent(val from: Point, val to: Point)

class AccessibilityGlobalEvent(val action: Int)