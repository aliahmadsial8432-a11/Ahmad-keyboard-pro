package com.ahmad.keyboard.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.ahmad.keyboard.data.SocialLinks
import com.ahmad.keyboard.theme.Themes
import com.ahmad.keyboard.theme.chip
import com.ahmad.keyboard.theme.dp

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("akp", Context.MODE_PRIVATE)
        val card = 0xFF1C1F2A.toInt()
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0F1117.toInt())
            setPadding(dp(16), dp(32), dp(16), dp(24))
        }
        fun lp() = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { setMargins(0, dp(5), 0, dp(5)) }
        fun title(t: String) = page.addView(TextView(this).apply {
            text = t; textSize = 19f; setTextColor(Color.WHITE); setPadding(0, dp(16), 0, dp(4))
        })
        fun btn(label: String, bg: Int = card, fg: Int = Color.WHITE, onClick: () -> Unit) {
            page.addView(chip(label, bg, fg, 12, 16f, onClick).apply { layoutParams = lp() })
        }
        fun toggle(label: String, key: String, def: Boolean) {
            val tv = chip("", card, Color.WHITE, 12, 16f) {}
            fun render() { tv.text = (if (prefs.getBoolean(key, def)) "✔  " else "✖  ") + label }
            tv.setOnClickListener { prefs.edit().putBoolean(key, !prefs.getBoolean(key, def)).apply(); render() }
            render(); tv.layoutParams = lp(); page.addView(tv)
        }

        page.addView(TextView(this).apply {
            text = "Ahmad Keyboard Pro"; textSize = 26f; setTextColor(0xFFD4FF00.toInt())
        })
        page.addView(TextView(this).apply {
            text = "1) Enable   2) Select   3) Test below"
            setTextColor(Color.LTGRAY); setPadding(0, dp(6), 0, dp(10))
        })
        btn("1 · Enable keyboard", 0xFFD4FF00.toInt(), 0xFF111111.toInt()) {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        btn("2 · Select keyboard") {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }
        page.addView(EditText(this).apply {
            hint = "3 · Tap here to test…"; setTextColor(Color.WHITE); setHintTextColor(0xFF888888.toInt())
        })

        title("Settings")
        toggle("Vibration on key press", "haptic", true)
        toggle("Key click sound", "sound", false)
        toggle("Number row", "numrow", true)

        title("Themes")
        Themes.all.forEach { t ->
            btn(t.name) {
                prefs.edit().putString("theme", t.name).apply()
                Toast.makeText(this, "${t.name} applied", Toast.LENGTH_SHORT).show()
            }
        }

        title("Tools")
        btn("✍️ Designer Studio Pad") { startActivity(Intent(this, StudioActivity::class.java)) }

        title("★ Creator Hub")
        SocialLinks.all.forEach { (n, u) -> btn(n) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) } }
        setContentView(ScrollView(this).apply { addView(page) })
    }
}
