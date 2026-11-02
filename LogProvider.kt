package com.sanya.monetmiui

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Сховище логів у внутрішній пам'яті модуля. */
object LogStore {
    private const val MAX_BYTES = 200_000
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    private fun file(ctx: Context) = File(ctx.filesDir, "hook.log")

    @Synchronized
    fun append(ctx: Context, line: String) {
        val f = file(ctx)
        if (f.exists() && f.length() > MAX_BYTES) {
            val t = f.readText()
            f.writeText(t.substring(t.length / 2))
        }
        f.appendText("${fmt.format(Date())}  $line\n")
    }

    @Synchronized
    fun read(ctx: Context): String = file(ctx).takeIf { it.exists() }?.readText() ?: ""

    @Synchronized
    fun clear(ctx: Context) { file(ctx).delete() }
}

class LogProvider : ContentProvider() {
    override fun onCreate() = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method == "log" && arg != null) context?.let { LogStore.append(it, arg) }
        return null
    }

    override fun query(u: Uri, p: Array<String>?, s: String?, a: Array<String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<String>?) = 0
}
