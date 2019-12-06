package ja.burhanrashid52.photoeditor

import android.graphics.Typeface

data class ViewCharacteristic(
    var scaleX: Float = 0f,
    var scaleY: Float = 0f,
    var alpha: Float = 0f,
    var rotation: Float = 0f,

    var typeface: Typeface? = null,
    var viewType: ViewType? = null,
    var textStyleBuilder: TextStyleBuilder? = null,
    var content: String? = null,
    var coordX: Float = 0F,
    var coordY: Float = 0F,
    val pivotX: Float = 0F,
    val pivotY: Float = 0F,
    val color: Int = 0
)