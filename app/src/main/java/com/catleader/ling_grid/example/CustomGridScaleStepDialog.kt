package com.catleader.ling_grid.example

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import com.catleader.ling_grid_view.LingGridException


class CustomGridScaleStepDialog private constructor(
    private val context: Context,
    onValueSet: ((gridScaleHorizontal: Int, gridScaleVertical: Int) -> Unit)? = null
) {

    companion object {
        fun getInstance(
            context: Context,
            onValueSet: ((gridScaleHorizontal: Int, gridScaleVertical: Int) -> Unit)? = null,
        ): CustomGridScaleStepDialog =
            CustomGridScaleStepDialog(context, onValueSet)
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

    var gridSizeInMeters: Pair<Int, Int>? = null

    private var edtGridScaleHorizontalStep: EditText =
        dialog.findViewById(R.id.edtGridScaleHorizontalStep)

    private var edtGridScaleVerticalStep: EditText =
        dialog.findViewById(R.id.edtGridScaleVerticalStep)

    private var btnOK: Button = dialog.findViewById(R.id.btnOK)

    fun setCurrentGridScale(horizonScale: Int, verticalScale: Int) {
        edtGridScaleHorizontalStep.setText(horizonScale.toString())
        edtGridScaleVerticalStep.setText(verticalScale.toString())
    }

    fun clearError() {
        edtGridScaleHorizontalStep.error = null
        edtGridScaleVerticalStep.error = null
    }

    init {
        edtGridScaleHorizontalStep.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            }
        }

        edtGridScaleVerticalStep.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val text = edtGridScaleVerticalStep.text
                if(text != null) edtGridScaleVerticalStep.setSelection(text.length)
            }
        }

        btnOK.setOnClickListener {
            val gridWidth = gridSizeInMeters?.first

            val gridHeight = gridSizeInMeters?.second

            if (gridWidth == null) {
                return@setOnClickListener
            }

            if (gridHeight == null) {
                return@setOnClickListener
            }

            val gridScaleHorizontal = edtGridScaleHorizontalStep.text.toString().toIntOrNull()

            val gridScaleVertical = edtGridScaleVerticalStep.text.toString().toIntOrNull()

            var error = false


            if (gridScaleHorizontal == null) {
                edtGridScaleHorizontalStep.error = "need value"
                error = true
            }

            if (gridScaleVertical == null) {
                edtGridScaleVerticalStep.error = "need value"
                error = true
            }


            if (error) return@setOnClickListener

            if (gridScaleVertical != null && gridScaleHorizontal != null) {
                try {
                    onValueSet?.invoke(gridScaleHorizontal, gridScaleVertical)
                    hide()
                } catch (exception: Exception) {
                    when (exception) {
                        LingGridException.HorizontalScaleValueExceed -> {
                            edtGridScaleHorizontalStep.error = "Scale value exceed grid 's width."
                        }
                        LingGridException.VerticalScaleValueExceed -> {
                            edtGridScaleVerticalStep.error = "Scale value exceed grid 's height."
                        }
                        LingGridException.HorizontalScaleTooSmall -> {
                            edtGridScaleHorizontalStep.error = "Scale value too small."
                        }
                        LingGridException.HorizontalScaleTooSmall -> {
                            edtGridScaleVerticalStep.error = "Scale value too small."
                        }
                        LingGridException.HorizontalScaleMustBeEvenWhenZoomedOut -> {
                            edtGridScaleHorizontalStep.error = "Scale must be an even value when zoomed-out."
                        }
                        LingGridException.HorizontalScaleMustBeEvenWhenZoomedOut -> {
                            edtGridScaleVerticalStep.error = "Scale must be an even value when zoomed-out."
                        }
                    }
                }
            }
        }

    }

    private var dialogView: View? = null

    fun show() {
        if (!dialog.isShowing) dialog.show()
        edtGridScaleHorizontalStep.setSelection(edtGridScaleHorizontalStep.text.length)
        edtGridScaleHorizontalStep.requestFocus()
    }

    fun hide() {
        dialog.dismiss()
    }

}