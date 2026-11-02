package com.sanya.monetmiui

import android.app.AndroidAppHelper
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class MonetIcon(ctx: Context, bmp: Bitmap) : BitmapDrawable(ctx.resources, bmp)

class MainHook : IXposedHookLoadPackage {

    companion object {
        const val LAUNCHER = "com.miui.home"
        // Імена id ImageView, які фарбуємо. Реальні дивись у логах рядків "seen".
        val TARGET_IDS = setOf("icon_icon")
        const val MAX_SEEN_LOGS = 60
    }

    private val idNames = ConcurrentHashMap<Int, String>()
    private val seen = ConcurrentHashMap<String, Boolean>()
    private val recolored = AtomicInteger()
    private val errors = AtomicInteger()
    @Volatile private var introDone = false

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != LAUNCHER) return
        HookLog.d("модуль завантажено у $LAUNCHER (Xposed API ${XposedBridge.getXposedVersion()})")

        try {
            XposedHelpers.findAndHookMethod(
                ImageView::class.java, "setImageDrawable", Drawable::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(p: MethodHookParam) {
                        try { handle(p) } catch (t: Throwable) {
                            if (errors.incrementAndGet() <= 5) HookLog.d("ПОМИЛКА хука: $t")
                        }
                    }
                })
            HookLog.d("хук ImageView.setImageDrawable встановлено")
        } catch (t: Throwable) {
            HookLog.d("НЕ вдалось встановити хук: $t")
        }
    }

    private fun handle(p: XC_MethodHook.MethodHookParam) {
        val iv = p.thisObject as ImageView
        val src = p.args[0] as? Drawable ?: return
        if (src is MonetIcon || iv.id == View.NO_ID) return

        if (!introDone) {
            introDone = true
            try {
                val app = AndroidAppHelper.currentApplication()
                val v = app.packageManager.getPackageInfo(app.packageName, 0)
                HookLog.d("лаунчер версія ${v.versionName} (${v.longVersionCode})")
            } catch (t: Throwable) { HookLog.d("версію лаунчера не отримано: $t") }
        }

        val name = idNames.getOrPut(iv.id) {
            try { iv.resources.getResourceEntryName(iv.id) } catch (e: Exception) { "?" }
        }

        // Діагностика: які id/типи drawable взагалі проходять через хук
        val key = "$name | ${src.javaClass.simpleName} | ${iv.javaClass.simpleName}"
        if (seen.size < MAX_SEEN_LOGS && seen.putIfAbsent(key, true) == null) {
            HookLog.d("seen: $key${if (name in TARGET_IDS) "  <- ціль" else ""}")
        }

        if (name !in TARGET_IDS) return

        p.args[0] = recolor(iv.context, src)
        val n = recolored.incrementAndGet()
        if (n <= 10 || n % 100 == 0) HookLog.d("перефарбовано іконок: $n (останній id=$name)")
    }

    private fun recolor(ctx: Context, src: Drawable): Drawable {
        val dark = (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        // Світлий режим: темний гліф на світлому фоні; темний — навпаки
        val c1 = ctx.getColor(if (dark) android.R.color.system_accent1_800 else android.R.color.system_accent1_100)
        val c2 = ctx.getColor(if (dark) android.R.color.system_accent1_100 else android.R.color.system_accent1_700)

        val w = if (src.intrinsicWidth > 0) src.intrinsicWidth else 192
        val h = if (src.intrinsicHeight > 0) src.intrinsicHeight else 192
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // яскравість -> lerp(c1, c2)
        val lr = floatArrayOf(0.2126f, 0.7152f, 0.0722f)
        fun row(ch: (Int) -> Int): FloatArray {
            val k = (ch(c2) - ch(c1)) / 255f
            return floatArrayOf(k * lr[0], k * lr[1], k * lr[2], 0f, ch(c1).toFloat())
        }
        val m = ColorMatrix(floatArrayOf(
            *row { Color.red(it) },
            *row { Color.green(it) },
            *row { Color.blue(it) },
            0f, 0f, 0f, 1f, 0f
        ))

        val copy = src.constantState?.newDrawable()?.mutate() ?: src
        copy.setBounds(0, 0, w, h)
        copy.colorFilter = ColorMatrixColorFilter(m)
        copy.draw(canvas)
        return MonetIcon(ctx, bmp)
    }
}
