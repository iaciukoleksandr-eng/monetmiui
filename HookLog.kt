package com.sanya.monetmiui

import android.app.AndroidAppHelper
import android.net.Uri
import de.robv.android.xposed.XposedBridge
import java.util.concurrent.Executors

/** Логи з процесу лаунчера -> LSPosed log + застосунок модуля (через ContentProvider). */
object HookLog {
    private val uri = Uri.parse("content://com.sanya.monetmiui.logs")
    private val pending = ArrayDeque<String>()
    private val exec = Executors.newSingleThreadExecutor()

    fun d(msg: String) {
        XposedBridge.log("MonetMiui: $msg")
        synchronized(pending) {
            pending.addLast(msg)
            while (pending.size > 300) pending.removeFirst()
        }
        exec.execute { flush() }
    }

    private fun flush() {
        val app = AndroidAppHelper.currentApplication() ?: return // ще не готовий — відправимо з наступним логом
        while (true) {
            val m = synchronized(pending) { pending.firstOrNull() } ?: return
            try {
                app.contentResolver.call(uri, "log", m, null)
            } catch (t: Throwable) {
                return // модуль недоступний — лишаємо в черзі
            }
            synchronized(pending) { pending.removeFirstOrNull() }
        }
    }
}
