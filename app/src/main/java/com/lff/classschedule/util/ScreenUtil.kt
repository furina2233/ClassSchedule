package com.lff.classschedule.util

import android.content.Context
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment

object ScreenUtil {
    fun setDialogSize(dialog: DialogFragment, context: Context,ratio: Float) {
        val window = dialog.dialog?.window
        window?.let {
            val params = window.attributes
            params.width = (context.resources.displayMetrics.widthPixels * ratio).toInt()
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            window.attributes = params
        }
    }
}