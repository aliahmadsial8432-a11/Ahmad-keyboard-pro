package com.ahmad.keyboard.theme

import android.graphics.Color

class KbTheme(val name: String, bg: String, key: String, text: String, accent: String, val corner: Int) {
    val bg = Color.parseColor(bg)
    val key = Color.parseColor(key)
    val text = Color.parseColor(text)
    val accent = Color.parseColor(accent)
}

object Themes {
    val all = listOf(
        KbTheme("Night Studio Obsidian", "#0F1117", "#1C1F2A", "#FFFFFF", "#D4FF00", 10),
        KbTheme("Lavender Cloud", "#E8DFFF", "#F7F3FF", "#5B4A8A", "#B9A3F5", 20),
        KbTheme("Pastel Plaid & Bunny", "#CFE8FF", "#FFFFFF", "#6B7FA3", "#FFB6C9", 22),
        KbTheme("Sweet Chocolate", "#4A2C1D", "#7B4A2E", "#FFE9D0", "#F2A65A", 14),
        KbTheme("Green Apple & Mint", "#DFF5D0", "#9BE564", "#24530A", "#FF5C5C", 16)
    )
    fun byName(n: String?): KbTheme = all.firstOrNull { it.name == n } ?: all[0]
}
