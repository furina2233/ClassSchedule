package com.lff.classschedule.util

import android.content.Context
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment

object ScreenUtil {
    fun setDialogWidth(dialog: DialogFragment, context: Context, ratio: Float) {
        val window = dialog.dialog?.window
        window?.let {
            val params = window.attributes
            params.width = (context.resources.displayMetrics.widthPixels * ratio).toInt()
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            window.attributes = params
        }
    }

    fun setDialogMaxHeight(dialog: DialogFragment, context: Context, ratio: Float) {
        val window = dialog.dialog?.window
        val root = dialog.view

        window?.let {
            val displayMetrics = context.resources.displayMetrics
            val maxHeight = (displayMetrics.heightPixels * ratio).toInt()

            root?.post {
                if (root.height > maxHeight) {
                    val params = root.layoutParams
                    params.height = maxHeight
                    root.layoutParams = params
                }
            }
        }
    }

    fun setDialogMinHeight(dialog: DialogFragment, context: Context, ratio: Float) {
        val window = dialog.dialog?.window
        val root = dialog.view

        window?.let {
            val displayMetrics = context.resources.displayMetrics
            val minHeight = (displayMetrics.heightPixels * ratio).toInt()

            root?.post {
                if (root.height < minHeight) {
                    val params = root.layoutParams
                    params.height = minHeight
                    root.layoutParams = params
                }
            }
        }
    }
}