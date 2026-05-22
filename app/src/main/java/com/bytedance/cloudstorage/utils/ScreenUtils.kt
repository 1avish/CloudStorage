package com.bytedance.cloudstorage.utils

import android.content.Context
import android.content.res.Resources

/**
 * 屏幕适配工具，参考 Flutter ScreenUtil 的设计思路。
 *
 * 原理：以设计稿宽度为基准，将设计稿中的 px 值按比例换算为当前设备的 dp 值。
 * 例如设计稿标注 padding = 18px（基于 390 宽），在 412 宽的设备上实际为
 * 18 * (412 / 390) ≈ 19dp，从而保证不同屏幕上的视觉比例一致。
 *
 * 使用方式：
 *   1. 在 Application 或 Activity.onCreate 中调用 ScreenUtils.init(context)
 *   2. 在 Composable 中通过 ScreenUtils.dp(18f) 或扩展属性 18.w 取得自适应 dp 值
 */
object ScreenUtils {

    // 设备屏幕宽度（px），取真实物理像素
    private var screenWidthPx = 0f

    // 设计稿基准宽度（px），默认 390
    private var designWidthPx = 390f

    // 屏幕密度，px / dp
    private var density = 1f

    /**
     * 初始化，必须在使用前调用一次。
     *
     * @param context   任意 Context，用于获取屏幕参数
     * @param designWidth 设计稿宽度（px），默认 390
     */
    fun init(context: Context, designWidth: Float = 390f) {
        designWidthPx = designWidth
        density = context.resources.displayMetrics.density
        // 使用真实物理像素宽度（不乘 density），与设计稿 px 对齐
        screenWidthPx = context.resources.displayMetrics.widthPixels.toFloat()
    }

    /**
     * 将设计稿 px 值转换为当前设备的 dp 值。
     *
     * 计算公式：目标 dp = 设计稿 px × (设备屏幕宽度 px / 设计稿宽度 px) / density
     * 简化为：  目标 dp = 设计稿 px × scale / density
     */
    fun dp(designPx: Float): Float {
        val scale = screenWidthPx / designWidthPx
        return designPx * scale / density
    }

    fun dp(designPx: Int): Float = dp(designPx.toFloat())

    /**
     * 将设计稿 px 值转换为 sp（字体大小），逻辑与 dp 相同。
     */
    fun sp(designPx: Float): Float {
        val scale = screenWidthPx / designWidthPx
        return designPx * scale / density
    }

    fun sp(designPx: Int): Float = sp(designPx.toFloat())
}

/**
 * Int 扩展属性：设计稿 px → 自适应 dp。
 * 用法：18.w  → 返回 Float，可直接传给 Compose 的 dp 参数。
 */
val Int.w: Float get() = ScreenUtils.dp(this)

/**
 * Float 扩展属性：设计稿 px → 自适应 dp。
 */
val Float.w: Float get() = ScreenUtils.dp(this)

/**
 * Int 扩展属性：设计稿 px → 自适应 sp（字体大小）。
 */
val Int.ws: Float get() = ScreenUtils.sp(this)

/**
 * Float 扩展属性：设计稿 px → 自适应 sp（字体大小）。
 */
val Float.ws: Float get() = ScreenUtils.sp(this)
