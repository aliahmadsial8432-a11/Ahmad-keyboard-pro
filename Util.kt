package com.ahmad.keyboard.theme

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.widget.TextView

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun blend(a: Int, b: Int, t: Float): Int = Color.rgb(
    (Color.red(a) * (1 - t) + Color.red(b) * t).toInt(),
    (Color.green(a) * (1 - t) + Color.green(b) * t).toInt(),
    (Color.blue(a) * (1 - t) + Color.blue(b) * t).toInt()
)

/** Keycap with a darker bottom edge for a subtle 3D look. */
fun Context.keyBg(color: Int, radiusDp: Int): LayerDrawable {
    val r = dp(radiusDp).toFloat()
    val shadow = GradientDrawable().apply { setColor(blend(color, Color.BLACK, 0.35f)); cornerRadius = r }
    val face = GradientDrawable().apply { setColor(color); cornerRadius = r }
    return LayerDrawable(arrayOf(shadow, face)).apply { setLayerInset(1, 0, 0, 0, dp(2)) }
}

fun Context.keyStates(color: Int, pressed: Int, radiusDp: Int): StateListDrawable =
    StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_pressed), keyBg(pressed, radiusDp))
        addState(intArrayOf(), keyBg(color, radiusDp))
    }

fun Context.chip(
    label: String, bg: Int, fg: Int, radiusDp: Int = 10, sizeSp: Float = 16f, onClick: () -> Unit
): TextView = TextView(this).apply {
    this.text = label
    gravity = Gravity.CENTER
    setTextColor(fg)
    textSize = sizeSp
    background = GradientDrawable().apply { setColor(bg); cornerRadius = dp(radiusDp).toFloat() }
    setPadding(dp(10), dp(8), dp(10), dp(8))
    setOnClickListener { onClick() }
}
