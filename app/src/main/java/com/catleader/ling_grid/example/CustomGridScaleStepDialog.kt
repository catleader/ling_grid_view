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
import com.catleader.ling_grid_view.LingGridView

class CustomGridScaleStepDialog private constructor(
    private val context: Context,
    onValueSet: ((gridScale: Int) -> Unit)? = null
) {

    companion object {
        fun getInstance(
            context: Context,
            onValueSet: ((gridScale: Int) -> Unit)? = null,
        ): CustomGridScaleStepDialog = CustomGridScaleStepDialog(context, onValueSet)
    }

    private val dialog: Dialog by lazy {
        dialogView =
            LayoutInflater.from(context).inflate(R.layout.custom_grid_scale_step_dialog, null)
        val dialog = Dialog(context)
        dialog.setCancelable(true)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialogView?.apply { dialog.setContentView(this) }
        dialog.window?.attributes?.windowAnimations = R.style.DialogPopupAnimation
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog
    }

    init {

        val edtGridScale = dialog.findViewById<EditText>(R.id.edtGridScaleStep)


        dialog.findViewById<Button>(R.id.btnOK)?.setOnClickListener {
            val gridScaleStep = edtGridScale.text.toString().toIntOrNull()

            var error = false

            if (gridScaleStep == null) {
                edtGridScale.error = "need value"
                error = true
            }

            if (gridScaleStep !in 1..LingGridView.maxGridSizePossibleInMeter) {
                edtGridScale.error = "value must be 1..${LingGridView.maxGridSizePossibleInMeter}"
                error = true
            }

            if (error) return@setOnClickListener


            onValueSet?.invoke(gridScaleStep!!)
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