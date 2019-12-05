package ja.burhanrashid52.photoeditor

import android.graphics.Bitmap
import android.support.annotation.IntRange

/**
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 * @since 8/8/2018
 * Builder Class to apply multiple save options
 */
class SaveSettings(
    val isTransparencyEnabled: Boolean? = null,
    val isClearViewsEnabled: Boolean? = null,
    val compressFormat: Bitmap.CompressFormat? = null,
    val compressQuality: Int? = null
) {

}
