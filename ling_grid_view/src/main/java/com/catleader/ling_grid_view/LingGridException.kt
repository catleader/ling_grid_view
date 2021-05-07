package com.catleader.ling_grid_view


sealed class LingGridException : Exception() {
    object HorizontalScaleValueExceed : LingGridException()
    object HorizontalScaleTooSmall : LingGridException()
    object HorizontalGridSizeMustGreaterOrEqualItsScale : LingGridException()
    object HorizontalScaleMustBeEvenWhenZoomedOut : LingGridException()

    object VerticalScaleValueExceed : LingGridException()
    object VerticalScaleTooSmall : LingGridException()
    object VerticalGridSizeMustGreaterOrEqualItsScale : LingGridException()
    object VerticalScaleMustBeEvenValueZoomedOut : LingGridException()


}
