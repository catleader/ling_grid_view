package com.catleader.ling_grid.example

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText

class CustomGridSizeDialog private constructor(
    private val context: Context,
    onValueSet: ((gd: Int, gh: Int) -> Unit)? = null
) {

    companion object {
        fun getInstance(
            context: Context,
            onValueSet: ((gw: Int, gh: Int) -> Unit)? = null,
        ): CustomGridSizeDialog = CustomGridSizeDialog(context, onValueSet)
    }

    private val dialog: Dialog by lazy {
        dialogView = LayoutInflater.from(context).inflate(R.layout.custom_grid_size_dialog, null)
        val dialog = Dialog(context)
        dialog.setCancelable(true)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialogView?.apply { dialog.setContentView(this) }
        dialog.window?.attributes?.windowAnimations = R.style.DialogPopupAnimation
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog
    }

    init {

        val edtGw = dialog.findViewById<EditText>(R.id.edtGridWidth)

        val edtGh = dialog.findViewById<EditText>(R.id.edtGridHeight)

        dialog.findViewById<Button>(R.id.btnOK)?.setOnClickListener {
            val gw = edtGw.text.toString().toIntOrNull()
            val gh = edtGh.text.toString().toIntOrNull()


            var error = false

            if (gw == null) {
                edtGw.error = "need value"
                error = true
            }

            if (gh == null) {
                edtGh.error = "need value"
                error = true
            }

            if(error) return@setOnClickListener


            onValueSet?.invoke(gw!!, gh!!)
        }

    }

    private var dialogView: View? = null

    fun show() {
        if (!dialog.isShowing) dialog.show()
    }

    fun hide() {
        dialog.dismiss()
    }

}