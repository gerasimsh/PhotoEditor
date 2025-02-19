package ja.burhanrashid52.photoeditor

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.widget.TextView

import java.util.HashMap

/**
 *
 *
 * This class is used to wrap the styles to apply on the TextView on [PhotoEditor.addText] and [PhotoEditor.editText]
 *
 *
 * @author [Christian Caballero](https://github.com/Sulfkain)
 * @since 14/05/2019
 */
open class TextStyleBuilder {

    private val values = HashMap<TextStyle, Any>()
    fun getValues(): Map<TextStyle, Any> {
        return values
    }

    /**
     * Set this textSize style
     *
     * @param size Size to apply on text
     */
    fun withTextSize(size: Float) {
        values[TextStyle.SIZE] = size
    }

    /**
     * Set this color style
     *
     * @param color Color to apply on text
     */
    fun withTextColor(color: Int) {
        values[TextStyle.COLOR] = color
    }

    /**
     * Set this [Typeface] style
     *
     * @param textTypeface TypeFace to apply on text
     */
    fun withTextFont(textTypeface: Typeface) {
        values[TextStyle.FONT_FAMILY] = textTypeface
    }

    /**
     * Set this gravity style
     *
     * @param gravity Gravity style to apply on text
     */
    fun withGravity(gravity: Int) {
        values[TextStyle.GRAVITY] = gravity
    }

    /**
     * Set this background color
     *
     * @param background Background color to apply on text, this method overrides the preview set on [TextStyleBuilder.withBackgroundDrawable]
     */
    fun withBackgroundColor(background: Int) {
        values[TextStyle.BACKGROUND] = background
    }

    /**
     * Set this background [Drawable], this method overrides the preview set on [TextStyleBuilder.withBackgroundColor]
     *
     * @param bgDrawable Background drawable to apply on text
     */
    fun withBackgroundDrawable(bgDrawable: Drawable) {
        values[TextStyle.BACKGROUND] = bgDrawable
    }

    /**
     * Set this textAppearance style
     *
     * @param textAppearance Text style to apply on text
     */
    fun withTextAppearance(textAppearance: Int) {
        values[TextStyle.TEXT_APPEARANCE] = textAppearance
    }

    /**
     * Method to apply all the style setup on this Builder}
     *
     * @param textView TextView to apply the style
     */
    internal fun applyStyle(textView: TextView) {
        for ((key, value) in values) {
            when (key) {
                TextStyle.SIZE -> {
                    val size = value as Float
                    applyTextSize(textView, size)
                }

                TextStyle.COLOR -> {
                    val color = value as Int
                    applyTextColor(textView, color)
                }

                TextStyle.FONT_FAMILY -> {
                    val typeface = value as Typeface
                    applyFontFamily(textView, typeface)
                }

                TextStyle.GRAVITY -> {
                    val gravity = value as Int
                    applyGravity(textView, gravity)
                }

                TextStyle.BACKGROUND -> {
                    if (value is Drawable) {
                        applyBackgroundDrawable(textView, value)

                    } else if (value is Int) {
                        applyBackgroundColor(textView, value)
                    }
                }

                TextStyle.TEXT_APPEARANCE -> {
                    if (value is Int) {
                        applyTextAppearance(textView, value)
                    }
                }
            }
        }
    }

    protected open fun applyTextSize(textView: TextView, size: Float) {
        textView.textSize = size
    }

    protected open fun applyTextColor(textView: TextView, color: Int) {
        textView.setTextColor(color)
    }

    protected open fun applyFontFamily(textView: TextView, typeface: Typeface) {
        textView.typeface = typeface
    }

    protected open fun applyGravity(textView: TextView, gravity: Int) {
        textView.gravity = gravity
    }

    protected open fun applyBackgroundColor(textView: TextView, color: Int) {
        textView.setBackgroundColor(color)
    }

    protected open fun applyBackgroundDrawable(textView: TextView, bg: Drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            textView.background = bg
        } else {
            textView.setBackgroundDrawable(bg)
        }
    }

    protected open fun applyTextAppearance(textView: TextView, styleAppearance: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            textView.setTextAppearance(styleAppearance)
        } else {
            textView.setTextAppearance(textView.context, styleAppearance)
        }
    }

    /**
     * Enum to maintain current supported style properties used on on [PhotoEditor.addText] and [PhotoEditor.editText]
     */
    enum class TextStyle private constructor(val property: String) {
        SIZE("TextSize"),
        COLOR("TextColor"),
        GRAVITY("Gravity"),
        FONT_FAMILY("FontFamily"),
        BACKGROUND("Background"),
        TEXT_APPEARANCE("TextAppearance")
    }
}