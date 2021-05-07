package com.catleader.ling_grid.example

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import com.catleader.ling_grid_view.LingGridException
import com.catleader.ling_grid_view.LingGridView

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

    private var tag = "CustomGridSizeDialog"

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


    private val edtGw: EditText = dialog.findViewById(R.id.edtGridWidth)

    private val edtGh: EditText = dialog.findViewById(R.id.edtGridHeight)

    fun setCurrentGridSize(gridSize: Pair<Int, Int>) {
        edtGw.setText(gridSize.first.toString())
        edtGh.setText(gridSize.second.toString())
    }

    init {
        edtGw.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            }
        }

        edtGh.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val text = edtGh.text
                if (text != null) edtGh.setSelection(text.length)
            }
        }

        dialog.findViewById<Button>(R.id.btnOK)?.setOnClickListener {
            val gw = edtGw.text.toString().toIntOrNull()
            val gh = edtGh.text.toString().toIntOrNull()

            val error: Boolean

            when {
                gw == null -> {
                    edtGw.error = "need value."
                    error = true
                }
                gw > LingGridView.maxGridSizePossibleInMeter -> {
                    edtGw.error = "must be in 1..${LingGridView.maxGridSizePossibleInMeter}"
                    error = true
                }
                gh == null -> {
                    edtGw.error = "need value."
                    error = true
                }
                gh > LingGridView.maxGridSizePossibleInMeter -> {
                    edtGw.error = "must be in 1..${LingGridView.maxGridSizePossibleInMeter}"
                    error = true
                }
                else -> {
                    error = false
                }
            }

            if (error) return@setOnClickListener


            try {
                onValueSet?.invoke(gw!!, gh!!)
                hide()
            } catch (e: Exception) {
                when (e) {
                    LingGridException.HorizontalGridSizeMustGreaterOrEqualItsScale -> {
                        Log.e(
                            tag,
                            "Error on setting horizontal grid size, $gw is less than its corresponding scale."
                        )
                    }
                    LingGridException.VerticalGridSizeMustGreaterOrEqualItsScale -> {
                        Log.e(
                            tag,
                            "Error on setting vertical grid size, $gh is less than its corresponding scale."
                        )
                    }
                    else -> {
                        Log.e(tag, "Something happened when setting grid size: ${e.message}")
                    }
                }
            }

        }

    }

    private var dialogView: View? = null

    fun clearError() {
        edtGh.error = null
        edtGw.error = null
    }

    fun show() {
        if (!dialog.isShowing) dialog.show()
        edtGw.requestFocus()
        edtGw.setSelection(edtGw.text.length)
    }

    fun hide() {
        dialog.dismiss()
    }

}