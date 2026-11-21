package com.ahmad.keyboard.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import com.ahmad.keyboard.data.Glyphs
import com.ahmad.keyboard.engine.FontTransformer
import com.ahmad.keyboard.engine.UrduDecorator
import com.ahmad.keyboard.theme.chip
import com.ahmad.keyboard.theme.dp

class StudioActivity : Activity() {
    private val card = 0xFF1C1F2A.toInt()

    private fun copy(t: String) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("studio", t))
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0F1117.toInt())
            setPadding(dp(12), dp(24), dp(12), dp(12))
        }
        val input = EditText(this).apply {
            hint = "Type a name or bio…"
            setTextColor(Color.WHITE); setHintTextColor(0xFF888888.toInt()); textSize = 20f
            setText("Ahmad")
        }
        page.addView(input)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        fun add(label: String, value: String) {
            list.addView(chip(label, card, Color.WHITE, 10, 17f) { copy(value) }.apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { setMargins(0, dp(4), 0, dp(4)) }
            })
        }

        fun refresh() {
            list.removeAllViews()
            val n = input.text.toString()
            add("— Blank / ghost characters —", "")
            Glyphs.blanks.forEach { (name, ch) -> add(name, ch) }
            add("— Fonts —", "")
            FontTransformer.styles.forEach { s -> s.apply(n).let { add(it, it) } }
            add("— Name templates —", "")
            Glyphs.templates.forEach { t -> t.replace("{n}", n).let { add(it, it) } }
            add("— Urdu decorator —", "")
            UrduDecorator.borders.indices.forEach { i -> UrduDecorator.decorate(n, i).let { add(it, it) } }
            add("— Ancient glyphs —", "")
            add(Glyphs.hieroglyphs, Glyphs.hieroglyphs)
            add(Glyphs.tarot, Glyphs.tarot)
        }

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { refresh() }
        })
        refresh()
        page.addView(ScrollView(this).apply { addView(list) })
        setContentView(page)
    }
}
