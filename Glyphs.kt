package com.ahmad.keyboard.data

object Glyphs {
    // name-format templates; {n} is replaced with the typed name
    val templates = listOf(
        "る! 🌷 . {n} 𓏲 𖧶",
        "ִֶָ ⩇⩇ ꜥ 💭 ◖ {n} 𖥔 ࣪˖ 💒",
        "•- ❍Ꮿ͢ {n} ᭄𓆪",
        "Ξ⤹³ {n}",
        "୨ৎ {n} ୨ৎ",
        "ʚ {n} ɞ",
        "♡̶ {n} ♡̶",
        "ᥫ᭡ {n} ᥫ᭡",
        "౨ৎ {n} ౨ৎ"
    )
    val hieroglyphs = "𓀀 𓀁 𓂀 𓆩 𓆪 𓈀"
    val tarot = "🂠 🂡 🂢"
    val blanks = listOf(
        "Zero-width space (U+200B)" to "\u200B",
        "Hangul filler (U+FFA0)" to "\uFFA0",
        "Hangul filler (U+3164)" to "\u3164",
        "Invisible separator (U+2063)" to "\u2063"
    )
}
