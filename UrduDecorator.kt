package com.ahmad.keyboard.engine

object UrduDecorator {
    private const val KASHIDA = '\u0640'
    // letters that never join to the following letter
    private val nonJoiners = setOf('ا', 'آ', 'د', 'ڈ', 'ذ', 'ر', 'ڑ', 'ز', 'ژ', 'و', 'ے', 'ء', 'ں', 'ۂ')

    private fun isArabicLetter(c: Char) = c in '\u0620'..'\u06D3'

    fun kashida(s: String): String = buildString {
        for (i in s.indices) {
            append(s[i])
            if (i + 1 < s.length && isArabicLetter(s[i]) && isArabicLetter(s[i + 1]) && s[i] !in nonJoiners) append(KASHIDA)
        }
    }

    val borders = listOf("❀ ✿ ❁" to "❁ ✿ ❀", "﴾" to "﴿", "✦ ❖" to "❖ ✦", "♡ ꕥ" to "ꕥ ♡")

    fun decorate(s: String, border: Int = 0): String {
        val (l, r) = borders[border % borders.size]
        return "$l ${kashida(s)} $r"
    }
}
