package com.ahmad.keyboard.ime

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.ahmad.keyboard.data.Emojis
import com.ahmad.keyboard.data.Kaomojis
import com.ahmad.keyboard.data.SocialLinks
import com.ahmad.keyboard.engine.FontTransformer
import com.ahmad.keyboard.theme.KbTheme
import com.ahmad.keyboard.theme.Themes
import com.ahmad.keyboard.theme.blend
import com.ahmad.keyboard.theme.chip
import com.ahmad.keyboard.theme.dp
import com.ahmad.keyboard.theme.keyStates

class KeyboardService : InputMethodService() {
    companion object { var instance: KeyboardService? = null }

    private lateinit var prefs: SharedPreferences
    private lateinit var root: LinearLayout
    private lateinit var panelBox: FrameLayout
    private lateinit var keysBox: LinearLayout

    private var theme: KbTheme = Themes.all[0]
    private var shift = false
    private var capsLock = false
    private var lastShiftTap = 0L
    private var lastSpace = 0L
    private var mode = 0              // 0 English, 1 symbols, 2 Urdu, 3 symbols page 2
    private var fontIndex = -1        // -1 = normal typing
    private var openPanel: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private val letterKeys = mutableListOf<Pair<TextView, String>>()
    private var shiftKey: TextView? = null
    private val dark = 0xFF111111.toInt()

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val t = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()?.trim()
        if (!t.isNullOrEmpty()) {
            val list = recent().toMutableList()
            list.remove(t); list.add(0, t)
            prefs.edit().putString("recent", list.take(15).joinToString("\u0001")).apply()
        }
    }

    private fun recent(): List<String> =
        prefs.getString("recent", "")!!.split("\u0001").filter { it.isNotEmpty() }

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = getSharedPreferences("akp", Context.MODE_PRIVATE)
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).addPrimaryClipChangedListener(clipListener)
    }

    override fun onDestroy() {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).removePrimaryClipChangedListener(clipListener)
        instance = null
        super.onDestroy()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        theme = Themes.byName(prefs.getString("theme", null))
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        build()
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (!::root.isInitialized) return
        val cls = (info?.inputType ?: 0) and InputType.TYPE_MASK_CLASS
        mode = when {
            cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE -> 1
            mode == 2 -> 2
            else -> 0
        }
        shift = false; capsLock = false
        val t = Themes.byName(prefs.getString("theme", null))
        if (t !== theme) { theme = t; build() } else {
            openPanel = null; panelBox.visibility = View.GONE; rebuildKeys()
        }
        autoCap()
    }

    // ---------- helpers ----------
    fun commit(t: String) { currentInputConnection?.commitText(t, 1) }

    private fun open(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun isPasswordField(): Boolean {
        val it = currentInputEditorInfo?.inputType ?: return false
        val cls = it and InputType.TYPE_MASK_CLASS
        val v = it and InputType.TYPE_MASK_VARIATION
        return (cls == InputType.TYPE_CLASS_TEXT && (v == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            v == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD || v == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) ||
            (cls == InputType.TYPE_CLASS_NUMBER && v == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
    }

    private fun feedback(v: View) {
        if (prefs.getBoolean("haptic", true)) v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (prefs.getBoolean("sound", false))
            (getSystemService(Context.AUDIO_SERVICE) as AudioManager).playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
    }

    private fun autoCap() {
        if (mode != 0 || capsLock) return
        val ei = currentInputEditorInfo ?: return
        if (ei.inputType == 0) return
        val want = (currentInputConnection?.getCursorCapsMode(ei.inputType) ?: 0) != 0
        if (want != shift) { shift = want; refreshShift() }
    }

    private fun doEnter() {
        val opts = currentInputEditorInfo?.imeOptions ?: 0
        val action = opts and EditorInfo.IME_MASK_ACTION
        val noEnter = (opts and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (!noEnter && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED)
            currentInputConnection?.performEditorAction(action)
        else sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        autoCap()
    }

    private fun enterLabel(): String {
        val opts = currentInputEditorInfo?.imeOptions ?: 0
        if ((opts and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) return "⏎"
        return when (opts and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_SEARCH -> "🔍"
            EditorInfo.IME_ACTION_SEND -> "➤"
            EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_NEXT -> "→"
            EditorInfo.IME_ACTION_DONE -> "✓"
            else -> "⏎"
        }
    }

    private fun space() {
        val ic = currentInputConnection ?: return
        val now = SystemClock.uptimeMillis()
        if (mode == 0 && now - lastSpace < 400) {
            val before = ic.getTextBeforeCursor(2, 0)?.toString() ?: ""
            if (before.length == 2 && before[1] == ' ' && before[0].isLetterOrDigit()) {
                ic.deleteSurroundingText(1, 0); ic.commitText(". ", 1)
                lastSpace = 0; autoCap(); return
            }
        }
        ic.commitText(" ", 1); lastSpace = now; autoCap()
    }

    private fun deleteOne() { sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL); autoCap() }

    // ---------- layout ----------
    private fun build() {
        root.removeAllViews()
        root.setBackgroundColor(theme.bg)
        openPanel = null

        val tools: List<Pair<String, () -> Unit>> = listOf(
            "★" to { showPanel("hub") },
            "aあ" to { showPanel("fonts") },
            "🎨" to { showPanel("themes") },
            "😀" to { showPanel("emoji") },
            "🧸" to { showPanel("kaomoji") },
            "📋" to { showPanel("clip") },
            "🎙" to {
                startActivity(Intent(this, VoiceActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            },
            "اردو" to { mode = if (mode == 2) 0 else 2; shift = false; rebuildKeys() }
        )
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tools.forEach { (label, action) ->
            bar.addView(chip(label, theme.key, theme.text, theme.corner, 15f) { action() }.apply {
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, dp(38)).apply { setMargins(dp(4), dp(4), dp(4), dp(2)) }
            })
        }
        root.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(bar) })

        panelBox = FrameLayout(this).apply { visibility = View.GONE }
        root.addView(panelBox)

        keysBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(2), 0, dp(2), dp(4)) }
        root.addView(keysBox)
        rebuildKeys()
    }

    private fun key(label: String, w: Float, h: Int, bg: Int = theme.key, fg: Int = theme.text, onTap: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            setTextColor(fg)
            textSize = if (label.length > 2) 13f else 20f
            background = keyStates(bg, blend(bg, theme.accent, 0.45f), theme.corner)
            layoutParams = LinearLayout.LayoutParams(0, dp(h), w).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }
            setOnClickListener { v -> feedback(v); onTap() }
        }

    private fun spacer(w: Float): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, w) }

    private fun newRow() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

    private fun layoutRows(): List<String> = when (mode) {
        0 -> listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
        1 -> listOf("1234567890", "@#\$%&-+()/", "*\"':;!?")
        3 -> listOf("~`|•√π÷×¶∆", "£¢€¥^°={}\\", "©®™✓[]<>")
        else -> if (shift) listOf("ٹثڈڑؤئۃھںۓ", "آأ،؟۔؛٪ًَُ", "ژذضظغـ")
                else listOf("قوعرتےءیہپ", "اسدفگحجکل", "زشچطبنم")
    }

    private fun shiftLabel() = if (capsLock) "⇪" else if (shift) "⬆" else "⇧"

    private fun refreshShift() {
        letterKeys.forEach { (tv, c) -> tv.text = if (shift) c.uppercase() else c }
        shiftKey?.let { sk ->
            sk.text = shiftLabel()
            sk.background = keyStates(if (shift) theme.accent else theme.key, blend(theme.key, theme.accent, 0.45f), theme.corner)
            sk.setTextColor(if (shift) dark else theme.text)
        }
    }

    private fun onShift() {
        val now = SystemClock.uptimeMillis()
        if (capsLock) { capsLock = false; shift = false }
        else if (shift && now - lastShiftTap < 350) capsLock = true
        else shift = !shift
        lastShiftTap = now
        if (mode == 2) rebuildKeys() else refreshShift()
    }

    private fun backspaceKey(h: Int): TextView {
        val k = key("⌫", 1.5f, h) {}
        val repeat = object : Runnable {
            override fun run() { deleteOne(); handler.postDelayed(this, 45) }
        }
        k.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { v.isPressed = true; feedback(v); deleteOne(); handler.postDelayed(repeat, 400) }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { v.isPressed = false; handler.removeCallbacks(repeat) }
            }
            true
        }
        return k
    }

    private fun rebuildKeys() {
        keysBox.removeAllViews(); letterKeys.clear(); shiftKey = null
        val land = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val h = if (land) 40 else 48

        if (prefs.getBoolean("numrow", true) && (mode == 0 || mode == 2)) {
            val nr = newRow()
            "1234567890".forEach { c -> nr.addView(key(c.toString(), 1f, h - 6) { typeKey(c.toString()) }) }
            keysBox.addView(nr)
        }

        layoutRows().forEachIndexed { idx, chars ->
            val row = newRow()
            if (idx == 1 && mode == 0) row.addView(spacer(0.5f))
            if (idx == 2) {
                when (mode) {
                    0, 2 -> { val sk = key(shiftLabel(), 1.5f, h) { onShift() }; shiftKey = sk; row.addView(sk) }
                    1 -> row.addView(key("=\\<", 1.5f, h) { mode = 3; rebuildKeys() })
                    else -> row.addView(key("1/2", 1.5f, h) { mode = 1; rebuildKeys() })
                }
            }
            chars.forEach { c ->
                val s = c.toString()
                val k = key(if (mode == 0 && shift) s.uppercase() else s, 1f, h) { typeKey(s) }
                if (mode == 0) letterKeys.add(k to s)
                row.addView(k)
            }
            if (idx == 1 && mode == 0) row.addView(spacer(0.5f))
            if (idx == 2) row.addView(backspaceKey(h))
            keysBox.addView(row)
        }

        val bottom = newRow()
        bottom.addView(key(if (mode == 1 || mode == 3) "ABC" else "123", 1.3f, h) { mode = if (mode == 1 || mode == 3) 0 else 1; rebuildKeys() })
        bottom.addView(key(if (mode == 2) "EN" else "اردو", 1.2f, h) { mode = if (mode == 2) 0 else 2; shift = false; rebuildKeys() })
        bottom.addView(key("😀", 1.1f, h) { showPanel("emoji") })
        val spaceLabel = if (fontIndex >= 0) FontTransformer.styles[fontIndex].name else if (mode == 2) "اردو" else "Ahmad Keyboard"
        bottom.addView(key(spaceLabel, 4f, h) { space() })
        bottom.addView(key(",", 0.8f, h) { commit(",") })
        bottom.addView(key(".", 0.8f, h) { commit(".") })
        bottom.addView(key(enterLabel(), 1.3f, h, theme.accent, dark) { doEnter() })
        keysBox.addView(bottom)
        refreshShift()
    }

    private fun typeKey(c: String) {
        var t = if (mode == 0 && shift) c.uppercase() else c
        if (mode == 0 && fontIndex >= 0 && !isPasswordField()) t = FontTransformer.styles[fontIndex].apply(t)
        commit(t)
        if (mode == 2 && shift) { shift = false; rebuildKeys() }
        else if (mode == 0 && shift && !capsLock) { shift = false; refreshShift() }
        autoCap()
    }

    // ---------- panels ----------
    private fun reopen(kind: String) { openPanel = null; showPanel(kind) }

    private fun showPanel(kind: String) {
        panelBox.removeAllViews()
        if (openPanel == kind) { openPanel = null; panelBox.visibility = View.GONE; return }
        openPanel = kind
        panelBox.visibility = View.VISIBLE

        if (kind == "fonts") {
            val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val names = listOf("Normal") + FontTransformer.styles.map { it.apply("Abc") }
            names.forEachIndexed { i, label ->
                val sel = (i - 1) == fontIndex
                bar.addView(chip(label, if (sel) theme.accent else theme.key, if (sel) dark else theme.text, theme.corner, 16f) {
                    fontIndex = i - 1; rebuildKeys(); reopen("fonts")
                }.apply {
                    layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, dp(44)).apply { setMargins(dp(4), dp(4), dp(4), dp(4)) }
                })
            }
            panelBox.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(bar) })
            return
        }

        val items: List<Pair<String, () -> Unit>>
        var cols = 3
        when (kind) {
            "hub" -> {
                cols = 2
                items = SocialLinks.all.map { (n, u) -> n to { open(u) } }
            }
            "themes" -> {
                cols = 2
                items = Themes.all.map { t ->
                    t.name to { prefs.edit().putString("theme", t.name).apply(); theme = t; build() }
                }
            }
            "emoji" -> {
                cols = 6
                items = Emojis.all.map { e -> e to { commit(e) } }
            }
            "kaomoji" -> {
                items = Kaomojis.all.map { k -> k to { commit(k) } }
            }
            else -> {
                cols = 1
                val pins = prefs.getStringSet("pins", emptySet())!!.toList()
                val rec = recent()
                val list = mutableListOf<Pair<String, () -> Unit>>()
                if (rec.isNotEmpty()) {
                    list.add("📌 Pin latest copy" to {
                        prefs.edit().putStringSet("pins", pins.toSet() + rec[0]).apply(); reopen("clip")
                    })
                }
                pins.forEach { p -> list.add("📌 $p" to { commit(p) }) }
                rec.forEach { r -> list.add("📋 $r" to { commit(r) }) }
                if (list.isEmpty()) list.add("Nothing copied yet" to {})
                else list.add("🗑 Clear pins & history" to {
                    prefs.edit().remove("pins").remove("recent").apply(); reopen("clip")
                })
                items = list
            }
        }
        panelBox.addView(grid(items, cols))
    }

    private fun grid(items: List<Pair<String, () -> Unit>>, cols: Int): View {
        val sv = ScrollView(this).apply { layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, dp(150)) }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        items.chunked(cols).forEach { r ->
            val rowL = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            r.forEach { (t, a) ->
                rowL.addView(chip(t, theme.key, theme.text, theme.corner, if (cols >= 6) 20f else 15f) { a() }.apply {
                    layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }
                })
            }
            repeat(cols - r.size) { rowL.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f)) }
            col.addView(rowL)
        }
        sv.addView(col)
        return sv
    }
}
