package com.sanya.monetmiui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var tv: TextView
    private lateinit var scroll: ScrollView
    private val handler = Handler(Looper.getMainLooper())
    private var last = ""

    private val tick = object : Runnable {
        override fun run() { refresh(); handler.postDelayed(this, 1000) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val d = resources.displayMetrics.density
        val pad = (12 * d).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, (40 * d).toInt(), pad, pad)
        }

        val title = TextView(this).apply {
            text = "Monet Icons — логи хука"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        }
        val hint = TextView(this).apply {
            text = "Увімкни модуль у LSPosed (скоуп: com.miui.home) і перезапусти лаунчер."
            textSize = 12f
        }

        val row = LinearLayout(this).apply { gravity = Gravity.CENTER }
        fun btn(t: String, onClick: () -> Unit) = Button(this).apply {
            text = t; isAllCaps = false; setOnClickListener { onClick() }
            row.addView(this, LinearLayout.LayoutParams(0, -2, 1f))
        }
        btn("Копіювати") {
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("log", LogStore.read(this)))
            Toast.makeText(this, "Скопійовано", Toast.LENGTH_SHORT).show()
        }
        btn("Поділитись") {
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, LogStore.read(this)), null))
        }
        btn("Очистити") { LogStore.clear(this); refresh() }

        tv = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 11f
            setTextIsSelectable(true)
        }
        scroll = ScrollView(this).apply { addView(tv) }

        root.addView(title)
        root.addView(hint)
        root.addView(row)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun refresh() {
        val t = LogStore.read(this).ifEmpty { "(поки порожньо — логи з'являться, коли лаунчер запуститься з модулем)" }
        if (t == last) return
        last = t
        tv.text = t
        scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    override fun onResume() { super.onResume(); handler.post(tick) }
    override fun onPause() { super.onPause(); handler.removeCallbacks(tick) }
}
