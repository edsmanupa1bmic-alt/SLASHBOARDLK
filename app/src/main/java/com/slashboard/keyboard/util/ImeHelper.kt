package com.slashboard.keyboard.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager

object ImeHelper {

    fun isImeEnabled(context: Context): Boolean {
        return try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return false
            val enabledMethods = imm.enabledInputMethodList ?: return false
            val myPackage = context.packageName
            enabledMethods.any { it?.packageName == myPackage }
        } catch (_: Throwable) {
            false
        }
    }

    fun isImeSelected(context: Context): Boolean {
        return try {
            val defaultIme = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            ) ?: return false
            defaultIme.contains(context.packageName)
        } catch (_: Throwable) {
            false
        }
    }

    fun openInputMethodSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Throwable) {}
        }
    }

    fun showInputMethodPicker(context: Context) {
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
        } catch (_: Throwable) {
            openInputMethodSettings(context)
        }
    }
}
