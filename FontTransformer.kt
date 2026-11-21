package com.ahmad.keyboard.engine

object FontTransformer {
    class Style(val name: String, private val f: (String) -> String) {
        fun apply(s: String): String = f(s)
    }

    private fun math(up: Int, low: Int, ex: Map<Char, String> = emptyMap(), digit: Int? = null): (String) -> String =
        { s ->
            buildString {
                s.forEach { c ->
                    when {
                        ex.containsKey(c) -> append(ex[c])
                        c in 'A'..'Z' -> appendCodePoint(up + (c - 'A'))
                        c in 'a'..'z' -> appendCodePoint(low + (c - 'a'))
                        c in '0'..'9' && digit != null -> appendCodePoint(digit + (c - '0'))
                        else -> append(c)
                    }
                }
            }
        }

    private fun mark(m: String): (String) -> String = { s ->
        buildString { s.forEach { append(it); if (it.isLetterOrDigit()) append(m) } }
    }

    private val scriptEx = mapOf('B' to "ℬ", 'E' to "ℰ", 'F' to "ℱ", 'H' to "ℋ", 'I' to "ℐ", 'L' to "ℒ",
        'M' to "ℳ", 'R' to "ℛ", 'e' to "ℯ", 'g' to "ℊ", 'o' to "ℴ")
    private val frakEx = mapOf('C' to "ℭ", 'H' to "ℌ", 'I' to "ℑ", 'R' to "ℜ", 'Z' to "ℨ")
    private val dsEx = mapOf('C' to "ℂ", 'H' to "ℍ", 'N' to "ℕ", 'P' to "ℙ", 'Q' to "ℚ", 'R' to "ℝ", 'Z' to "ℤ")
    private const val SC = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ"
    private val glitchMarks = listOf('\u0300', '\u0315', '\u0334', '\u0338', '\u0489', '\u033F', '\u0346', '\u034A')

    val styles: List<Style> = listOf(
        Style("Bold", math(0x1D400, 0x1D41A, digit = 0x1D7CE)),
        Style("Italic", math(0x1D434, 0x1D44E, mapOf('h' to "ℎ"))),
        Style("Bold Italic", math(0x1D468, 0x1D482)),
        Style("Cursive", math(0x1D49C, 0x1D4B6, scriptEx)),
        Style("Bold Cursive", math(0x1D4D0, 0x1D4EA)),
        Style("Gothic", math(0x1D504, 0x1D51E, frakEx)),
        Style("Bold Gothic", math(0x1D56C, 0x1D586)),
        Style("Double-Struck", math(0x1D538, 0x1D552, dsEx, 0x1D7D8)),
        Style("Sans", math(0x1D5A0, 0x1D5BA, digit = 0x1D7E2)),
        Style("Sans Bold", math(0x1D5D4, 0x1D5EE, digit = 0x1D7EC)),
        Style("Sans Italic", math(0x1D608, 0x1D622)),
        Style("Monospace", math(0x1D670, 0x1D68A, digit = 0x1D7F6)),
        Style("Fullwidth", math(0xFF21, 0xFF41, digit = 0xFF10)),
        Style("Bubble", math(0x24B6, 0x24D0)),
        Style("Squared", math(0x1F130, 0x1F130)),
        Style("Neg. Squared", math(0x1F170, 0x1F170)),
        Style("Small Caps", { s ->
            s.map { c -> val l = c.lowercaseChar(); if (l in 'a'..'z') SC[l - 'a'] else c }.joinToString("")
        }),
        Style("Strike", mark("\u0336")),
        Style("Slashed", mark("\u0338")),
        Style("Underline", mark("\u0332")),
        Style("Glitch", { s ->
            var i = 0
            buildString {
                s.forEach { c ->
                    append(c)
                    if (c.isLetterOrDigit()) {
                        append(glitchMarks[i % glitchMarks.size]); i++
                        append(glitchMarks[(i + 3) % glitchMarks.size]); i++
                    }
                }
            }
        }),
        Style("Spaced", { s -> s.toList().joinToString(" ") })
    )
}
