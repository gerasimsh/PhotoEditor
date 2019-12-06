package ja.burhanrashid52.photoeditor

import android.graphics.Typeface
import android.view.View

data class ViewParam(
    var view: View? = null,
    var typeface: Typeface? = null,
    var textStyleBuilder: TextStyleBuilder? = null,
    var colorView: Int? = null
)
